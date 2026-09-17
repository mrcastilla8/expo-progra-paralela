import java.io.File;
import java.io.IOException;

public final class ParallelEngine {

    // =========================================================================
    // ESTRUCTURA DE RESULTADOS PARALELOS
    // =========================================================================

    /**
     * Estructura plana con tipos primitivos que almacena los resultados
     * del procesamiento paralelo: extremos globales, tiempo T_p y número de hilos.
     */
    public static final class ResultadoParalelo {
        // Par con correlación MÁXIMA
        public int colMax1;
        public int colMax2;
        public double valorMax;

        // Par con correlación MÍNIMA
        public int colMin1;
        public int colMin2;
        public double valorMin;

        // Total de pares evaluados globalmente
        public long totalPares;

        // Número de hilos utilizados
        public int numHilos;

        // Tiempo de ejecución paralelo en milisegundos (T_p)
        public long tiempoMs;
    }

    // =========================================================================
    // TRABAJADOR CONCURRENTE (WORKER THREAD)
    // =========================================================================

    /**
     * Tarea ejecutable para cada hilo concurrente.
     * Mantiene total aislamiento de recursos y almacena sus propios extremos locales.
     */
    private static final class WorkerThread implements Runnable {
        private final int idHilo;
        private final File archivo;
        private final int N;
        private final int M;
        private final int anchoFijo;
        private final int bytesSalto;
        private final long ini;
        private final long fin;

        // Extremos locales de este hilo (sin contención con otros hilos)
        int colMax1;
        int colMax2;
        double valorMax;

        int colMin1;
        int colMin2;
        double valorMin;

        long paresEvaluados;
        Throwable error;

        WorkerThread(int idHilo, File archivo, int N, int M, int anchoFijo,
                     int bytesSalto, long ini, long fin) {
            this.idHilo = idHilo;
            this.archivo = archivo;
            this.N = N;
            this.M = M;
            this.anchoFijo = anchoFijo;
            this.bytesSalto = bytesSalto;
            this.ini = ini;
            this.fin = fin;

            // Inicialización de extremos locales
            this.valorMax = -2.0; // Pearson mínimo posible es -1.0
            this.valorMin =  2.0; // Pearson máximo posible es +1.0
            this.paresEvaluados = 0;
            this.error = null;
        }

        @Override
        public void run() {
            // Si el hilo no tiene pares asignados (ej. más hilos que tareas), termina de inmediato
            if (ini >= fin) {
                return;
            }
            
            try (RAFManager raf = new RAFManager(archivo, N, M, anchoFijo, bytesSalto)) {

                // 1. Determinar el par inicial (j, k) correspondiente al índice lineal 'ini'
                int j = 0;
                long acumulado = 0;
                while (acumulado + (M - 1 - j) <= ini) {
                    acumulado += (M - 1 - j);
                    j++;
                }
                int k = (int) (j + 1 + (ini - acumulado));

                // 2. Iterar exactamente sobre los pares asignados [ini, fin)
                for (long p = ini; p < fin; p++) {

                    // Cálculo de Pearson out-of-core con acumuladores escalares primitivos
                    double r = SerialEngine.pearson(raf, N, j, k);

                    // Actualizar máximo local
                    if (r > valorMax) {
                        valorMax = r;
                        colMax1 = j;
                        colMax2 = k;
                    }

                    // Actualizar mínimo local
                    if (r < valorMin) {
                        valorMin = r;
                        colMin1 = j;
                        colMin2 = k;
                    }

                    paresEvaluados++;

                    // Avanzar al siguiente par combinatorio sin repetición
                    k++;
                    if (k == M) {
                        j++;
                        k = j + 1;
                    }
                }

            } catch (Throwable t) {
                this.error = t;
            }
        }
    }

    // =========================================================================
    // PROCESAMIENTO PARALELO MULTIHILO
    // =========================================================================

    /**
     * Ejecuta el procesamiento concurrente de asociaciones particionando los
     * T = M * (M - 1) / 2 pares entre numHilos hilos de ejecución.
     *
     * @param archivo    Archivo físico del dataset en disco.
     * @param N          Número de filas (observaciones).
     * @param M          Número de columnas (atributos).
     * @param anchoFijo  Ancho fijo de cada celda en bytes (W).
     * @param bytesSalto Bytes del salto de línea (2 para CRLF).
     * @param numHilos   Cantidad de hilos a desplegar (ej. 2, 4, 8).
     * @return ResultadoParalelo con extremos globales y tiempo Tp medido.
     * @throws IOException Si ocurre un error de acceso a disco en algún hilo.
     * @throws InterruptedException Si la espera de hilos es interrumpida.
     */
    public static ResultadoParalelo procesarParalelo(File archivo, int N, int M,
                                                     int anchoFijo, int bytesSalto,
                                                     int numHilos) throws IOException, InterruptedException {
        if (numHilos <= 0) {
            throw new IllegalArgumentException("El número de hilos debe ser mayor a 0: " + numHilos);
        }
        if (M < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 columnas para calcular asociaciones: " + M);
        }

        // Total de combinaciones sin repetición T = M * (M - 1) / 2
        long T = (long) M * (M - 1) / 2;

        // Limitar número de hilos si T es menor que numHilos
        int hilosEfectivos = (int) Math.min((long) numHilos, T);

        WorkerThread[] workers = new WorkerThread[hilosEfectivos];
        Thread[] threads = new Thread[hilosEfectivos];

        // =====================================================================
        // CRONÓMETRO: INICIO del tiempo paralelo T_p
        // =====================================================================
        long t1 = System.currentTimeMillis();

        // 1. Partición Equitativa de tareas y lanzamiento de hilos
        for (int t = 0; t < hilosEfectivos; t++) {
            // Rango determinista de tareas de las notas de clase: [ini, fin)
            long ini = (long) t * T / hilosEfectivos;
            long fin = (long) (t + 1) * T / hilosEfectivos;

            workers[t] = new WorkerThread(t, archivo, N, M, anchoFijo, bytesSalto, ini, fin);
            threads[t] = new Thread(workers[t], "ParallelWorker-" + t);
            threads[t].start();
        }

        // 2. Sincronización explícita: esperar a que todos los hilos concluyan su trabajo
        for (int t = 0; t < hilosEfectivos; t++) {
            threads[t].join();
        }

        // =====================================================================
        // CRONÓMETRO: FIN del tiempo paralelo T_p
        // =====================================================================
        long t2 = System.currentTimeMillis();

        // 3. Verificar si algún hilo capturó una excepción
        for (int t = 0; t < hilosEfectivos; t++) {
            if (workers[t].error != null) {
                if (workers[t].error instanceof IOException) {
                    throw (IOException) workers[t].error;
                }
                throw new RuntimeException("Error en hilo " + t + ": " + workers[t].error.getMessage(), workers[t].error);
            }
        }

        // 4. Reducción final de extremos locales a extremos globales
        ResultadoParalelo res = new ResultadoParalelo();
        res.numHilos = hilosEfectivos;
        res.tiempoMs = t2 - t1;
        res.totalPares = 0;
        res.valorMax = -2.0;
        res.valorMin =  2.0;

        for (int t = 0; t < hilosEfectivos; t++) {
            res.totalPares += workers[t].paresEvaluados;

            if (workers[t].valorMax > res.valorMax) {
                res.valorMax = workers[t].valorMax;
                res.colMax1 = workers[t].colMax1;
                res.colMax2 = workers[t].colMax2;
            }

            if (workers[t].valorMin < res.valorMin) {
                res.valorMin = workers[t].valorMin;
                res.colMin1 = workers[t].colMin1;
                res.colMin2 = workers[t].colMin2;
            }
        }

        return res;
    }
}
