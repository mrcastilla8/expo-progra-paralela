import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Orquestador principal de la aplicacion de correlacion out-of-core.
 * Coordina las fases de ejecucion, validacion, demostracion y benchmark riguroso,
 * delegando la presentacion a ConsoleUI.
 */
public final class Main {

    private static final int DEFAULT_N = 500;
    private static final int DEFAULT_M = 10;
    private static final int DEFAULT_HILOS = 4;
    private static final String DEFAULT_ARCHIVO = "dataset_orquestador.txt";
    private static final int BENCHMARK_WARMUP = 1;
    private static final int BENCHMARK_REPETICIONES = 3;

    private Main() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        File resultadoSerial = null;
        File resultadoParalelo = null;
        File archivoDataset = null;
        boolean datasetTemporal = false;

        try {
            // 1. Banner y configuracion inicial
            ConsoleUI.imprimirBanner();
            Config config = leerConfiguracion(args);

            // Validaciones iniciales
            DatasetGenerator.validarDimensiones(config.N, config.M, DatasetGenerator.DEFAULT_W);
            if (config.hilos <= 0) {
                throw new IllegalArgumentException("La cantidad de hilos debe ser mayor a cero. Valor recibido: " + config.hilos);
            }

            int W = DatasetGenerator.DEFAULT_W;
            int salto = DatasetGenerator.BYTES_SALTO_LINEA;
            long totalPares = (long) config.M * (config.M - 1) / 2;

            ConsoleUI.mostrarConfiguracion(config.N, config.M, totalPares, config.hilos,
                    config.archivo, W, salto);

            // 2. FASE 1: Preparacion y validacion del dataset
            ConsoleUI.iniciarFase(1, 5, "Preparacion del dataset");
            archivoDataset = new File(config.archivo);
            if (!archivoDataset.exists()) {
                ConsoleUI.logVerbose("Dataset no encontrado. Generando dataset correlacionado sintetico...");
                long inicioGen = System.nanoTime();
                DatasetGenerator.generarDatasetCorrelacionado(
                        config.archivo, config.N, config.M, W, DatasetGenerator.DEFAULT_SEED);
                long tiempoGenNs = System.nanoTime() - inicioGen;
                datasetTemporal = true;
                DatasetGenerator.validarArchivo(archivoDataset, config.N, config.M, W);
                ConsoleUI.mostrarDatasetGenerado(config.archivo, config.N, config.M,
                        archivoDataset.length(), tiempoGenNs / 1_000_000.0);
            } else {
                DatasetGenerator.validarArchivo(archivoDataset, config.N, config.M, W);
                ConsoleUI.mostrarDatasetValidado(config.archivo, config.N, config.M, archivoDataset.length());
            }

            resultadoSerial = File.createTempFile("serial_resultados_", ".bin");
            resultadoParalelo = File.createTempFile("paralelo_resultados_", ".bin");
            ConsoleUI.logVerbose("Archivos temporales creados: " + resultadoSerial.getName() + ", " + resultadoParalelo.getName());

            // 3. FASE 2: Procesamiento Serial Demostrativo
            ConsoleUI.iniciarFase(2, 5, "Procesamiento serial");
            SerialEngine.ResultadoAsociacion serialDemo = SerialEngine.procesarSerial(
                    archivoDataset, config.N, config.M, W, salto,
                    pares -> ConsoleUI.imprimirBarraProgreso(pares, totalPares, "Progreso"),
                    resultadoSerial);
            ConsoleUI.finalizarBarraProgreso();
            ConsoleUI.mostrarResultadoSerial(serialDemo);

            // 4. FASE 3: Procesamiento Paralelo Demostrativo
            ConsoleUI.iniciarFase(3, 5, "Procesamiento paralelo - " + config.hilos + " hilos");
            ParallelEngine.ResultadoParalelo paraleloDemo = ParallelEngine.procesarParalelo(
                    archivoDataset, config.N, config.M, W, salto, config.hilos,
                    pares -> ConsoleUI.imprimirBarraProgreso(pares, totalPares, "Progreso"),
                    resultadoParalelo);
            ConsoleUI.finalizarBarraProgreso();
            ConsoleUI.mostrarResultadoParalelo(paraleloDemo);

            // 5. FASE 4: Verificacion exacta de equivalencia bit a bit
            ConsoleUI.iniciarFase(4, 5, "Verificacion serial vs paralelo");
            ConsoleUI.mostrarInicioEquivalencia(totalPares);
            verificarEquivalenciaExacta(resultadoSerial, resultadoParalelo,
                    totalPares, serialDemo, paraleloDemo);

            // 6. FASE 5: Benchmark riguroso
            ConsoleUI.iniciarFase(5, 5, "Benchmark de rendimiento");
            BenchmarkResult benchmark = ejecutarBenchmarkRiguroso(archivoDataset, config.N, config.M, W, salto, config.hilos);

            // 7. Resumen Final
            ConsoleUI.mostrarResumenFinal(config.N, config.M, totalPares,
                    benchmark.serialMedianaNs,
                    benchmark.hilosEfectivos,
                    benchmark.paralelasMedianaNs,
                    true);

        } catch (IllegalArgumentException e) {
            ConsoleUI.mostrarError("Configuracion invalida", e.getMessage(), "Verifique los parametros ingresados.");
            if (ConsoleUI.isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        } catch (IOException e) {
            ConsoleUI.mostrarError("Error de E/S en Dataset / Archivos", e.getMessage(), "Revise la existencia y permisos del archivo.");
            if (ConsoleUI.isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        } catch (Exception e) {
            ConsoleUI.mostrarError("Error de ejecucion", e.getMessage(), "Ocurrio un error inesperado durante el procesamiento.");
            if (ConsoleUI.isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        } finally {
            boolean borradoSerial = resultadoSerial == null || resultadoSerial.delete();
            boolean borradoParalelo = resultadoParalelo == null || resultadoParalelo.delete();
            if (datasetTemporal && archivoDataset != null) {
                archivoDataset.delete();
            }
            ConsoleUI.logVerbose("Limpieza completada (Serial=" + borradoSerial + ", Paralelo=" + borradoParalelo + ")");
        }
    }

    private static Config leerConfiguracion(String[] args) {
        List<String> positionalArgs = new ArrayList<>();
        boolean verbose = false;

        for (String arg : args) {
            if ("--verbose".equalsIgnoreCase(arg) || "-v".equalsIgnoreCase(arg)) {
                verbose = true;
            } else {
                positionalArgs.add(arg);
            }
        }

        ConsoleUI.setVerbose(verbose);
        if (verbose) {
            ConsoleUI.logVerbose("Modo verbose activado.");
        }

        if (!positionalArgs.isEmpty()) {
            String archivo = positionalArgs.get(0);
            int N = positionalArgs.size() > 1 ? parsePositivo(positionalArgs.get(1), "Observaciones (N)") : DEFAULT_N;
            int M = positionalArgs.size() > 2 ? parsePositivo(positionalArgs.get(2), "Variables (M)") : DEFAULT_M;
            int hilos = positionalArgs.size() > 3 ? parsePositivo(positionalArgs.get(3), "Hilos (H)") : DEFAULT_HILOS;
            return new Config(archivo, N, M, hilos);
        }

        // Modo interactivo
        Scanner scanner = new Scanner(System.in);
        ConsoleUI.mostrarTituloConfiguracion();
        int N = ConsoleUI.leerEntero(scanner, "Observaciones", DEFAULT_N, "N");
        int M = ConsoleUI.leerEntero(scanner, "Variables", DEFAULT_M, "M");
        int hilos = ConsoleUI.leerEntero(scanner, "Hilos principales", DEFAULT_HILOS, "H");
        String ruta = ConsoleUI.leerRuta(scanner, "Dataset", DEFAULT_ARCHIVO);

        return new Config(ruta, N, M, hilos);
    }

    private static int parsePositivo(String texto, String campo) {
        try {
            int valor = Integer.parseInt(texto);
            if (valor <= 0) {
                throw new IllegalArgumentException(campo + " debe ser mayor a cero. Valor recibido: " + texto);
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un entero valido. Valor recibido: '" + texto + "'");
        }
    }

    private static void verificarEquivalenciaExacta(File serialFile, File paraleloFile,
                                                     long totalPares,
                                                     SerialEngine.ResultadoAsociacion serial,
                                                     ParallelEngine.ResultadoParalelo paralelo) throws IOException {
        long diferencia = ResultFileManager.encontrarPrimeraDiferencia(
                serialFile, paraleloFile, totalPares);

        boolean metadata = serial.totalPares == paralelo.totalPares
                && serial.colMax1 == paralelo.colMax1
                && serial.colMax2 == paralelo.colMax2
                && serial.colMin1 == paralelo.colMin1
                && serial.colMin2 == paralelo.colMin2
                && Double.doubleToRawLongBits(serial.valorMax)
                   == Double.doubleToRawLongBits(paralelo.valorMax)
                && Double.doubleToRawLongBits(serial.valorMin)
                   == Double.doubleToRawLongBits(paralelo.valorMin);

        if (diferencia == -1L && metadata) {
            ConsoleUI.mostrarEquivalenciaExitosa(totalPares);
        } else {
            if (diferencia >= 0) {
                double valSerial = 0.0;
                double valParalelo = 0.0;
                try (ResultFileManager sf = new ResultFileManager(serialFile, totalPares, false);
                     ResultFileManager pf = new ResultFileManager(paraleloFile, totalPares, false)) {
                    valSerial = Double.longBitsToDouble(sf.leerBits(diferencia));
                    valParalelo = Double.longBitsToDouble(pf.leerBits(diferencia));
                } catch (Exception ignored) {
                }
                ConsoleUI.mostrarEquivalenciaFallida(diferencia,
                        String.format(Locale.US, "%.10f (bits: 0x%016X)", valSerial, Double.doubleToRawLongBits(valSerial)),
                        String.format(Locale.US, "%.10f (bits: 0x%016X)", valParalelo, Double.doubleToRawLongBits(valParalelo)));
                throw new IllegalStateException("Primera diferencia numerica en el indice lineal de par " + diferencia);
            }
            ConsoleUI.mostrarEquivalenciaFallida(-1,
                    String.format(Locale.US, "Max=(%d,%d, r=%.6f), Min=(%d,%d, r=%.6f)",
                            serial.colMax1, serial.colMax2, serial.valorMax, serial.colMin1, serial.colMin2, serial.valorMin),
                    String.format(Locale.US, "Max=(%d,%d, r=%.6f), Min=(%d,%d, r=%.6f)",
                            paralelo.colMax1, paralelo.colMax2, paralelo.valorMax, paralelo.colMin1, paralelo.colMin2, paralelo.valorMin));
            throw new IllegalStateException("Los coeficientes coinciden, pero los metadatos de extremos difieren");
        }
    }

    private static BenchmarkResult ejecutarBenchmarkRiguroso(File archivo, int N, int M,
                                                              int W, int salto, int hilosUsuario)
            throws IOException, InterruptedException {
        // Configuraciones estandar a medir: 2, 4, 8 hilos (incluyendo hilosUsuario si es distinto)
        List<Integer> listaHilos = new ArrayList<>(Arrays.asList(2, 4, 8));
        if (hilosUsuario > 0 && !listaHilos.contains(hilosUsuario) && hilosUsuario != 1) {
            listaHilos.add(hilosUsuario);
            listaHilos.sort(Integer::compareTo);
        }
        int[] hilosConfig = listaHilos.stream().mapToInt(i -> i).toArray();

        ConsoleUI.mostrarInicioBenchmark(BENCHMARK_WARMUP, BENCHMARK_REPETICIONES);

        // 1. Calentamiento JVM
        for (int i = 0; i < BENCHMARK_WARMUP; i++) {
            SerialEngine.procesarSerial(archivo, N, M, W, salto);
            for (int h : hilosConfig) {
                ParallelEngine.procesarParalelo(archivo, N, M, W, salto, h);
            }
        }
        ConsoleUI.mostrarWarmupCompletado();

        // 2. Mediciones Serial
        long[] serialTiempos = new long[BENCHMARK_REPETICIONES];
        for (int i = 0; i < BENCHMARK_REPETICIONES; i++) {
            serialTiempos[i] = SerialEngine.procesarSerial(archivo, N, M, W, salto).tiempoNs;
            ConsoleUI.logVerbose(String.format("Serial iteracion %d: %.3f ms", i + 1, serialTiempos[i] / 1_000_000.0));
        }
        long serialMediana = mediana(serialTiempos);
        ConsoleUI.logVerbose(String.format("Serial mediana: %.3f ms", serialMediana / 1_000_000.0));

        // 3. Mediciones Paralelas
        long[] paralelasMediana = new long[hilosConfig.length];
        int[] hilosEfectivos = new int[hilosConfig.length];
        for (int c = 0; c < hilosConfig.length; c++) {
            long[] mediciones = new long[BENCHMARK_REPETICIONES];
            for (int i = 0; i < BENCHMARK_REPETICIONES; i++) {
                ParallelEngine.ResultadoParalelo r = ParallelEngine.procesarParalelo(
                        archivo, N, M, W, salto, hilosConfig[c]);
                mediciones[i] = r.tiempoNs;
                hilosEfectivos[c] = r.numHilos;
                ConsoleUI.logVerbose(String.format("Paralelo (%d hilos) iteracion %d: %.3f ms",
                        hilosConfig[c], i + 1, r.tiempoNs / 1_000_000.0));
            }
            paralelasMediana[c] = mediana(mediciones);
            ConsoleUI.logVerbose(String.format("Paralelo (%d hilos) mediana: %.3f ms",
                    hilosConfig[c], paralelasMediana[c] / 1_000_000.0));
        }

        ConsoleUI.mostrarBenchmarkCompletado();
        ConsoleUI.mostrarTablaBenchmark(serialMediana, hilosConfig, hilosEfectivos, paralelasMediana);

        guardarBenchmarkCsv(serialMediana, hilosConfig, hilosEfectivos, paralelasMediana);

        return new BenchmarkResult(serialMediana, hilosConfig, hilosEfectivos, paralelasMediana);
    }

    private static void guardarBenchmarkCsv(long serialNs, int[] solicitados,
                                             int[] efectivos, long[] paralelosNs) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream("benchmark_resultados.csv"))) {
            pw.println("modalidad,hilos_solicitados,hilos_efectivos,mediana_ns,mediana_ms,speedup,eficiencia");
            pw.printf(Locale.US, "serial,1,1,%d,%.6f,1.000000,1.000000%n",
                    serialNs, nsAMs(serialNs));
            for (int i = 0; i < solicitados.length; i++) {
                double speedup = (double) serialNs / paralelosNs[i];
                double eficiencia = speedup / efectivos[i];
                pw.printf(Locale.US, "paralelo,%d,%d,%d,%.6f,%.6f,%.6f%n",
                        solicitados[i], efectivos[i], paralelosNs[i],
                        nsAMs(paralelosNs[i]), speedup, eficiencia);
            }
        }
    }

    private static long mediana(long[] valores) {
        long[] copia = new long[valores.length];
        System.arraycopy(valores, 0, copia, 0, valores.length);
        for (int i = 0; i < copia.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < copia.length; j++) {
                if (copia[j] < copia[min]) {
                    min = j;
                }
            }
            long tmp = copia[i];
            copia[i] = copia[min];
            copia[min] = tmp;
        }
        return copia[copia.length / 2];
    }

    private static double nsAMs(long ns) {
        return ns / 1_000_000.0;
    }

    private static final class Config {
        final String archivo;
        final int N;
        final int M;
        final int hilos;

        Config(String archivo, int N, int M, int hilos) {
            this.archivo = archivo;
            this.N = N;
            this.M = M;
            this.hilos = hilos;
        }
    }

    private static final class BenchmarkResult {
        final long serialMedianaNs;
        final int[] hilosSolicitados;
        final int[] hilosEfectivos;
        final long[] paralelasMedianaNs;

        BenchmarkResult(long serialMedianaNs, int[] hilosSolicitados,
                        int[] hilosEfectivos, long[] paralelasMedianaNs) {
            this.serialMedianaNs = serialMedianaNs;
            this.hilosSolicitados = hilosSolicitados;
            this.hilosEfectivos = hilosEfectivos;
            this.paralelasMedianaNs = paralelasMedianaNs;
        }
    }
}
