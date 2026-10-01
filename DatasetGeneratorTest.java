import java.io.File;

public final class DatasetGeneratorTest {

    private DatasetGeneratorTest() {
    }

    public static void main(String[] args) {
        boolean ok = true;
        File normal = new File("dataset_test.txt");
        File overflow = new File("dataset_overflow_test.txt");

        try {
            int N = 100;
            int M = 5;
            int W = 10;

            long filaBytes = DatasetGenerator.calcularBytesPorFila(M, W);
            long totalBytes = DatasetGenerator.calcularTamanoArchivo(N, M, W);
            ok &= verificar(filaBytes == 52L, "Bytes por fila = 52");
            ok &= verificar(totalBytes == 5200L, "Tamano total = 5200 bytes");

            DatasetGenerator.generarDatasetAleatorio(normal.getPath(), N, M,
                    100, 9999, W, 12345L);
            ok &= verificar(normal.length() == totalBytes,
                    "El archivo generado conserva exactamente el ancho fijo");
            DatasetGenerator.validarArchivo(normal, N, M, W);

            boolean rechazoOverflow = false;
            try {
                DatasetGenerator.generarDatasetAleatorio(overflow.getPath(), 2, 2,
                        1000, 1000, 3, 1L);
            } catch (IllegalArgumentException esperado) {
                rechazoOverflow = true;
            }
            ok &= verificar(rechazoOverflow,
                    "Se rechaza un valor que excede el ancho fijo W");

            boolean rechazoDimension = false;
            try {
                DatasetGenerator.validarDimensiones(1, 2, 10);
            } catch (IllegalArgumentException esperado) {
                rechazoDimension = true;
            }
            ok &= verificar(rechazoDimension, "Se rechaza N < 2");

        } catch (Exception e) {
            e.printStackTrace();
            ok = false;
        } finally {
            normal.delete();
            overflow.delete();
        }

        if (!ok) {
            System.err.println("DatasetGeneratorTest: FALLO");
            System.exit(1);
        }
        System.out.println("DatasetGeneratorTest: OK");
    }

    private static boolean verificar(boolean condicion, String mensaje) {
        System.out.printf("[%s] %s%n", condicion ? "OK" : "ERROR", mensaje);
        return condicion;
    }
}
