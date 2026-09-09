import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

public class DatasetGenerator {

    // Ancho fijo estándar por celda / columna (en caracteres / bytes)
    public static final int DEFAULT_W = 10;

    // Salto de línea determinista (CRLF = \r\n = 2 bytes)
    public static final String SALTO_LINEA = "\r\n";
    public static final int BYTES_SALTO_LINEA = SALTO_LINEA.getBytes(StandardCharsets.US_ASCII).length; // 2 bytes

    // FÓRMULAS MATEMÁTICAS DE DIMENSIONAMIENTO

    /**
     * Calcula la cantidad total de bytes que mide una fila completa en disco:
     * FilaBytes = (M * W) + BytesSaltoDeLinea
     */
    public static long calcularBytesPorFila(int M, int W) {
        return ((long) M * W) + BYTES_SALTO_LINEA;
    }

    /**
     * Calcula el tamaño exacto esperado del archivo en disco:
     * TamanoEsperado = N * FilaBytes
     */
    public static long calcularTamanoArchivo(int N, int M, int W) {
        return (long) N * calcularBytesPorFila(M, W);
    }

    // =========================================================================
    // FORMATEO CON PADDING DE ANCHO FIJO
    // =========================================================================

    /**
     * Aplica padding con espacios a la izquierda para garantizar que cada número
     * ocupe exactamente W caracteres (ej. 785 -> "       785").
     */
    public static String formatearNumero(long numero, int W) {
        return String.format("%" + W + "d", numero);
    }

    // =========================================================================
    // GENERACIÓN DIRECTA A DISCO (OUT-OF-CORE)
    // =========================================================================

    /**
     * Genera un archivo con N filas y M columnas de números enteros aleatorios [minVal, maxVal].
     * Escribe directamente al flujo de disco sin almacenar ninguna matriz en memoria RAM.
     */
    public static void generarDatasetAleatorio(String nombreArchivo, int N, int M, int minVal, int maxVal, int W) throws IOException {
        Random random = new Random();
        int rango = maxVal - minVal + 1;
        byte[] bytesSalto = SALTO_LINEA.getBytes(StandardCharsets.US_ASCII);

        // Buffer de escritura directo a disco (sin almacenar el dataset en RAM)
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(nombreArchivo), 65536)) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    int valor = minVal + random.nextInt(rango);
                    String celda = formatearNumero(valor, W);
                    os.write(celda.getBytes(StandardCharsets.US_ASCII));
                }
                os.write(bytesSalto);
            }
            os.flush();
        }
    }

    /**
     * Genera un dataset con columnas correlacionadas para validar analíticamente:
     * - Columna 0: Valor base X (ej. 100 a 999)
     * - Columna 1: Y = 10 * X (Correlación Pearson = +1.0 con Col 0, según ejemplo de Pablito)
     * - Columnas restantes: Valores aleatorios independientes
     */
    public static void generarDatasetCorrelacionado(String nombreArchivo, int N, int M, int W) throws IOException {
        if (M < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 columnas para el dataset correlacionado.");
        }
        Random random = new Random();
        byte[] bytesSalto = SALTO_LINEA.getBytes(StandardCharsets.US_ASCII);

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(nombreArchivo), 65536)) {
            for (int i = 0; i < N; i++) {
                // Columna 0: Base X
                int x = 100 + random.nextInt(900);
                String col0 = formatearNumero(x, W);
                os.write(col0.getBytes(StandardCharsets.US_ASCII));

                // Columna 1: Y = 10 * X (asociación lineal directa perfecta)
                long y = (long) x * 10;
                String col1 = formatearNumero(y, W);
                os.write(col1.getBytes(StandardCharsets.US_ASCII));

                // Columnas 2 .. M-1: Valores independientes
                for (int j = 2; j < M; j++) {
                    int r = 50 + random.nextInt(950);
                    String colJ = formatearNumero(r, W);
                    os.write(colJ.getBytes(StandardCharsets.US_ASCII));
                }

                os.write(bytesSalto);
            }
            os.flush();
        }
    }

    // =========================================================================
    // MÉTODO PRINCIPAL (CONSOLA DE GENERACIÓN)
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("        GENERADOR DE DATASET DE ANCHO FIJO (OUT-OF-CORE)       ");
        System.out.println("===============================================================");
        System.out.println("Configuracion de formato:");
        System.out.println(" - Ancho de Celda (W): " + DEFAULT_W + " bytes");
        System.out.println(" - Salto de linea: " + BYTES_SALTO_LINEA + " bytes (CRLF)");
        System.out.println("---------------------------------------------------------------");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese nombre del archivo (ej. dataset.txt): ");
        String filename = scanner.nextLine().trim();
        if (filename.isEmpty()) filename = "dataset.txt";

        System.out.print("Ingrese numero de Filas (N): ");
        int N = scanner.nextInt();

        System.out.print("Ingrese numero de Columnas (M): ");
        int M = scanner.nextInt();

        System.out.print("¿Generar dataset correlacionado (ejemplo clase factor 10)? (1=Si, 0=No): ");
        int correlacionado = scanner.nextInt();

        try {
            long bytesEsperados = calcularTamanoArchivo(N, M, DEFAULT_W);
            System.out.printf("Tamano teorico exacto en disco: %d bytes (%.2f KB / %.2f MB)\n",
                    bytesEsperados, bytesEsperados / 1024.0, bytesEsperados / (1024.0 * 1024.0));

            long t1 = System.currentTimeMillis();
            if (correlacionado == 1) {
                generarDatasetCorrelacionado(filename, N, M, DEFAULT_W);
            } else {
                generarDatasetAleatorio(filename, N, M, 100, 9999, DEFAULT_W);
            }
            long t2 = System.currentTimeMillis();

            File f = new File(filename);
            System.out.println("---------------------------------------------------------------");
            System.out.println("Dataset generado exitosamente directamente en disco:");
            System.out.println(" - Archivo: " + f.getAbsolutePath());
            System.out.println(" - Tamano real en disco: " + f.length() + " bytes");
            System.out.println(" - Coincidencia exacta bit a bit: " + (f.length() == bytesEsperados ? "CORRECTO [OK]" : "ERROR [DESALINEADO]"));
            System.out.println(" - Tiempo de escritura: " + (t2 - t1) + " ms");
            System.out.println("===============================================================");

        } catch (IOException e) {
            System.err.println("Error al generar el dataset: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
