import java.io.File;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Scanner;

/**
 * Concentra toda la capa de presentacion, formateo de consola, colores ANSI,
 * barras de progreso, tablas y resumenes para la aplicacion de correlacion out-of-core.
 * 
 * Garantiza la separacion estricta entre la interfaz de usuario y las
 * regiones de calculo medidas en el benchmark.
 */
public final class ConsoleUI {

    public static final int ANCHO_CONSOLA = 70;

    // Simbolos y marcadores compatibles con cualquier consola
    public static final String OK_TAG    = "[OK]";
    public static final String ERR_TAG   = "[ERROR]";
    public static final String WARN_TAG  = "[ADVERTENCIA]";
    public static final String ARROW     = "->";
    public static final String BULLET    = "*";

    // Codigos de color y estilo ANSI
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_DIM = "\u001B[2m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_MAGENTA = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    // Modificadores compuestos
    public static final String ANSI_BOLD_CYAN = "\u001B[1;36m";
    public static final String ANSI_BOLD_GREEN = "\u001B[1;32m";
    public static final String ANSI_BOLD_YELLOW = "\u001B[1;33m";
    public static final String ANSI_BOLD_RED = "\u001B[1;31m";
    public static final String ANSI_BOLD_WHITE = "\u001B[1;37m";

    private static boolean verbose = false;
    private static final PrintStream OUT = System.out;
    private static final PrintStream ERR = System.err;

    private ConsoleUI() {
    }

    public static void setVerbose(boolean isVerbose) {
        verbose = isVerbose;
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static void logVerbose(String mensaje) {
        if (verbose) {
            OUT.println(ANSI_DIM + "  [VERBOSE] " + mensaje + ANSI_RESET);
        }
    }

    public static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static String centrar(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(0, ancho);
        }
        int izq = (ancho - texto.length()) / 2;
        int der = ancho - texto.length() - izq;
        return repetir(' ', izq) + texto + repetir(' ', der);
    }

    // =========================================================================
    // 1. BANNER PRINCIPAL
    // =========================================================================

    public static void imprimirBanner() {
        String bordeDoble = "+" + repetir('=', ANCHO_CONSOLA - 2) + "+";
        String separador = "+" + repetir('-', ANCHO_CONSOLA - 2) + "+";
        int anchoInterior = ANCHO_CONSOLA - 4;

        OUT.println(ANSI_CYAN + bordeDoble + ANSI_RESET);
        OUT.println(ANSI_CYAN + "| " + ANSI_BOLD_WHITE + centrar("ANALIZADOR DE CORRELACION OUT-OF-CORE", anchoInterior) + ANSI_CYAN + " |" + ANSI_RESET);
        OUT.println(ANSI_CYAN + "| " + ANSI_CYAN + centrar("Programacion Concurrente y Paralela", anchoInterior) + ANSI_CYAN + " |" + ANSI_RESET);
        OUT.println(ANSI_CYAN + separador + ANSI_RESET);
        OUT.println(ANSI_CYAN + "| " + ANSI_RESET + centrar("Universidad Nacional Mayor de San Marcos", anchoInterior) + ANSI_CYAN + " |" + ANSI_RESET);
        OUT.println(ANSI_CYAN + "| " + ANSI_RESET + centrar("Trabajo de Aplicacion 1 - Procesamiento Serial vs Paralelo con RAF", anchoInterior) + ANSI_CYAN + " |" + ANSI_RESET);
        OUT.println(ANSI_CYAN + bordeDoble + ANSI_RESET);
        OUT.println();
    }

    // =========================================================================
    // 2. ASISTENTE Y VISUALIZACION DE CONFIGURACION
    // =========================================================================

    public static void mostrarTituloConfiguracion() {
        OUT.println(ANSI_BOLD_CYAN + "CONFIGURACION" + ANSI_RESET);
        OUT.println(ANSI_DIM + "Presione [Enter] para aceptar los valores predeterminados." + ANSI_RESET);
        OUT.println();
    }

    public static int leerEntero(Scanner scanner, String etiqueta, int valorDefault, String variableSimbolo) {
        OUT.printf("  %-20s %-5s [%s%d%s] : ",
                etiqueta,
                variableSimbolo,
                ANSI_BOLD_YELLOW, valorDefault, ANSI_RESET);
        String linea = scanner.nextLine().trim();
        if (linea.isEmpty()) {
            return valorDefault;
        }
        try {
            int val = Integer.parseInt(linea);
            if (val <= 0) {
                mostrarAdvertencia("El valor debe ser mayor a cero. Usando valor por defecto: " + valorDefault);
                return valorDefault;
            }
            return val;
        } catch (NumberFormatException e) {
            mostrarAdvertencia("Entrada no valida. Usando valor por defecto: " + valorDefault);
            return valorDefault;
        }
    }

    public static String leerRuta(Scanner scanner, String etiqueta, String valorDefault) {
        OUT.printf("  %-20s %-5s [%s%s%s] : ",
                etiqueta,
                "",
                ANSI_BOLD_YELLOW, valorDefault, ANSI_RESET);
        String linea = scanner.nextLine().trim();
        return linea.isEmpty() ? valorDefault : linea;
    }

    public static void mostrarConfiguracion(int N, int M, long totalPares, int hilos,
                                            String archivo, int W, int salto) {
        OUT.println();
        OUT.println(ANSI_BOLD_WHITE + "Configuracion seleccionada" + ANSI_RESET);
        OUT.println();
        OUT.printf("  %-16s : %s%s%s%n", "Observaciones", ANSI_BOLD_WHITE, NumberFormat.getNumberInstance(Locale.US).format(N), ANSI_RESET);
        OUT.printf("  %-16s : %s%d%s%n", "Variables", ANSI_BOLD_WHITE, M, ANSI_RESET);
        OUT.printf("  %-16s : %s%s%s (%d pares M(M-1)/2)%n", "Pares unicos", ANSI_BOLD_YELLOW, NumberFormat.getNumberInstance(Locale.US).format(totalPares), ANSI_RESET, totalPares);
        OUT.printf("  %-16s : %s%d%s%n", "Hilos", ANSI_BOLD_WHITE, hilos, ANSI_RESET);
        OUT.printf("  %-16s : %s%s%s%n", "Dataset", ANSI_BOLD_CYAN, archivo, ANSI_RESET);
        OUT.printf("  %-16s : %s%d bytes/celda + CRLF (%d bytes)%s%n", "Formato", ANSI_DIM, W, salto, ANSI_RESET);
        OUT.println();
    }

    // =========================================================================
    // 3. ENCABEZADOS DE FASES
    // =========================================================================

    public static void iniciarFase(int numero, int totalFases, String titulo) {
        OUT.printf("%s[%d/%d] %s%s%n%n", ANSI_BOLD_CYAN, numero, totalFases, titulo.toUpperCase(), ANSI_RESET);
    }

    // =========================================================================
    // 4. FASE 1: DATASET
    // =========================================================================

    public static void mostrarDatasetGenerado(String archivo, int N, int M, long bytes, double tiempoMs) {
        OUT.println(ANSI_BOLD_GREEN + "  " + OK_TAG + " Dataset generado correctamente" + ANSI_RESET);
        OUT.printf("    Archivo : %s%s%s%n", ANSI_BOLD_WHITE, archivo, ANSI_RESET);
        OUT.printf("    Filas   : %s%d%s%n", ANSI_BOLD_WHITE, N, ANSI_RESET);
        OUT.printf("    Columnas: %s%d%s%n", ANSI_BOLD_WHITE, M, ANSI_RESET);
        OUT.printf("    Tamano  : %s%s%s (%d bytes)%n", ANSI_BOLD_YELLOW, formatearBytes(bytes), ANSI_RESET, bytes);
        OUT.printf("    Tiempo  : %s%.2f ms%s%n", ANSI_DIM, tiempoMs, ANSI_RESET);
        OUT.println();
    }

    public static void mostrarDatasetValidado(String archivo, int N, int M, long bytes) {
        OUT.println(ANSI_BOLD_GREEN + "  " + OK_TAG + " Dataset existente validado" + ANSI_RESET);
        OUT.printf("    Archivo : %s%s%s%n", ANSI_BOLD_WHITE, archivo, ANSI_RESET);
        OUT.printf("    Filas   : %s%d%s%n", ANSI_BOLD_WHITE, N, ANSI_RESET);
        OUT.printf("    Columnas: %s%d%s%n", ANSI_BOLD_WHITE, M, ANSI_RESET);
        OUT.printf("    Tamano  : %s%s%s (%d bytes conforme al formato fisico)%n",
                ANSI_BOLD_YELLOW, formatearBytes(bytes), ANSI_RESET, bytes);
        OUT.println();
    }

    // =========================================================================
    // 5. BARRAS DE PROGRESO (SOLO MODO DEMOSTRATIVO)
    // =========================================================================

    public static void imprimirBarraProgreso(long actual, long total, String etiqueta) {
        int ancho = 24;
        double ratio = total <= 0 ? 1.0 : Math.min(1.0, (double) actual / total);
        int llenos = (int) Math.round(ratio * ancho);
        if (llenos > ancho) {
            llenos = ancho;
        }

        StringBuilder sb = new StringBuilder("\r  ");
        sb.append(ANSI_CYAN).append(String.format("%-8s", etiqueta)).append(ANSI_RESET).append(" [");
        sb.append(ANSI_GREEN);
        for (int i = 0; i < ancho; i++) {
            if (i < llenos) {
                sb.append("=");
            } else if (i == llenos && llenos > 0 && llenos < ancho) {
                sb.append(">");
            } else {
                sb.append(".");
            }
        }
        sb.append(ANSI_RESET).append("] ");
        sb.append(String.format(Locale.US, "%s%5.1f%%%s  %s%d/%d%s pares",
                ANSI_BOLD_WHITE, ratio * 100.0, ANSI_RESET,
                ANSI_YELLOW, actual, total, ANSI_RESET));

        OUT.print(sb);
        OUT.flush();
    }

    public static void finalizarBarraProgreso() {
        OUT.println();
    }

    // =========================================================================
    // 6. RESULTADOS SERIAL Y PARALELO
    // =========================================================================

    public static void mostrarResultadoSerial(SerialEngine.ResultadoAsociacion r) {
        OUT.println(ANSI_BOLD_GREEN + "  " + OK_TAG + " Procesamiento serial completado" + ANSI_RESET);
        OUT.println();
        OUT.printf("  Pares evaluados : %s%d%s%n", ANSI_BOLD_WHITE, r.totalPares, ANSI_RESET);
        OUT.println("  Maxima asociacion");
        OUT.printf("      Columnas     : %s(%d, %d)%s%n", ANSI_BOLD_CYAN, r.colMax1, r.colMax2, ANSI_RESET);
        OUT.printf("      Pearson      : %s%+.6f%s%n", ANSI_BOLD_GREEN, r.valorMax, ANSI_RESET);
        OUT.println();
        OUT.println("  Minima asociacion");
        OUT.printf("      Columnas     : %s(%d, %d)%s%n", ANSI_BOLD_CYAN, r.colMin1, r.colMin2, ANSI_RESET);
        OUT.printf("      Pearson      : %s%+.6f%s%n", ANSI_BOLD_YELLOW, r.valorMin, ANSI_RESET);
        OUT.println();
        OUT.printf("  Tiempo demostrativo: %s%d ms%s %s(incluye visualizacion y persistencia out-of-core)%s%n",
                ANSI_YELLOW, r.tiempoMs, ANSI_RESET,
                ANSI_DIM, ANSI_RESET);
        OUT.println();
    }

    public static void mostrarResultadoParalelo(ParallelEngine.ResultadoParalelo r) {
        OUT.println(ANSI_BOLD_GREEN + "  " + OK_TAG + " Procesamiento paralelo completado" + ANSI_RESET);
        OUT.println();
        OUT.printf("  Hilos utilizados : %s%d%s%n", ANSI_BOLD_WHITE, r.numHilos, ANSI_RESET);
        OUT.printf("  Pares evaluados  : %s%d%s%n", ANSI_BOLD_WHITE, r.totalPares, ANSI_RESET);
        OUT.println();
        OUT.printf("  Maxima asociacion : %s(%d, %d)%s " + ARROW + " %s%+.6f%s%n",
                ANSI_BOLD_CYAN, r.colMax1, r.colMax2, ANSI_RESET,
                ANSI_BOLD_GREEN, r.valorMax, ANSI_RESET);
        OUT.printf("  Minima asociacion : %s(%d, %d)%s " + ARROW + " %s%+.6f%s%n",
                ANSI_BOLD_CYAN, r.colMin1, r.colMin2, ANSI_RESET,
                ANSI_BOLD_YELLOW, r.valorMin, ANSI_RESET);
        OUT.println();
        OUT.printf("  Tiempo demostrativo: %s%d ms%s %s(incluye visualizacion y persistencia out-of-core)%s%n",
                ANSI_YELLOW, r.tiempoMs, ANSI_RESET,
                ANSI_DIM, ANSI_RESET);
        OUT.println();
    }

    // =========================================================================
    // 7. FASE 4: VERIFICACION DE EQUIVALENCIA
    // =========================================================================

    public static void mostrarInicioEquivalencia(long totalPares) {
        OUT.println("  Comparando archivos de resultados en disco...");
        OUT.printf("  Coeficientes esperados: %s%d%s%n",
                ANSI_BOLD_YELLOW, totalPares, ANSI_RESET);
    }

    public static void mostrarEquivalenciaExitosa(long totalPares) {
        OUT.println();
        OUT.printf("  %s%d / %d coeficientes verificados%s%n",
                ANSI_BOLD_YELLOW, totalPares, totalPares, ANSI_RESET);
        OUT.println(ANSI_BOLD_GREEN + "  " + OK_TAG + " EQUIVALENCIA EXACTA CONFIRMADA" + ANSI_RESET);
        OUT.println();
        OUT.printf("    Resultados seriales  : %s%d%s%n", ANSI_BOLD_WHITE, totalPares, ANSI_RESET);
        OUT.printf("    Resultados paralelos : %s%d%s%n", ANSI_BOLD_WHITE, totalPares, ANSI_RESET);
        OUT.printf("    Diferencias          : %s0%s%n", ANSI_BOLD_GREEN, ANSI_RESET);
        OUT.printf("    Comparacion          : %sbit a bit (64-bit IEEE-754)%s%n", ANSI_BOLD_WHITE, ANSI_RESET);
        OUT.println();
    }

    public static void mostrarEquivalenciaFallida(long indiceDiferencia, String serialVal, String paraleloVal) {
        OUT.println();
        OUT.println(ANSI_BOLD_RED + "  " + ERR_TAG + " EQUIVALENCIA NO CONFIRMADA" + ANSI_RESET);
        OUT.println();
        OUT.println("    Primera diferencia encontrada:");
        if (indiceDiferencia >= 0) {
            OUT.printf("    Indice del par : %s%d%s%n", ANSI_BOLD_RED, indiceDiferencia, ANSI_RESET);
        }
        if (serialVal != null) {
            OUT.printf("    Serial         : %s%s%s%n", ANSI_BOLD_YELLOW, serialVal, ANSI_RESET);
        }
        if (paraleloVal != null) {
            OUT.printf("    Paralelo       : %s%s%s%n", ANSI_BOLD_YELLOW, paraleloVal, ANSI_RESET);
        }
        OUT.println();
    }

    // =========================================================================
    // 8. FASE 5: BENCHMARK
    // =========================================================================

    public static void mostrarInicioBenchmark(int warmup, int repeticiones) {
        OUT.println("  Configuracion del benchmark:");
        OUT.printf("    * Warm-up      : %d ronda(s)%n", warmup);
        OUT.printf("    * Repeticiones : %d por configuracion (se utiliza la mediana)%n", repeticiones);
        OUT.println();
        OUT.println("  Preparando JVM...");
    }

    public static void mostrarWarmupCompletado() {
        OUT.println(ANSI_BOLD_GREEN + "  " + OK_TAG + " Warm-up completado" + ANSI_RESET);
        OUT.println();
        OUT.println("  Ejecutando mediciones sin salida de consola...");
    }

    public static void mostrarBenchmarkCompletado(int repeticiones) {
        OUT.println(ANSI_BOLD_GREEN + "  " + OK_TAG + " Benchmark completado (mediana de " + repeticiones + " repeticiones)" + ANSI_RESET);
        OUT.println();
    }

    public static void mostrarTablaBenchmark(long serialMedianaNs, int[] solicitados,
                                             int[] efectivos, long[] paralelosMedianaNs) {
        String bordeSep = "+------------+-------------+-----------+----------------+-----------+------------+";

        OUT.println(ANSI_CYAN + bordeSep + ANSI_RESET);
        OUT.printf(ANSI_CYAN + "| " + ANSI_BOLD_WHITE + "%-10s" + ANSI_CYAN + " | "
                + ANSI_BOLD_WHITE + "%-11s" + ANSI_CYAN + " | "
                + ANSI_BOLD_WHITE + "%-9s" + ANSI_CYAN + " | "
                + ANSI_BOLD_WHITE + "%-14s" + ANSI_CYAN + " | "
                + ANSI_BOLD_WHITE + "%-9s" + ANSI_CYAN + " | "
                + ANSI_BOLD_WHITE + "%-10s" + ANSI_CYAN + " |%n" + ANSI_RESET,
                "Modalidad", "Solicitados", "Efectivos", "Mediana", "Speedup", "Eficiencia");
        OUT.println(ANSI_CYAN + bordeSep + ANSI_RESET);

        // Fila Serial
        OUT.printf(ANSI_CYAN + "| " + ANSI_RESET + "%-10s" + ANSI_CYAN + " | "
                + ANSI_RESET + "%11d" + ANSI_CYAN + " | "
                + ANSI_RESET + "%9d" + ANSI_CYAN + " | "
                + ANSI_YELLOW + "%-14s" + ANSI_CYAN + " | "
                + ANSI_RESET + "%-9s" + ANSI_CYAN + " | "
                + ANSI_RESET + "%-10s" + ANSI_CYAN + " |%n" + ANSI_RESET,
                "Serial", 1, 1,
                formatearTiempo(serialMedianaNs),
                "1.000x",
                "100.0%");

        // Filas Paralelas
        for (int i = 0; i < solicitados.length; i++) {
            double speedup = (double) serialMedianaNs / paralelosMedianaNs[i];
            double eficiencia = (speedup / efectivos[i]) * 100.0;
            OUT.printf(ANSI_CYAN + "| " + ANSI_RESET + "%-10s" + ANSI_CYAN + " | "
                    + ANSI_BOLD_WHITE + "%11d" + ANSI_CYAN + " | "
                    + ANSI_BOLD_WHITE + "%9d" + ANSI_CYAN + " | "
                    + ANSI_YELLOW + "%-14s" + ANSI_CYAN + " | "
                    + ANSI_BOLD_GREEN + "%-9s" + ANSI_CYAN + " | "
                    + ANSI_CYAN + "%-10s" + ANSI_CYAN + " |%n" + ANSI_RESET,
                    "Paralelo",
                    solicitados[i],
                    efectivos[i],
                    formatearTiempo(paralelosMedianaNs[i]),
                    String.format(Locale.US, "%.3fx", speedup),
                    String.format(Locale.US, "%.1f%%", eficiencia));
        }

        OUT.println(ANSI_CYAN + bordeSep + ANSI_RESET);
    }

    public static void mostrarBenchmarkExportado(String archivoCsv) {
        OUT.println(ANSI_DIM + "  * Medidas exportadas exitosamente a: " + ANSI_RESET + ANSI_BOLD_WHITE + archivoCsv + ANSI_RESET);
        OUT.println();
    }

    // =========================================================================
    // 9. RESUMEN FINAL
    // =========================================================================

    public static void mostrarResumenFinal(int N, int M, long totalPares,
                                           long serialMedianaNs, int[] hilosEfectivos,
                                           long[] paralelosMedianaNs,
                                           boolean serialBorrado, boolean paraleloBorrado,
                                           boolean datasetBorrado, boolean huboDatasetTemporal) {
        String sep = repetir('=', ANCHO_CONSOLA);
        OUT.println(ANSI_CYAN + sep + ANSI_RESET);
        OUT.println(ANSI_BOLD_WHITE + centrar("RESUMEN FINAL", ANCHO_CONSOLA) + ANSI_RESET);
        OUT.println(ANSI_CYAN + sep + ANSI_RESET);
        OUT.println();
        OUT.println(ANSI_GREEN + "  " + OK_TAG + " Dataset procesado mediante acceso aleatorio (RAF)" + ANSI_RESET);
        OUT.println(ANSI_GREEN + "  " + OK_TAG + " Dataset completo nunca cargado en memoria RAM" + ANSI_RESET);
        OUT.println(ANSI_GREEN + "  " + OK_TAG + " Procesamiento serial completado" + ANSI_RESET);
        OUT.println(ANSI_GREEN + "  " + OK_TAG + " Procesamiento paralelo completado" + ANSI_RESET);
        OUT.println(ANSI_GREEN + "  " + OK_TAG + " Equivalencia bit a bit confirmada" + ANSI_RESET);
        OUT.println(ANSI_GREEN + "  " + OK_TAG + " Benchmark multihilo completado" + ANSI_RESET);
        OUT.println();

        OUT.println("  Configuracion:");
        OUT.printf("    N = %d%n", N);
        OUT.printf("    M = %d%n", M);
        OUT.printf("    Pares = %d%n", totalPares);
        OUT.println();

        // Encontrar mejor tiempo (menor mediana observada)
        int mejorIndice = -1;
        long mejorTiempo = serialMedianaNs;
        for (int i = 0; i < paralelosMedianaNs.length; i++) {
            if (paralelosMedianaNs[i] < mejorTiempo) {
                mejorTiempo = paralelosMedianaNs[i];
                mejorIndice = i;
            }
        }

        OUT.println("  Menor mediana observada:");
        if (mejorIndice >= 0) {
            double bestSpeedup = (double) serialMedianaNs / mejorTiempo;
            OUT.printf("    %s%d hilos efectivos%s " + ARROW + " %s%s%s%n",
                    ANSI_BOLD_WHITE, hilosEfectivos[mejorIndice], ANSI_RESET,
                    ANSI_BOLD_YELLOW, formatearTiempo(mejorTiempo), ANSI_RESET);
            OUT.println();
            OUT.println("  Speedup:");
            OUT.printf("    %s%.3fx%s%n", ANSI_BOLD_GREEN, bestSpeedup, ANSI_RESET);
        } else {
            OUT.printf("    %sSerial (1 hilo)%s " + ARROW + " %s%s%s%n",
                    ANSI_BOLD_WHITE, ANSI_RESET,
                    ANSI_BOLD_YELLOW, formatearTiempo(serialMedianaNs), ANSI_RESET);
            OUT.println();
            OUT.println("  Speedup:");
            OUT.printf("    %s1.000x%s%n", ANSI_BOLD_GREEN, ANSI_RESET);
        }
        OUT.println();

        OUT.println("  Archivos temporales:");
        if (serialBorrado && paraleloBorrado && (!huboDatasetTemporal || datasetBorrado)) {
            OUT.println(ANSI_GREEN + "    " + OK_TAG + " Archivos temporales eliminados correctamente" + ANSI_RESET);
        } else {
            if (!serialBorrado) {
                OUT.println(ANSI_BOLD_YELLOW + "    " + WARN_TAG + " No se pudo eliminar resultado serial temporal" + ANSI_RESET);
            }
            if (!paraleloBorrado) {
                OUT.println(ANSI_BOLD_YELLOW + "    " + WARN_TAG + " No se pudo eliminar resultado paralelo temporal" + ANSI_RESET);
            }
            if (huboDatasetTemporal && !datasetBorrado) {
                OUT.println(ANSI_BOLD_YELLOW + "    " + WARN_TAG + " No se pudo eliminar dataset temporal" + ANSI_RESET);
            }
        }
        OUT.println();

        OUT.println(ANSI_CYAN + sep + ANSI_RESET);
        OUT.println(ANSI_BOLD_GREEN + centrar("EJECUCION FINALIZADA CORRECTAMENTE", ANCHO_CONSOLA) + ANSI_RESET);
        OUT.println(ANSI_CYAN + sep + ANSI_RESET);
        OUT.println();
    }

    // =========================================================================
    // 10. MENSAJES DE ERROR Y ADVERTENCIA
    // =========================================================================

    public static void mostrarAdvertencia(String mensaje) {
        OUT.println(ANSI_BOLD_YELLOW + "  " + WARN_TAG + " " + mensaje + ANSI_RESET);
    }

    public static void mostrarError(String titulo, String descripcion, String sugerencia) {
        ERR.println();
        ERR.println(ANSI_BOLD_RED + "ERROR: " + titulo.toUpperCase() + ANSI_RESET);
        ERR.println();
        ERR.println("  " + descripcion);
        if (sugerencia != null && !sugerencia.isEmpty()) {
            ERR.println();
            ERR.println(ANSI_YELLOW + "  " + sugerencia + ANSI_RESET);
        }
        ERR.println();
    }

    // =========================================================================
    // UTILIDADES DE FORMATEO
    // =========================================================================

    public static String formatearTiempo(long ns) {
        if (ns >= 1_000_000_000L) {
            return String.format(Locale.US, "%.3f s", ns / 1_000_000_000.0);
        } else {
            return String.format(Locale.US, "%.3f ms", ns / 1_000_000.0);
        }
    }

    public static String formatearBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.2f KB", bytes / 1024.0);
        } else {
            return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
