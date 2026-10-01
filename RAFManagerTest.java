import java.io.File;
import java.io.RandomAccessFile;

public final class RAFManagerTest {

    private RAFManagerTest() {
    }

    public static void main(String[] args) {
        boolean ok = true;
        File archivo = new File("raf_test_dataset.txt");

        try {
            int N = 20;
            int M = 4;
            int W = DatasetGenerator.DEFAULT_W;
            DatasetGenerator.generarDatasetCorrelacionado(archivo.getPath(), N, M, W, 7L);

            try (RAFManager raf = new RAFManager(archivo, N, M, W,
                    DatasetGenerator.BYTES_SALTO_LINEA)) {
                double x0 = raf.leerCelda(0, 0);
                double y0 = raf.leerCelda(0, 1);
                ok &= verificar(y0 == x0 * 10.0,
                        "El offset directo recupera correctamente columnas correlacionadas");

                double xUltimo = raf.leerCelda(N - 1, 0);
                double yUltimo = raf.leerCelda(N - 1, 1);
                ok &= verificar(yUltimo == xUltimo * 10.0,
                        "El acceso directo funciona en la ultima fila");
            }

            try (RandomAccessFile truncado = new RandomAccessFile(archivo, "rw")) {
                truncado.setLength(archivo.length() - 1);
            }

            boolean rechazoTruncado = false;
            try (RAFManager ignored = new RAFManager(archivo, N, M, W,
                    DatasetGenerator.BYTES_SALTO_LINEA)) {
                // No debe llegar aqui.
            } catch (Exception esperado) {
                rechazoTruncado = true;
            }
            ok &= verificar(rechazoTruncado, "Se rechaza un dataset truncado");

        } catch (Exception e) {
            e.printStackTrace();
            ok = false;
        } finally {
            archivo.delete();
        }

        if (!ok) {
            System.err.println("RAFManagerTest: FALLO");
            System.exit(1);
        }
        System.out.println("RAFManagerTest: OK");
    }

    private static boolean verificar(boolean condicion, String mensaje) {
        System.out.printf("[%s] %s%n", condicion ? "OK" : "ERROR", mensaje);
        return condicion;
    }
}
