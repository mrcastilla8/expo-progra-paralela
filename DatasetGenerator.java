import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

public final class DatasetGenerator {

    public static final int DEFAULT_W = 10;
    public static final String SALTO_LINEA = "\r\n";
    public static final int BYTES_SALTO_LINEA = 2;
    public static final long DEFAULT_SEED = 20260917L;

    private DatasetGenerator() {
    }

    public static long calcularBytesPorFila(int M, int W) {
        if (M <= 0 || W <= 0) {
            throw new IllegalArgumentException("M y W deben ser mayores a cero");
        }
        return ((long) M * W) + BYTES_SALTO_LINEA;
    }

    public static long calcularTamanoArchivo(int N, int M, int W) {
        if (N <= 0) {
            throw new IllegalArgumentException("N debe ser mayor a cero");
        }
        return (long) N * calcularBytesPorFila(M, W);
    }

    public static void validarDimensiones(int N, int M, int W) {
        if (N < 2) {
            throw new IllegalArgumentException("N debe ser al menos 2 para calcular correlacion");
        }
        if (M < 2) {
            throw new IllegalArgumentException("M debe ser al menos 2 para calcular asociaciones");
        }
        if (W <= 0) {
            throw new IllegalArgumentException("W debe ser mayor a cero");
        }
    }

    public static void validarArchivo(File archivo, int N, int M, int W) throws IOException {
        if (archivo == null || !archivo.isFile()) {
            throw new IOException("El dataset no existe o no es un archivo regular");
        }
        long esperado = calcularTamanoArchivo(N, M, W);
        long real = archivo.length();
        if (real != esperado) {
            throw new IOException("Tamano de dataset incompatible con N/M/W. Esperado="
                    + esperado + " bytes, real=" + real + " bytes");
        }
    }

    /**
     * Escribe un entero con ancho fijo sin crear Strings temporales por celda.
     */
    private static void escribirNumeroFijo(OutputStream os, long numero, byte[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) ' ';
        }

        long n = numero;
        boolean negativo = n < 0;
        if (!negativo) {
            n = -n; // trabajar en dominio negativo evita overflow con Long.MIN_VALUE
        }

        int pos = buffer.length - 1;
        do {
            if (pos < 0) {
                throw new IllegalArgumentException("El valor " + numero
                        + " excede el ancho fijo W=" + buffer.length);
            }
            int digito = (int) (-(n % 10));
            buffer[pos--] = (byte) ('0' + digito);
            n /= 10;
        } while (n != 0);

        if (negativo) {
            if (pos < 0) {
                throw new IllegalArgumentException("El valor " + numero
                        + " excede el ancho fijo W=" + buffer.length);
            }
            buffer[pos] = (byte) '-';
        }

        os.write(buffer);
    }

    public static void generarDatasetAleatorio(String nombreArchivo, int N, int M,
                                                int minVal, int maxVal, int W) throws IOException {
        generarDatasetAleatorio(nombreArchivo, N, M, minVal, maxVal, W, DEFAULT_SEED);
    }

    public static void generarDatasetAleatorio(String nombreArchivo, int N, int M,
                                                int minVal, int maxVal, int W,
                                                long seed) throws IOException {
        validarDimensiones(N, M, W);
        if (minVal > maxVal) {
            throw new IllegalArgumentException("minVal no puede ser mayor que maxVal");
        }
        long rangoLong = (long) maxVal - minVal + 1L;
        if (rangoLong <= 0 || rangoLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rango aleatorio no soportado");
        }

        Random random = new Random(seed);
        int rango = (int) rangoLong;
        byte[] bufferCelda = new byte[W];
        byte[] bytesSalto = SALTO_LINEA.getBytes(StandardCharsets.US_ASCII);

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(nombreArchivo), 65536)) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    int valor = minVal + random.nextInt(rango);
                    escribirNumeroFijo(os, valor, bufferCelda);
                }
                os.write(bytesSalto);
            }
        }
    }

    public static void generarDatasetCorrelacionado(String nombreArchivo, int N, int M, int W) throws IOException {
        generarDatasetCorrelacionado(nombreArchivo, N, M, W, DEFAULT_SEED);
    }

    public static void generarDatasetCorrelacionado(String nombreArchivo, int N, int M,
                                                     int W, long seed) throws IOException {
        validarDimensiones(N, M, W);

        Random random = new Random(seed);
        byte[] bufferCelda = new byte[W];
        byte[] bytesSalto = SALTO_LINEA.getBytes(StandardCharsets.US_ASCII);

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(nombreArchivo), 65536)) {
            for (int i = 0; i < N; i++) {
                int x = 100 + random.nextInt(900);
                escribirNumeroFijo(os, x, bufferCelda);

                long y = (long) x * 10L;
                escribirNumeroFijo(os, y, bufferCelda);

                for (int j = 2; j < M; j++) {
                    int r = 50 + random.nextInt(950);
                    escribirNumeroFijo(os, r, bufferCelda);
                }
                os.write(bytesSalto);
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("Nombre del archivo [dataset.txt]: ");
            String filename = scanner.nextLine().trim();
            if (filename.isEmpty()) {
                filename = "dataset.txt";
            }

            System.out.print("Filas N: ");
            int N = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Columnas M: ");
            int M = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Correlacionado (1=Si, 0=No): ");
            boolean correlacionado = "1".equals(scanner.nextLine().trim());

            validarDimensiones(N, M, DEFAULT_W);
            long esperado = calcularTamanoArchivo(N, M, DEFAULT_W);
            long inicioNs = System.nanoTime();

            if (correlacionado) {
                generarDatasetCorrelacionado(filename, N, M, DEFAULT_W);
            } else {
                generarDatasetAleatorio(filename, N, M, 100, 9999, DEFAULT_W);
            }

            long duracionNs = System.nanoTime() - inicioNs;
            File archivo = new File(filename);
            validarArchivo(archivo, N, M, DEFAULT_W);

            System.out.println("Dataset generado correctamente.");
            System.out.println("Archivo: " + archivo.getAbsolutePath());
            System.out.println("Tamano fisico: " + archivo.length() + " bytes (esperado: " + esperado + ")");
            System.out.printf("Tiempo de escritura: %.3f ms%n", duracionNs / 1_000_000.0);
        } catch (RuntimeException | IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
}
