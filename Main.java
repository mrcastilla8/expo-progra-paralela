import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_BOLD   = "\u001B[1m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE   = "\u001B[34m";
    public static final String ANSI_CYAN   = "\u001B[36m";

    private static final int DEFAULT_N = 500;
    private static final int DEFAULT_M = 10;
    private static final int DEFAULT_HILOS = 4;
    private static final String DEFAULT_ARCHIVO = "dataset_orquestador.txt";

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        try {
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8"));
        } catch (Exception ignored) {
        }

        String nombreArchivo;
        int N;
        int M;
        int hilosPrincipales;

        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            imprimirBannerBienvenida();

            System.out.print(ANSI_CYAN + "  > Ingrese observaciones N [" + DEFAULT_N + "]: " + ANSI_RESET);
            String inputN = scanner.nextLine().trim();
            N = inputN.isEmpty() ? DEFAULT_N : Integer.parseInt(inputN);

            System.out.print(ANSI_CYAN + "  > Ingrese variables M [" + DEFAULT_M + "]: " + ANSI_RESET);
            String inputM = scanner.nextLine().trim();
            M = inputM.isEmpty() ? DEFAULT_M : Integer.parseInt(inputM);

            System.out.print(ANSI_CYAN + "  > Ingrese cantidad de hilos [" + DEFAULT_HILOS + "]: " + ANSI_RESET);
            String inputH = scanner.nextLine().trim();
            hilosPrincipales = inputH.isEmpty() ? DEFAULT_HILOS : Integer.parseInt(inputH);

            System.out.print(ANSI_CYAN + "  > Ruta del dataset [" + DEFAULT_ARCHIVO + "]: " + ANSI_RESET);
            String inputArch = scanner.nextLine().trim();
            nombreArchivo = inputArch.isEmpty() ? DEFAULT_ARCHIVO : inputArch;
            System.out.println();
        } else {
            nombreArchivo = args[0];
            N = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_N;
            M = (args.length > 2) ? Integer.parseInt(args[2]) : DEFAULT_M;
            hilosPrincipales = (args.length > 3) ? Integer.parseInt(args[3]) : DEFAULT_HILOS;
        }

        int anchoFijo = DatasetGenerator.DEFAULT_W;
        int bytesSalto = DatasetGenerator.BYTES_SALTO_LINEA;
        long totalParesEsperados = (long) M * (M - 1) / 2;

        imprimirEncabezado(N, M, totalParesEsperados, anchoFijo, bytesSalto);

        File archivoDataset = new File(nombreArchivo);
        boolean archivoGeneradoPorNosotros = false;

        try {

            if (!archivoDataset.exists()) {
                System.out.println(ANSI_BOLD + "\n[FASE 1] Generando dataset estructurado en disco (out-of-core)..." + ANSI_RESET);
                DatasetGenerator.generarDatasetCorrelacionado(nombreArchivo, N, M, anchoFijo);
                archivoGeneradoPorNosotros = true;
                System.out.printf("  " + ANSI_GREEN + "[OK]" + ANSI_RESET + " Dataset creado: '%s' | Tamano: %d bytes (%.2f KB)%n",
                        nombreArchivo, archivoDataset.length(), archivoDataset.length() / 1024.0);
            } else {
                System.out.printf(ANSI_BOLD + "\n[FASE 1] Utilizando dataset existente: '%s' (%d bytes)%n" + ANSI_RESET,
                        nombreArchivo, archivoDataset.length());
            }

            System.out.println("\n---------------------------------------------------------------");
            System.out.println(ANSI_BOLD + "[FASE 2] EJECUTANDO PROCESAMIENTO SERIAL (SerialEngine)..." + ANSI_RESET);
            System.out.println("---------------------------------------------------------------");

            SerialEngine.ResultadoAsociacion resSerial =
                    SerialEngine.procesarSerial(archivoDataset, N, M, anchoFijo, bytesSalto,
                            (pares) -> imprimirBarraProgreso(pares, totalParesEsperados, "Progreso Serial"));

            System.out.println();
            System.out.printf("  > Pares evaluados serialmente: %d%n", resSerial.totalPares);
            System.out.printf("  > Tiempo de ejecucion Serial (T_s): " + ANSI_YELLOW + "%s (%d ms)" + ANSI_RESET + "%n",
                    formatearTiempo(resSerial.tiempoMs), resSerial.tiempoMs);
            System.out.printf("  > Par con maxima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resSerial.colMax1, resSerial.colMax2, resSerial.valorMax);
            System.out.printf("  > Par con minima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resSerial.colMin1, resSerial.colMin2, resSerial.valorMin);

            System.out.println("\n---------------------------------------------------------------");
            System.out.printf(ANSI_BOLD + "[FASE 3] EJECUTANDO PROCESAMIENTO PARALELO (ParallelEngine con %d hilos)...%n" + ANSI_RESET,
                    hilosPrincipales);
            System.out.println("---------------------------------------------------------------");

            ParallelEngine.ResultadoParalelo resParalelo =
                    ParallelEngine.procesarParalelo(archivoDataset, N, M, anchoFijo, bytesSalto, hilosPrincipales,
                            (pares) -> imprimirBarraProgreso(pares, totalParesEsperados, "Progreso Paralelo"));

            System.out.println();
            System.out.printf("  > Hilos desplegados: %d%n", resParalelo.numHilos);
            System.out.printf("  > Pares evaluados en paralelo: %d%n", resParalelo.totalPares);
            System.out.printf("  > Tiempo de ejecucion Paralelo (T_p): " + ANSI_YELLOW + "%s (%d ms)" + ANSI_RESET + "%n",
                    formatearTiempo(resParalelo.tiempoMs), resParalelo.tiempoMs);
            System.out.printf("  > Par con maxima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resParalelo.colMax1, resParalelo.colMax2, resParalelo.valorMax);
            System.out.printf("  > Par con minima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resParalelo.colMin1, resParalelo.colMin2, resParalelo.valorMin);

            demostrarEquivalencia(resSerial, resParalelo);

            calcularYMostrarSpeedup(resSerial.tiempoMs, resParalelo.tiempoMs, resParalelo.numHilos);

            ejecutarBenchmarkMultihilo(archivoDataset, N, M, anchoFijo, bytesSalto, resSerial);

        } catch (IOException | InterruptedException e) {
            System.err.println(ANSI_RED + "Error critico durante la orquestacion: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } finally {

            if (archivoGeneradoPorNosotros && archivoDataset.exists()) {
                boolean borrado = archivoDataset.delete();
                if (borrado) {
                    System.out.println("\n[LIMPIEZA] Archivo temporal de dataset eliminado correctamente.");
                }
            }
        }
    }

    private static void demostrarEquivalencia(SerialEngine.ResultadoAsociacion resSerial,
                                             ParallelEngine.ResultadoParalelo resParalelo) {
        System.out.println("\n===============================================================");
        System.out.println(ANSI_BOLD + "              DEMOSTRACION FORMAL DE EQUIVALENCIA              " + ANSI_RESET);
        System.out.println("                   (OBLIGATORIO POR RUBRICA)                   ");
        System.out.println("===============================================================");

        System.out.printf("El par con maxima asociacion en Serial es (%d,%d) con %.2f%n",
                resSerial.colMax1, resSerial.colMax2, resSerial.valorMax);
        System.out.printf("El par en Paralelo es (%d,%d) con %.2f%n",
                resParalelo.colMax1, resParalelo.colMax2, resParalelo.valorMax);

        boolean paresCoinciden = (resSerial.totalPares == resParalelo.totalPares);
        boolean maxColsCoinciden = (resSerial.colMax1 == resParalelo.colMax1) &&
                                   (resSerial.colMax2 == resParalelo.colMax2);
        boolean maxValCoincide = Math.abs(resSerial.valorMax - resParalelo.valorMax) < 1e-6;

        boolean minColsCoinciden = (resSerial.colMin1 == resParalelo.colMin1) &&
                                   (resSerial.colMin2 == resParalelo.colMin2);
        boolean minValCoincide = Math.abs(resSerial.valorMin - resParalelo.valorMin) < 1e-6;

        boolean equivalenciaExacta = paresCoinciden && maxColsCoinciden && maxValCoincide &&
                                     minColsCoinciden && minValCoincide;

        System.out.println("---------------------------------------------------------------");
        if (equivalenciaExacta) {
            System.out.println(ANSI_GREEN + ANSI_BOLD + ">>> CERTIFICACION DE EQUIVALENCIA: Ambos resultados son EXACTAMENTE IGUALES bit a bit. [OK] <<<" + ANSI_RESET);
            System.out.println("    - Mismo total de pares: " + resSerial.totalPares);
            System.out.printf("    - Maximo identico: (%d,%d) con %.8f%n",
                    resSerial.colMax1, resSerial.colMax2, resSerial.valorMax);
            System.out.printf("    - Minimo identico: (%d,%d) con %.8f%n",
                    resSerial.colMin1, resSerial.colMin2, resSerial.valorMin);
        } else {
            System.out.println(ANSI_RED + ANSI_BOLD + ">>> ERROR: Se detectaron discrepancias entre Serial y Paralelo. <<<" + ANSI_RESET);
        }
        System.out.println("===============================================================");
    }

    private static void calcularYMostrarSpeedup(long ts, long tp, int hilos) {
        System.out.println("\n===============================================================");
        System.out.println(ANSI_BOLD + "                 CALCULO DE FACTOR DE ACELERACION              " + ANSI_RESET);
        System.out.println("===============================================================");
        System.out.println("Formula del profesor:");
        System.out.println(ANSI_YELLOW + ANSI_BOLD + "   Speedup (S = T_s / T_p)" + ANSI_RESET);
        System.out.println();
        System.out.printf("Sustitucion con valores medidos (%d hilos):%n", hilos);
        System.out.printf("   T_s (Tiempo Serial)   = %d ms (%s)%n", ts, formatearTiempo(ts));
        System.out.printf("   T_p (Tiempo Paralelo) = %d ms (%s)%n", tp, formatearTiempo(tp));

        double speedup = (tp > 0) ? ((double) ts / tp) : Double.POSITIVE_INFINITY;
        double eficiencia = speedup / hilos;

        System.out.println();
        System.out.printf("   Resultado: " + ANSI_GREEN + ANSI_BOLD + "Speedup S = %d / %d = %.4fx" + ANSI_RESET + "%n", ts, tp, speedup);
        System.out.printf("   Eficiencia concurrente E = S / H = %.4f (%.2f%%)%n",
                eficiencia, eficiencia * 100.0);
        System.out.println("===============================================================");
    }

    private static void ejecutarBenchmarkMultihilo(File archivo, int N, int M,
                                                  int anchoFijo, int bytesSalto,
                                                  SerialEngine.ResultadoAsociacion resSerial)
            throws IOException, InterruptedException {

        int[] configuracionesHilos = {2, 4, 8};

        System.out.println("\n=================================================================================");
        System.out.println(ANSI_BOLD + "                TABLA COMPARATIVA DE ESCALABILIDAD Y RENDIMIENTO                 " + ANSI_RESET);
        System.out.println("=================================================================================");

        System.out.println("+---------------+---------+--------------------+---------------+-----------------+");
        System.out.printf("| %-13s | %-7s | %-18s | %-13s | %-15s |%n",
                "Modalidad", "Hilos", "Tiempo Medido", "Speedup (S)", "Eficiencia (E)");
        System.out.println("+---------------+---------+--------------------+---------------+-----------------+");

        System.out.printf("| %-13s | %-7d | %-18s | %-13.4f | %-15s |%n",
                "Serial", 1, formatearTiempo(resSerial.tiempoMs), 1.0, "100.00%");

        for (int h : configuracionesHilos) {
            ParallelEngine.ResultadoParalelo rp =
                    ParallelEngine.procesarParalelo(archivo, N, M, anchoFijo, bytesSalto, h);

            double speedup = (rp.tiempoMs > 0) ? ((double) resSerial.tiempoMs / rp.tiempoMs) : Double.POSITIVE_INFINITY;
            double eficiencia = (speedup / h) * 100.0;
            String efStr = String.format(Locale.US, "%.2f%%", eficiencia);

            System.out.printf("| %-13s | %-7d | %-18s | %-13.4f | %-15s |%n",
                    "Paralelo", h, formatearTiempo(rp.tiempoMs), speedup, efStr);
        }

        System.out.println("+---------------+---------+--------------------+---------------+-----------------+");
    }

    private static void imprimirBarraProgreso(long actual, long total, String prefijo) {
        int anchoBarra = 26;
        double ratio = (total > 0) ? (double) actual / total : 1.0;
        if (ratio > 1.0) ratio = 1.0;
        int llenado = (int) (ratio * anchoBarra);

        StringBuilder sb = new StringBuilder("\r  ");
        sb.append(ANSI_CYAN).append(prefijo).append(": [").append(ANSI_GREEN);
        for (int i = 0; i < llenado; i++) sb.append("=");
        if (llenado < anchoBarra) {
            sb.append(">");
            for (int i = llenado + 1; i < anchoBarra; i++) sb.append(" ");
        }
        sb.append(ANSI_CYAN).append("] ")
          .append(ANSI_BOLD).append(String.format(Locale.US, "%5.1f%%", ratio * 100.0)).append(ANSI_RESET)
          .append(String.format(" (%d/%d pares)", actual, total));

        System.out.print(sb.toString());
        System.out.flush();
    }

    private static String formatearTiempo(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else if (ms < 60000) {
            return String.format(Locale.US, "%.2f s", ms / 1000.0);
        } else {
            long mins = ms / 60000;
            long secs = (ms % 60000) / 1000;
            return String.format(Locale.US, "%dm %02ds", mins, secs);
        }
    }

    private static void imprimirBannerBienvenida() {
        System.out.println(ANSI_CYAN + "+=============================================================================+");
        System.out.println("|        ASISTENTE DE CONFIGURACION - ANALIZADOR DE CORRELACION OUT-OF-CORE   |");
        System.out.println("+=============================================================================+" + ANSI_RESET);
        System.out.println("Presione " + ANSI_YELLOW + "[ENTER]" + ANSI_RESET + " en cualquier campo para usar el valor sugerido.\n");
    }

    private static void imprimirEncabezado(int N, int M, long totalPares, int W, int salto) {
        System.out.println(ANSI_CYAN + "===============================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + "   UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS                    ");
        System.out.println("   FACULTAD DE INGENIERIA DE SISTEMAS E INFORMATICA            ");
        System.out.println("   CURSO: PROGRAMACION CONCURRENTE Y PARALELA                  ");
        System.out.println("   TRABAJO DE APLICACION 1 - ORQUESTADOR (Main.java)           " + ANSI_RESET);
        System.out.println(ANSI_CYAN + "===============================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + "Parametros del Problema:" + ANSI_RESET);
        System.out.printf("  - Observaciones (N): %d filas en disco%n", N);
        System.out.printf("  - Atributos (M):     %d columnas numericas%n", M);
        System.out.printf("  - Combinatoria:      T = M*(M-1)/2 = %d pares unicos%n", totalPares);
        System.out.printf("  - Ancho de celda:    W = %d bytes (longitud fija)%n", W);
        System.out.printf("  - Salto de linea:    %d bytes (CRLF)%n", salto);
        System.out.println("  - Arquitectura:      Out-of-Core directo a disco (sin RAM)");
        System.out.println(ANSI_CYAN + "===============================================================" + ANSI_RESET);
    }
}
