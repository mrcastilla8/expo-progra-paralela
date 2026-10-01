import java.io.File;
import java.io.IOException;
import java.util.function.LongConsumer;

public final class SerialEngine {

    private SerialEngine() {
    }

    public static final class ResultadoAsociacion {
        public int colMax1;
        public int colMax2;
        public double valorMax;
        public int colMin1;
        public int colMin2;
        public double valorMin;
        public long totalPares;
        public long tiempoNs;
        public long tiempoMs;
    }

    public static double pearson(RAFManager raf, int N, int colJ, int colK) throws IOException {
        if (N < 2) {
            throw new IllegalArgumentException("N debe ser al menos 2");
        }

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;

        for (int fila = 0; fila < N; fila++) {
            double x = raf.leerCelda(fila, colJ);
            double y = raf.leerCelda(fila, colK);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }

        double numerador = (double) N * sumXY - sumX * sumY;
        double a = (double) N * sumX2 - sumX * sumX;
        double b = (double) N * sumY2 - sumY * sumY;
        double denominador = Math.sqrt(a * b);

        if (denominador == 0.0) {
            return 0.0;
        }
        return numerador / denominador;
    }

    public static ResultadoAsociacion procesarSerial(File archivo, int N, int M,
                                                      int anchoFijo, int bytesSalto) throws IOException {
        return procesarSerial(archivo, N, M, anchoFijo, bytesSalto, null, null);
    }

    public static ResultadoAsociacion procesarSerial(File archivo, int N, int M,
                                                      int anchoFijo, int bytesSalto,
                                                      LongConsumer progressCallback) throws IOException {
        return procesarSerial(archivo, N, M, anchoFijo, bytesSalto, progressCallback, null);
    }

    public static ResultadoAsociacion procesarSerial(File archivo, int N, int M,
                                                      int anchoFijo, int bytesSalto,
                                                      LongConsumer progressCallback,
                                                      File archivoResultados) throws IOException {
        DatasetGenerator.validarDimensiones(N, M, anchoFijo);
        DatasetGenerator.validarArchivo(archivo, N, M, anchoFijo);

        long totalEsperado = (long) M * (M - 1) / 2;
        if (archivoResultados != null) {
            try (ResultFileManager ignored = new ResultFileManager(archivoResultados, totalEsperado, true)) {
                // Solo preasigna el archivo. La escritura real abre su propio descriptor abajo.
            }
        }

        ResultadoAsociacion resultado = new ResultadoAsociacion();
        resultado.valorMax = -Double.MAX_VALUE;
        resultado.valorMin = Double.MAX_VALUE;

        long paresCompletados = 0;
        long inicioNs = System.nanoTime();

        try (RAFManager raf = new RAFManager(archivo, N, M, anchoFijo, bytesSalto);
             ResultFileManager salida = archivoResultados == null
                     ? null
                     : new ResultFileManager(archivoResultados, totalEsperado, false)) {

            for (int j = 0; j < M - 1; j++) {
                for (int k = j + 1; k < M; k++) {
                    double r = pearson(raf, N, j, k);

                    if (salida != null) {
                        salida.escribir(paresCompletados, r);
                    }

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
        }

        resultado.tiempoNs = System.nanoTime() - inicioNs;
        resultado.tiempoMs = resultado.tiempoNs / 1_000_000L;
        resultado.totalPares = paresCompletados;
        return resultado;
    }
}
