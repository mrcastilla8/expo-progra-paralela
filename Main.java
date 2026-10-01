import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Scanner;

public final class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";

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
            Config config = leerConfiguracion(args);
            DatasetGenerator.validarDimensiones(config.N, config.M, DatasetGenerator.DEFAULT_W);
            if (config.hilos <= 0) {
                throw new IllegalArgumentException("La cantidad de hilos debe ser mayor a cero");
            }

            int W = DatasetGenerator.DEFAULT_W;
            int salto = DatasetGenerator.BYTES_SALTO_LINEA;
            long totalPares = (long) config.M * (config.M - 1) / 2;
            imprimirEncabezado(config.N, config.M, totalPares, W, salto);

            archivoDataset = new File(config.archivo);
            if (!archivoDataset.exists()) {
                System.out.println("\n[FASE 1] Generando dataset correlacionado reproducible...");
                DatasetGenerator.generarDatasetCorrelacionado(
                        config.archivo, config.N, config.M, W, DatasetGenerator.DEFAULT_SEED);
                datasetTemporal = true;
            }
            DatasetGenerator.validarArchivo(archivoDataset, config.N, config.M, W);
            System.out.printf("[OK] Dataset validado: %s (%d bytes)%n",
                    archivoDataset.getPath(), archivoDataset.length());

            resultadoSerial = File.createTempFile("serial_resultados_", ".bin");
            resultadoParalelo = File.createTempFile("paralelo_resultados_", ".bin");

            System.out.println("\n[FASE 2] Ejecucion demostrativa SERIAL con persistencia de todos los coeficientes");
            SerialEngine.ResultadoAsociacion serialDemo = SerialEngine.procesarSerial(
                    archivoDataset, config.N, config.M, W, salto,
                    pares -> imprimirBarraProgreso(pares, totalPares, "Serial"),
                    resultadoSerial);
            System.out.println();
            imprimirExtremosSerial(serialDemo);

            System.out.printf("\n[FASE 3] Ejecucion demostrativa PARALELA con %d hilos%n", config.hilos);
            ParallelEngine.ResultadoParalelo paraleloDemo = ParallelEngine.procesarParalelo(
                    archivoDataset, config.N, config.M, W, salto, config.hilos,
                    pares -> imprimirBarraProgreso(pares, totalPares, "Paralelo"),
                    resultadoParalelo);
            System.out.println();
            imprimirExtremosParalelo(paraleloDemo);

            System.out.println("\n[FASE 4] Verificacion exacta de equivalencia");
            verificarEquivalenciaExacta(resultadoSerial, resultadoParalelo,
                    totalPares, serialDemo, paraleloDemo);

            System.out.println("\n[FASE 5] Benchmark limpio (sin UI ni archivos de resultados dentro de la medicion)");
            ejecutarBenchmarkRiguroso(archivoDataset, config.N, config.M, W, salto);

        } catch (Exception e) {
            System.err.println(ANSI_RED + "ERROR: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (resultadoSerial != null) {
                resultadoSerial.delete();
            }
            if (resultadoParalelo != null) {
                resultadoParalelo.delete();
            }
            if (datasetTemporal && archivoDataset != null) {
                archivoDataset.delete();
            }
        }
    }

    private static Config leerConfiguracion(String[] args) {
        if (args.length > 0) {
            String archivo = args[0];
            int N = args.length > 1 ? parsePositivo(args[1], "N") : DEFAULT_N;
            int M = args.length > 2 ? parsePositivo(args[2], "M") : DEFAULT_M;
            int hilos = args.length > 3 ? parsePositivo(args[3], "hilos") : DEFAULT_HILOS;
            return new Config(archivo, N, M, hilos);
        }

        Scanner scanner = new Scanner(System.in);
        imprimirBannerBienvenida();
        int N = leerEnteroOpcional(scanner, "Observaciones N", DEFAULT_N);
        int M = leerEnteroOpcional(scanner, "Variables M", DEFAULT_M);
        int hilos = leerEnteroOpcional(scanner, "Cantidad de hilos", DEFAULT_HILOS);
        System.out.printf("Ruta del dataset [%s]: ", DEFAULT_ARCHIVO);
        String ruta = scanner.nextLine().trim();
        if (ruta.isEmpty()) {
            ruta = DEFAULT_ARCHIVO;
        }
        return new Config(ruta, N, M, hilos);
    }

    private static int leerEnteroOpcional(Scanner scanner, String etiqueta, int valorDefault) {
        System.out.printf("%s [%d]: ", etiqueta, valorDefault);
        String texto = scanner.nextLine().trim();
        return texto.isEmpty() ? valorDefault : parsePositivo(texto, etiqueta);
    }

    private static int parsePositivo(String texto, String campo) {
        try {
            int valor = Integer.parseInt(texto);
            if (valor <= 0) {
                throw new IllegalArgumentException(campo + " debe ser mayor a cero");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un entero valido");
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
            System.out.println(ANSI_GREEN + ANSI_BOLD
                    + "[OK] Los " + totalPares
                    + " coeficientes seriales y paralelos son identicos bit a bit."
                    + ANSI_RESET);
        } else {
            if (diferencia >= 0) {
                throw new IllegalStateException("Primera diferencia numerica en el indice lineal de par " + diferencia);
            }
            throw new IllegalStateException("Los coeficientes coinciden, pero los metadatos de extremos difieren");
        }
    }

    private static void ejecutarBenchmarkRiguroso(File archivo, int N, int M,
                                                   int W, int salto)
            throws IOException, InterruptedException {
        int[] hilos = {2, 4, 8};

        System.out.printf("Warm-up: %d ronda(s). Repeticiones medidas: %d.%n",
                BENCHMARK_WARMUP, BENCHMARK_REPETICIONES);
        System.out.println("La UI y la persistencia de resultados quedan fuera de estas mediciones.");

        for (int i = 0; i < BENCHMARK_WARMUP; i++) {
            SerialEngine.procesarSerial(archivo, N, M, W, salto);
            for (int h : hilos) {
                ParallelEngine.procesarParalelo(archivo, N, M, W, salto, h);
            }
        }

        long[] serialTiempos = new long[BENCHMARK_REPETICIONES];
        for (int i = 0; i < BENCHMARK_REPETICIONES; i++) {
            serialTiempos[i] = SerialEngine.procesarSerial(archivo, N, M, W, salto).tiempoNs;
        }
        long serialMediana = mediana(serialTiempos);

        long[] paralelasMediana = new long[hilos.length];
        int[] hilosEfectivos = new int[hilos.length];
        for (int c = 0; c < hilos.length; c++) {
            long[] mediciones = new long[BENCHMARK_REPETICIONES];
            for (int i = 0; i < BENCHMARK_REPETICIONES; i++) {
                ParallelEngine.ResultadoParalelo r = ParallelEngine.procesarParalelo(
                        archivo, N, M, W, salto, hilos[c]);
                mediciones[i] = r.tiempoNs;
                hilosEfectivos[c] = r.numHilos;
            }
            paralelasMediana[c] = mediana(mediciones);
        }

        System.out.println("+------------+-------+--------------+-----------+-------------+");
        System.out.println("| Modalidad  | Hilos | Mediana (ms) | Speedup   | Eficiencia  |");
        System.out.println("+------------+-------+--------------+-----------+-------------+");
        System.out.printf("| %-10s | %5d | %12.3f | %9.4f | %10s |%n",
                "Serial", 1, nsAMs(serialMediana), 1.0, "100.00%");

        for (int i = 0; i < hilos.length; i++) {
            double speedup = (double) serialMediana / paralelasMediana[i];
            double eficiencia = speedup / hilosEfectivos[i] * 100.0;
            System.out.printf("| %-10s | %5d | %12.3f | %9.4f | %9.2f%% |%n",
                    "Paralelo", hilosEfectivos[i], nsAMs(paralelasMediana[i]),
                    speedup, eficiencia);
        }
        System.out.println("+------------+-------+--------------+-----------+-------------+");

        guardarBenchmarkCsv(serialMediana, hilos, hilosEfectivos, paralelasMediana);
        System.out.println("Datos para la curva de Speedup: benchmark_resultados.csv");
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
        for (int i = 0; i < valores.length; i++) {
            copia[i] = valores[i];
        }
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

    private static void imprimirExtremosSerial(SerialEngine.ResultadoAsociacion r) {
        System.out.printf("Pares procesados: %d%n", r.totalPares);
        System.out.printf("Maximo: (%d,%d) r=%.10f%n", r.colMax1, r.colMax2, r.valorMax);
        System.out.printf("Minimo: (%d,%d) r=%.10f%n", r.colMin1, r.colMin2, r.valorMin);
        System.out.println("Nota: el tiempo de esta fase incluye la visualizacion y no se usa para Speedup.");
    }

    private static void imprimirExtremosParalelo(ParallelEngine.ResultadoParalelo r) {
        System.out.printf("Hilos efectivos: %d | Pares procesados: %d%n", r.numHilos, r.totalPares);
        System.out.printf("Maximo: (%d,%d) r=%.10f%n", r.colMax1, r.colMax2, r.valorMax);
        System.out.printf("Minimo: (%d,%d) r=%.10f%n", r.colMin1, r.colMin2, r.valorMin);
        System.out.println("Nota: el tiempo de esta fase incluye la visualizacion y no se usa para Speedup.");
    }

    private static void imprimirBarraProgreso(long actual, long total, String etiqueta) {
        int ancho = 24;
        double ratio = total == 0 ? 1.0 : Math.min(1.0, (double) actual / total);
        int llenos = (int) (ratio * ancho);
        StringBuilder sb = new StringBuilder("\r");
        sb.append(etiqueta).append(" [");
        for (int i = 0; i < ancho; i++) {
            sb.append(i < llenos ? '=' : ' ');
        }
        sb.append(String.format(Locale.US, "] %6.2f%% (%d/%d)", ratio * 100.0, actual, total));
        System.out.print(sb);
        System.out.flush();
    }

    private static void imprimirBannerBienvenida() {
        System.out.println("===============================================================");
        System.out.println(" ANALIZADOR DE CORRELACION SERIAL/PARALELO OUT-OF-CORE");
        System.out.println("===============================================================");
    }

    private static void imprimirEncabezado(int N, int M, long totalPares, int W, int salto) {
        System.out.println("===============================================================");
        System.out.println("UNMSM - PROGRAMACION CONCURRENTE Y PARALELA");
        System.out.println("TRABAJO DE APLICACION 1");
        System.out.println("===============================================================");
        System.out.printf("N=%d | M=%d | T=%d | W=%d | CRLF=%d bytes%n",
                N, M, totalPares, W, salto);
        System.out.println("Arquitectura: out-of-core; el dataset completo no se carga en RAM.");
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
}
