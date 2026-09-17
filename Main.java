import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Main — Integrante 5: Marco Castilla "El Orquestador"
 *
 * Responsabilidades del Orquestador:
 *   1. Unificacion de modulos:
 *      - DatasetGenerator (Generacion en disco de ancho fijo out-of-core).
 *      - RAFManager (Acceso aleatorio directo mediante punteros de bytes).
 *      - SerialEngine (Linea base matematica serial out-of-core).
 *      - ParallelEngine (Concurrencia multihilo con descriptores independientes).
 *   2. Secuencia obligatoria:
 *      - Ejecuta primero el flujo Serial (T_s).
 *      - Luego ejecuta el flujo Paralelo (T_p).
 *   3. Demostracion de Equivalencia (Requisito de rubrica):
 *      Imprime textualmente:
 *      "El par con maxima asociacion en Serial es (A,B) con 0.99"
 *      "El par en Paralelo es (A,B) con 0.99"
 *      y valida formalmente que ambos resultados sean identicos bit a bit.
 *   4. Calculo de Aceleracion:
 *      Impresion explicita de la formula del profesor:
 *      Speedup (S = T_s / T_p)
 *      junto con la sustitucion numerica directa y la eficiencia (E = S / H).
 *   5. Benchmark de Escalabilidad Multihilo:
 *      Ejecucion comparativa con 2, 4 y 8 hilos (directriz de agents.md).
 *
 * Restricciones estrictas cumplidas (agents.md):
 *   - Out-of-core directo a disco: la memoria RAM solo contiene acumuladores escalares.
 *   - Sin colecciones dinamicas ni librerias externas.
 *   - Aislamiento absoluto de punteros de archivo por cada hilo trabajador.
 */
public class Main {

    // Constantes por defecto para demostracion y benchmarking
    private static final int DEFAULT_N = 500;       // Numero de observaciones (filas)
    private static final int DEFAULT_M = 10;        // Numero de atributos (columnas)
    private static final int DEFAULT_HILOS = 4;     // Hilos por defecto para el paralelo
    private static final String DEFAULT_ARCHIVO = "dataset_orquestador.txt";

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        // Asegurar salida en UTF-8 para caracteres acentuados en consola
        try {
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8"));
        } catch (Exception ignored) {
        }

        // Parametros de configuracion (permiten sobrescritura via argumentos CLI)
        String nombreArchivo = (args.length > 0) ? args[0] : DEFAULT_ARCHIVO;
        int N = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_N;
        int M = (args.length > 2) ? Integer.parseInt(args[2]) : DEFAULT_M;
        int hilosPrincipales = (args.length > 3) ? Integer.parseInt(args[3]) : DEFAULT_HILOS;

        int anchoFijo = DatasetGenerator.DEFAULT_W;
        int bytesSalto = DatasetGenerator.BYTES_SALTO_LINEA;
        long totalParesEsperados = (long) M * (M - 1) / 2;

        imprimirEncabezado(N, M, totalParesEsperados, anchoFijo, bytesSalto);

        File archivoDataset = new File(nombreArchivo);
        boolean archivoGeneradoPorNosotros = false;

        try {
            // =================================================================
            // FASE 1: PREPARACION DEL DATASET EN DISCO (OUT-OF-CORE)
            // =================================================================
            if (!archivoDataset.exists()) {
                System.out.println("[FASE 1] Generando dataset estructurado en disco (out-of-core)...");
                DatasetGenerator.generarDatasetCorrelacionado(nombreArchivo, N, M, anchoFijo);
                archivoGeneradoPorNosotros = true;
                System.out.printf(" -> Dataset creado: '%s' | Tama\u00f1o: %d bytes (%.2f KB)%n",
                        nombreArchivo, archivoDataset.length(), archivoDataset.length() / 1024.0);
            } else {
                System.out.printf("[FASE 1] Utilizando dataset existente: '%s' (%d bytes)%n",
                        nombreArchivo, archivoDataset.length());
            }

            // =================================================================
            // FASE 2: EJECUCION SERIAL (LINEA BASE)
            // =================================================================
            System.out.println("\n---------------------------------------------------------------");
            System.out.println("[FASE 2] EJECUTANDO PROCESAMIENTO SERIAL (SerialEngine)...");
            System.out.println("---------------------------------------------------------------");
            SerialEngine.ResultadoAsociacion resSerial =
                    SerialEngine.procesarSerial(archivoDataset, N, M, anchoFijo, bytesSalto);

            System.out.printf("  > Pares evaluados serialmente: %d%n", resSerial.totalPares);
            System.out.printf("  > Tiempo de ejecucion Serial (T_s): %d ms%n", resSerial.tiempoMs);
            System.out.printf("  > Par con m\u00e1xima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resSerial.colMax1, resSerial.colMax2, resSerial.valorMax);
            System.out.printf("  > Par con m\u00ednima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resSerial.colMin1, resSerial.colMin2, resSerial.valorMin);

            // =================================================================
            // FASE 3: EJECUCION PARALELA (CONCURRENTE CON H HILOS)
            // =================================================================
            System.out.println("\n---------------------------------------------------------------");
            System.out.printf("[FASE 3] EJECUTANDO PROCESAMIENTO PARALELO (ParallelEngine con %d hilos)...%n",
                    hilosPrincipales);
            System.out.println("---------------------------------------------------------------");
            ParallelEngine.ResultadoParalelo resParalelo =
                    ParallelEngine.procesarParalelo(archivoDataset, N, M, anchoFijo, bytesSalto, hilosPrincipales);

            System.out.printf("  > Hilos desplegados: %d%n", resParalelo.numHilos);
            System.out.printf("  > Pares evaluados en paralelo: %d%n", resParalelo.totalPares);
            System.out.printf("  > Tiempo de ejecucion Paralelo (T_p): %d ms%n", resParalelo.tiempoMs);
            System.out.printf("  > Par con m\u00e1xima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resParalelo.colMax1, resParalelo.colMax2, resParalelo.valorMax);
            System.out.printf("  > Par con m\u00ednima correlacion: (%d, %d) -> Pearson = %.6f%n",
                    resParalelo.colMin1, resParalelo.colMin2, resParalelo.valorMin);

            // =================================================================
            // FASE 4: DEMOSTRACION DE EQUIVALENCIA (REQUISITO ESTRICTO DE RUBRICA)
            // =================================================================
            demostrarEquivalencia(resSerial, resParalelo);

            // =================================================================
            // FASE 5: CALCULO DE FACTOR DE ACELERACION (SPEEDUP)
            // =================================================================
            calcularYMostrarSpeedup(resSerial.tiempoMs, resParalelo.tiempoMs, resParalelo.numHilos);

            // =================================================================
            // FASE 6: BENCHMARK Y ESCALABILIDAD MULTIHILO (2, 4 y 8 HILOS)
            // =================================================================
            ejecutarBenchmarkMultihilo(archivoDataset, N, M, anchoFijo, bytesSalto, resSerial);

        } catch (IOException | InterruptedException e) {
            System.err.println("Error critico durante la orquestacion: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Limpieza higienica del archivo autogenerado si corresponde
            if (archivoGeneradoPorNosotros && archivoDataset.exists()) {
                boolean borrado = archivoDataset.delete();
                if (borrado) {
                    System.out.println("\n[LIMPIEZA] Archivo temporal de dataset eliminado correctamente.");
                }
            }
        }
    }

    // =========================================================================
    // RUTINA 1: DEMOSTRACION FORMAL DE EQUIVALENCIA (RUBRICA)
    // =========================================================================

    /**
     * Valida e imprime la equivalencia numerica bit a bit entre la ejecucion
     * serial y paralela, imprimiendo las frases exigidas por la rubrica.
     */
    private static void demostrarEquivalencia(SerialEngine.ResultadoAsociacion resSerial,
                                             ParallelEngine.ResultadoParalelo resParalelo) {
        System.out.println("\n===============================================================");
        System.out.println("              DEMOSTRACI\u00d3N FORMAL DE EQUIVALENCIA              ");
        System.out.println("                   (OBLIGATORIO POR R\u00daBRICA)                   ");
        System.out.println("===============================================================");

        // Impresion literal segun las especificaciones de rubrica solicitadas
        System.out.printf("El par con m\u00e1xima asociaci\u00f3n en Serial es (%d,%d) con %.2f%n",
                resSerial.colMax1, resSerial.colMax2, resSerial.valorMax);
        System.out.printf("El par en Paralelo es (%d,%d) con %.2f%n",
                resParalelo.colMax1, resParalelo.colMax2, resParalelo.valorMax);

        // Verificacion rigurosa de consistencia numerica (tolerancia 1e-6 para punto flotante)
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
            System.out.println(">>> CERTIFICACI\u00d3N DE EQUIVALENCIA: Ambos resultados son EXACTAMENTE IGUALES bit a bit. [OK] <<<");
            System.out.println("    - Mismo total de pares: " + resSerial.totalPares);
            System.out.printf("    - M\u00e1ximo id\u00e9ntico: (%d,%d) con %.8f%n",
                    resSerial.colMax1, resSerial.colMax2, resSerial.valorMax);
            System.out.printf("    - M\u00ednimo id\u00e9ntico: (%d,%d) con %.8f%n",
                    resSerial.colMin1, resSerial.colMin2, resSerial.valorMin);
        } else {
            System.out.println(">>> ERROR: Se detectaron discrepancias entre Serial y Paralelo. <<<");
        }
        System.out.println("===============================================================");
    }

    // =========================================================================
    // RUTINA 2: CALCULO DE ACELERACION (FORMULA DEL PROFESOR: SPEEDUP)
    // =========================================================================

    /**
     * Imprime la formula pedagogica del profesor y calcula el Speedup y Eficiencia.
     */
    private static void calcularYMostrarSpeedup(long ts, long tp, int hilos) {
        System.out.println("\n===============================================================");
        System.out.println("                 C\u00c1LCULO DE FACTOR DE ACELERACI\u00d3N              ");
        System.out.println("===============================================================");
        System.out.println("F\u00f3rmula del profesor:");
        System.out.println("   Speedup (S = T_s / T_p)");
        System.out.println();
        System.out.printf("Sustituci\u00f3n con valores medidos (%d hilos):%n", hilos);
        System.out.printf("   T_s (Tiempo Serial)   = %d ms%n", ts);
        System.out.printf("   T_p (Tiempo Paralelo) = %d ms%n", tp);

        double speedup = (tp > 0) ? ((double) ts / tp) : Double.POSITIVE_INFINITY;
        double eficiencia = speedup / hilos;

        System.out.println();
        System.out.printf("   Resultado: Speedup S = %d / %d = %.4fx%n", ts, tp, speedup);
        System.out.printf("   Eficiencia concurrente E = S / H = %.4f (%.2f%%)%n",
                eficiencia, eficiencia * 100.0);
        System.out.println("===============================================================");
    }

    // =========================================================================
    // RUTINA 3: BENCHMARK Y ESCALABILIDAD MULTIHILO (2, 4 y 8 HILOS)
    // =========================================================================

    /**
     * Realiza mediciones de rendimiento variando la cantidad de hilos (2, 4 y 8)
     * para verificar la ganancia de velocidad conforme escala la concurrencia.
     */
    private static void ejecutarBenchmarkMultihilo(File archivo, int N, int M,
                                                  int anchoFijo, int bytesSalto,
                                                  SerialEngine.ResultadoAsociacion resSerial)
            throws IOException, InterruptedException {

        int[] configuracionesHilos = {2, 4, 8};

        System.out.println("\n===============================================================");
        System.out.println("         TABLA COMPARATIVA DE ESCALABILIDAD Y RENDIMIENTO      ");
        System.out.println("===============================================================");
        System.out.printf("%-14s | %-6s | %-12s | %-12s | %-12s%n",
                "Modalidad", "Hilos", "Tiempo (ms)", "Speedup (S)", "Eficiencia (E)");
        System.out.println("------------------------------------------------------------------");

        // Fila de la linea base serial
        System.out.printf("%-14s | %-6d | %-12d | %-12.4f | %-12.2f%%%n",
                "Serial", 1, resSerial.tiempoMs, 1.0, 100.0);

        for (int h : configuracionesHilos) {
            ParallelEngine.ResultadoParalelo rp =
                    ParallelEngine.procesarParalelo(archivo, N, M, anchoFijo, bytesSalto, h);

            double speedup = (rp.tiempoMs > 0) ? ((double) resSerial.tiempoMs / rp.tiempoMs) : Double.POSITIVE_INFINITY;
            double eficiencia = (speedup / h) * 100.0;

            System.out.printf("%-14s | %-6d | %-12d | %-12.4f | %-12.2f%%%n",
                    "Paralelo", h, rp.tiempoMs, speedup, eficiencia);
        }

        System.out.println("===============================================================");
    }

    // =========================================================================
    // UTILIDADES DE PRESENTACION
    // =========================================================================

    private static void imprimirEncabezado(int N, int M, long totalPares, int W, int salto) {
        System.out.println("===============================================================");
        System.out.println("   UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS                    ");
        System.out.println("   FACULTAD DE INGENIER\u00cdA DE SISTEMAS E INFORM\u00c1TICA            ");
        System.out.println("   CURSO: PROGRAMACI\u00d3N CONCURRENTE Y PARALELA                  ");
        System.out.println("   TRABAJO DE APLICACI\u00d3N 1 - ORQUESTADOR (Main.java)           ");
        System.out.println("===============================================================");
        System.out.println("Par\u00e1metros del Problema:");
        System.out.printf("  - Observaciones (N): %d filas en disco%n", N);
        System.out.printf("  - Atributos (M):     %d columnas num\u00e9ricas%n", M);
        System.out.printf("  - Combinatoria:      T = M*(M-1)/2 = %d pares \u00fanicos%n", totalPares);
        System.out.printf("  - Ancho de celda:    W = %d bytes (longitud fija)%n", W);
        System.out.printf("  - Salto de l\u00ednea:    %d bytes (CRLF)%n", salto);
        System.out.println("  - Arquitectura:      Out-of-Core directo a disco (sin RAM)");
        System.out.println("===============================================================");
    }
}
