import java.io.File;
import java.io.IOException;
import java.util.function.LongConsumer;

/**
 * SerialEngine — Integrante 3: Gabriel "Ingeniero Serial y Matematico"
 *
 * Implementa la logica base del procesamiento serial:
 *   1. Calculo de Correlacion de Pearson entre dos columnas, leyendo
 *      celda por celda desde disco (out-of-core) con 6 acumuladores
 *      escalares primitivos. Ningun arreglo almacena columnas completas.
 *   2. Combinatoria sin repeticion: T = M*(M-1)/2 pares unicos.
 *      Doble bucle (j = 0..M-1, k = j+1..M-1) segun la formula de Pablito.
 *   3. Rastreo de la correlacion maxima y minima con variables primitivas.
 *   4. Medicion del tiempo serial exacto T_s en milisegundos.
 *
 * Restricciones cumplidas (agents.md):
 *   - No se carga el dataset en memoria RAM.
 *   - No se usan colecciones dinamicas ni librerias externas.
 *   - Cada celda se lee con seek(offset) mediante RAFManager.
 *   - Solo tipos primitivos y buffers de tamano fijo.
 */
public final class SerialEngine {

    // =========================================================================
    // RESULTADO DE LA EJECUCION SERIAL
    // =========================================================================

    /**
     * Estructura plana con campos primitivos que almacena el resultado
     * del procesamiento serial: par maximo, par minimo y tiempo T_s.
     * No utiliza ninguna coleccion dinamica.
     */
    public static final class ResultadoAsociacion {
        // Par con correlacion MAXIMA
        public int colMax1;
        public int colMax2;
        public double valorMax;

        // Par con correlacion MINIMA
        public int colMin1;
        public int colMin2;
        public double valorMin;

        // Total de pares evaluados: T = M*(M-1)/2
        public long totalPares;

        // Tiempo serial en milisegundos
        public long tiempoMs;
    }

    // =========================================================================
    // CORRELACION DE PEARSON (OUT-OF-CORE)
    // =========================================================================

    /**
     * Calcula el Coeficiente de Correlacion de Pearson entre la columna
     * colJ y la columna colK del dataset almacenado en disco.
     *
     * Formula:
     *   r = [N * sum(XY) - sumX * sumY]
     *       / sqrt([N * sumX2 - sumX^2] * [N * sumY2 - sumY^2])
     *
     * Se usan 6 acumuladores escalares (double). Cada valor se lee
     * directamente del archivo con raf.leerCelda(fila, col) y se
     * acumula de inmediato sin almacenarse en arreglo alguno.
     *
     * @param raf  Manejador de acceso aleatorio (descriptor propio).
     * @param N    Numero de filas (observaciones).
     * @param colJ Indice de la primera columna (0-based).
     * @param colK Indice de la segunda columna (0-based).
     * @return Coeficiente de Pearson en el rango [-1.0, +1.0].
     */
    public static double pearson(RAFManager raf, int N, int colJ, int colK)
            throws IOException {
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;

        // Recorre fila por fila acumulando desde disco
        for (int fila = 0; fila < N; fila++) {
            double x = raf.leerCelda(fila, colJ);
            double y = raf.leerCelda(fila, colK);

            sumX  += x;
            sumY  += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }

        // Aplicacion de la formula de Pearson
        double numerador = (double) N * sumXY - sumX * sumY;
        double denominador = Math.sqrt(
                ((double) N * sumX2 - sumX * sumX)
              * ((double) N * sumY2 - sumY * sumY)
        );

        // Proteccion contra division por cero (columna constante)
        if (denominador == 0.0) {
            return 0.0;
        }

        return numerador / denominador;
    }

    // =========================================================================
    // PROCESAMIENTO SERIAL COMPLETO
    // =========================================================================

    /**
     * Ejecuta el procesamiento serial de asociaciones entre todas las
     * columnas del dataset en disco.
     *
     * Algoritmo:
     *   1. Abre su propio RAFManager (descriptor independiente).
     *   2. T = M * (M - 1) / 2 (total de pares unicos).
     *   3. Doble bucle j = 0..M-1, k = j+1..M-1 (sin repeticion).
     *      - Para cada par (j, k), calcula Pearson desde disco.
     *      - Actualiza max/min con variables escalares primitivas.
     *   4. Cierra el RAFManager.
     *   5. Retorna ResultadoAsociacion con max, min y T_s.
     *
     * @param archivo        Referencia al archivo del dataset.
     * @param N              Numero de filas.
     * @param M              Numero de columnas.
     * @param anchoFijo      Ancho de cada celda en bytes (W).
     * @param bytesSalto     Bytes del salto de linea (2 para CRLF).
     * @return Resultado con extremos y tiempo serial.
     */
    public static ResultadoAsociacion procesarSerial(File archivo, int N,
            int M, int anchoFijo, int bytesSalto) throws IOException {
        return procesarSerial(archivo, N, M, anchoFijo, bytesSalto, null);
    }

    /**
     * Ejecuta el procesamiento serial de asociaciones con reporte de progreso opcional.
     *
     * @param archivo          Referencia al archivo del dataset.
     * @param N                Numero de filas.
     * @param M                Numero de columnas.
     * @param anchoFijo        Ancho de cada celda en bytes (W).
     * @param bytesSalto       Bytes del salto de linea (2 para CRLF).
     * @param progressCallback Callback que recibe los pares completados (opcional).
     * @return Resultado con extremos y tiempo serial.
     */
    public static ResultadoAsociacion procesarSerial(File archivo, int N,
            int M, int anchoFijo, int bytesSalto, LongConsumer progressCallback) throws IOException {

        ResultadoAsociacion resultado = new ResultadoAsociacion();

        // Total de combinaciones sin repeticion (formula de Pablito)
        resultado.totalPares = (long) M * (M - 1) / 2;

        // Inicializar extremos con valores que seran reemplazados
        resultado.valorMax = -2.0; // Pearson minimo posible es -1.0
        resultado.valorMin =  2.0; // Pearson maximo posible es +1.0

        // =====================================================================
        // CRONOMETRO: INICIO del tiempo serial T_s
        // =====================================================================
        long t1 = System.currentTimeMillis();

        // Cada llamada a procesarSerial abre su propio RAFManager
        // (descriptor independiente, aislamiento de puntero de archivo).
        RAFManager raf = new RAFManager(archivo, N, M, anchoFijo, bytesSalto);

        long paresCompletados = 0;

        // Doble bucle combinatorio de Pablito: j de 0 a M-1, k de j+1 a M-1
        for (int j = 0; j < M - 1; j++) {
            for (int k = j + 1; k < M; k++) {

                // Calcula Pearson entre columna j y columna k desde disco
                double r = pearson(raf, N, j, k);

                // Actualiza maximo
                if (r > resultado.valorMax) {
                    resultado.valorMax = r;
                    resultado.colMax1 = j;
                    resultado.colMax2 = k;
                }

                // Actualiza minimo
                if (r < resultado.valorMin) {
                    resultado.valorMin = r;
                    resultado.colMin1 = j;
                    resultado.colMin2 = k;
                }

                paresCompletados++;
                if (progressCallback != null) {
                    progressCallback.accept(paresCompletados);
                }
            }
        }

        // Libera el descriptor de archivo
        raf.close();

        // =====================================================================
        // CRONOMETRO: FIN del tiempo serial T_s
        // =====================================================================
        long t2 = System.currentTimeMillis();
        resultado.tiempoMs = t2 - t1;

        return resultado;
    }
}
