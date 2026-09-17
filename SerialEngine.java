import java.io.File;
import java.io.IOException;
import java.util.function.LongConsumer;

public final class SerialEngine {

    public static final class ResultadoAsociacion {

        public int colMax1;
        public int colMax2;
        public double valorMax;

        public int colMin1;
        public int colMin2;
        public double valorMin;

        public long totalPares;

        public long tiempoMs;
    }

    public static double pearson(RAFManager raf, int N, int colJ, int colK)
            throws IOException {
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;

        for (int fila = 0; fila < N; fila++) {
            double x = raf.leerCelda(fila, colJ);
            double y = raf.leerCelda(fila, colK);

            sumX  += x;
            sumY  += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }

        double numerador = (double) N * sumXY - sumX * sumY;
        double denominador = Math.sqrt(
                ((double) N * sumX2 - sumX * sumX)
              * ((double) N * sumY2 - sumY * sumY)
        );

        if (denominador == 0.0) {
            return 0.0;
        }

        return numerador / denominador;
    }

    public static ResultadoAsociacion procesarSerial(File archivo, int N,
            int M, int anchoFijo, int bytesSalto) throws IOException {
        return procesarSerial(archivo, N, M, anchoFijo, bytesSalto, null);
    }

    public static ResultadoAsociacion procesarSerial(File archivo, int N,
            int M, int anchoFijo, int bytesSalto, LongConsumer progressCallback) throws IOException {

        ResultadoAsociacion resultado = new ResultadoAsociacion();

        resultado.totalPares = (long) M * (M - 1) / 2;

        resultado.valorMax = -2.0;
        resultado.valorMin =  2.0;

        long t1 = System.currentTimeMillis();

        RAFManager raf = new RAFManager(archivo, N, M, anchoFijo, bytesSalto);

        long paresCompletados = 0;

        for (int j = 0; j < M - 1; j++) {
            for (int k = j + 1; k < M; k++) {

                double r = pearson(raf, N, j, k);

                if (r > resultado.valorMax) {
                    resultado.valorMax = r;
                    resultado.colMax1 = j;
                    resultado.colMax2 = k;
                }

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

        raf.close();

        long t2 = System.currentTimeMillis();
        resultado.tiempoMs = t2 - t1;

        return resultado;
    }
}
