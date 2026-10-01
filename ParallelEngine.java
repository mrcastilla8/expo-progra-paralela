import java.io.File;
import java.io.IOException;
import java.util.function.LongConsumer;

public final class ParallelEngine {

    private ParallelEngine() {
    }

    public static final class ResultadoParalelo {
        public int colMax1;
        public int colMax2;
        public double valorMax;
        public int colMin1;
        public int colMin2;
        public double valorMin;
        public long totalPares;
        public int numHilos;
        public long tiempoNs;
        public long tiempoMs;
    }

    private static final class WorkerThread implements Runnable {
        private final int idHilo;
        private final File archivo;
        private final File archivoResultados;
        private final int N;
        private final int M;
        private final int anchoFijo;
        private final int bytesSalto;
        private final long ini;
        private final long fin;
        private final long totalPares;

        int colMax1;
        int colMax2;
        double valorMax = -Double.MAX_VALUE;
        int colMin1;
        int colMin2;
        double valorMin = Double.MAX_VALUE;
        volatile long paresEvaluados;
        Throwable error;

        WorkerThread(int idHilo, File archivo, File archivoResultados,
                     int N, int M, int anchoFijo, int bytesSalto,
                     long ini, long fin, long totalPares) {
            this.idHilo = idHilo;
            this.archivo = archivo;
            this.archivoResultados = archivoResultados;
            this.N = N;
            this.M = M;
            this.anchoFijo = anchoFijo;
            this.bytesSalto = bytesSalto;
            this.ini = ini;
            this.fin = fin;
            this.totalPares = totalPares;
        }

        @Override
        public void run() {
            if (ini >= fin) {
                return;
            }

            try (RAFManager raf = new RAFManager(archivo, N, M, anchoFijo, bytesSalto);
                 ResultFileManager salida = archivoResultados == null
                         ? null
                         : new ResultFileManager(archivoResultados, totalPares, false)) {

                int j = 0;
                long acumulado = 0;
                while (acumulado + (M - 1L - j) <= ini) {
                    acumulado += (M - 1L - j);
                    j++;
                }
                int k = (int) (j + 1 + (ini - acumulado));

                for (long p = ini; p < fin; p++) {
                    double r = SerialEngine.pearson(raf, N, j, k);

                    if (salida != null) {
                        salida.escribir(p, r);
                    }

                    if (r > valorMax) {
                        valorMax = r;
                        colMax1 = j;
                        colMax2 = k;
                    }
                    if (r < valorMin) {
                        valorMin = r;
                        colMin1 = j;
                        colMin2 = k;
                    }

                    paresEvaluados++;
                    k++;
                    if (k == M) {
                        j++;
                        k = j + 1;
                    }
                }
            } catch (Throwable t) {
                error = t;
            }
        }
    }

    public static ResultadoParalelo procesarParalelo(File archivo, int N, int M,
                                                       int anchoFijo, int bytesSalto,
                                                       int numHilos) throws IOException, InterruptedException {
        return procesarParalelo(archivo, N, M, anchoFijo, bytesSalto, numHilos, null, null);
    }

    public static ResultadoParalelo procesarParalelo(File archivo, int N, int M,
                                                       int anchoFijo, int bytesSalto,
                                                       int numHilos,
                                                       LongConsumer progressCallback) throws IOException, InterruptedException {
        return procesarParalelo(archivo, N, M, anchoFijo, bytesSalto, numHilos,
                progressCallback, null);
    }

    public static ResultadoParalelo procesarParalelo(File archivo, int N, int M,
                                                       int anchoFijo, int bytesSalto,
                                                       int numHilos,
                                                       LongConsumer progressCallback,
                                                       File archivoResultados) throws IOException, InterruptedException {
        DatasetGenerator.validarDimensiones(N, M, anchoFijo);
        DatasetGenerator.validarArchivo(archivo, N, M, anchoFijo);
        if (numHilos <= 0) {
            throw new IllegalArgumentException("El numero de hilos debe ser mayor a cero");
        }

        long T = (long) M * (M - 1) / 2;
        int hilosEfectivos = (int) Math.min((long) numHilos, T);

        if (archivoResultados != null) {
            try (ResultFileManager ignored = new ResultFileManager(archivoResultados, T, true)) {
                // Preasignacion unica antes de iniciar los hilos.
            }
        }

        WorkerThread[] workers = new WorkerThread[hilosEfectivos];
        Thread[] threads = new Thread[hilosEfectivos];

        long inicioNs = System.nanoTime();

        for (int t = 0; t < hilosEfectivos; t++) {
            long ini = (long) t * T / hilosEfectivos;
            long fin = (long) (t + 1) * T / hilosEfectivos;
            workers[t] = new WorkerThread(t, archivo, archivoResultados, N, M,
                    anchoFijo, bytesSalto, ini, fin, T);
            threads[t] = new Thread(workers[t], "ParallelWorker-" + t);
            threads[t].start();
        }

        if (progressCallback != null) {
            boolean activos;
            do {
                long totalEvaluados = 0;
                activos = false;
                for (int t = 0; t < hilosEfectivos; t++) {
                    totalEvaluados += workers[t].paresEvaluados;
                    activos |= threads[t].isAlive();
                }
                progressCallback.accept(totalEvaluados);
                if (activos) {
                    Thread.sleep(40L);
                }
            } while (activos);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long tiempoNs = System.nanoTime() - inicioNs;

        for (int t = 0; t < hilosEfectivos; t++) {
            if (workers[t].error != null) {
                if (workers[t].error instanceof IOException) {
                    throw (IOException) workers[t].error;
                }
                throw new RuntimeException("Error en hilo " + workers[t].idHilo,
                        workers[t].error);
            }
        }

        ResultadoParalelo res = new ResultadoParalelo();
        res.numHilos = hilosEfectivos;
        res.tiempoNs = tiempoNs;
        res.tiempoMs = tiempoNs / 1_000_000L;
        res.valorMax = -Double.MAX_VALUE;
        res.valorMin = Double.MAX_VALUE;

        for (WorkerThread worker : workers) {
            res.totalPares += worker.paresEvaluados;
            if (worker.valorMax > res.valorMax) {
                res.valorMax = worker.valorMax;
                res.colMax1 = worker.colMax1;
                res.colMax2 = worker.colMax2;
            }
            if (worker.valorMin < res.valorMin) {
                res.valorMin = worker.valorMin;
                res.colMin1 = worker.colMin1;
                res.colMin2 = worker.colMin2;
            }
        }

        return res;
    }
}
