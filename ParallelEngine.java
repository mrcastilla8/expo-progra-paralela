import java.io.File;
import java.io.IOException;
import java.util.function.LongConsumer;

public final class ParallelEngine {

    public static final class ResultadoParalelo {

        public int colMax1;
        public int colMax2;
        public double valorMax;

        public int colMin1;
        public int colMin2;
        public double valorMin;

        public long totalPares;

        public int numHilos;

        public long tiempoMs;
    }

    private static final class WorkerThread implements Runnable {
        private final int idHilo;
        private final File archivo;
        private final int N;
        private final int M;
        private final int anchoFijo;
        private final int bytesSalto;
        private final long ini;
        private final long fin;

        int colMax1;
        int colMax2;
        double valorMax;

        int colMin1;
        int colMin2;
        double valorMin;

        volatile long paresEvaluados;
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

            this.valorMax = -2.0;
            this.valorMin =  2.0;
            this.paresEvaluados = 0;
            this.error = null;
        }

        @Override
        public void run() {

            if (ini >= fin) {
                return;
            }

            try (RAFManager raf = new RAFManager(archivo, N, M, anchoFijo, bytesSalto)) {

                int j = 0;
                long acumulado = 0;
                while (acumulado + (M - 1 - j) <= ini) {
                    acumulado += (M - 1 - j);
                    j++;
                }
                int k = (int) (j + 1 + (ini - acumulado));

                for (long p = ini; p < fin; p++) {

                    double r = SerialEngine.pearson(raf, N, j, k);

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
                this.error = t;
            }
        }
    }

    public static ResultadoParalelo procesarParalelo(File archivo, int N, int M,
                                                     int anchoFijo, int bytesSalto,
                                                     int numHilos) throws IOException, InterruptedException {
        return procesarParalelo(archivo, N, M, anchoFijo, bytesSalto, numHilos, null);
    }

    public static ResultadoParalelo procesarParalelo(File archivo, int N, int M,
                                                     int anchoFijo, int bytesSalto,
                                                     int numHilos,
                                                     LongConsumer progressCallback)
            throws IOException, InterruptedException {
        if (numHilos <= 0) {
            throw new IllegalArgumentException("El número de hilos debe ser mayor a 0: " + numHilos);
        }
        if (M < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 columnas para calcular asociaciones: " + M);
        }

        long T = (long) M * (M - 1) / 2;

        int hilosEfectivos = (int) Math.min((long) numHilos, T);

        WorkerThread[] workers = new WorkerThread[hilosEfectivos];
        Thread[] threads = new Thread[hilosEfectivos];

        long t1 = System.currentTimeMillis();

        for (int t = 0; t < hilosEfectivos; t++) {

            long ini = (long) t * T / hilosEfectivos;
            long fin = (long) (t + 1) * T / hilosEfectivos;

            workers[t] = new WorkerThread(t, archivo, N, M, anchoFijo, bytesSalto, ini, fin);
            threads[t] = new Thread(workers[t], "ParallelWorker-" + t);
            threads[t].start();
        }

        if (progressCallback != null) {
            long totalEvaluados = 0;
            while (totalEvaluados < T) {
                totalEvaluados = 0;
                for (int t = 0; t < hilosEfectivos; t++) {
                    totalEvaluados += workers[t].paresEvaluados;
                }
                progressCallback.accept(totalEvaluados);

                boolean vivos = false;
                for (int t = 0; t < hilosEfectivos; t++) {
                    if (threads[t].isAlive()) {
                        vivos = true;
                        break;
                    }
                }
                if (!vivos) {
                    break;
                }
                Thread.sleep(40);
            }

            totalEvaluados = 0;
            for (int t = 0; t < hilosEfectivos; t++) {
                totalEvaluados += workers[t].paresEvaluados;
            }
            progressCallback.accept(totalEvaluados);
        }

        for (int t = 0; t < hilosEfectivos; t++) {
            threads[t].join();
        }

        long t2 = System.currentTimeMillis();

        for (int t = 0; t < hilosEfectivos; t++) {
            if (workers[t].error != null) {
                if (workers[t].error instanceof IOException) {
                    throw (IOException) workers[t].error;
                }
                throw new RuntimeException("Error en hilo " + t + ": " + workers[t].error.getMessage(), workers[t].error);
            }
        }

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
