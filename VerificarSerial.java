import java.io.File;

public final class VerificarSerial {

    private VerificarSerial() {
    }

    public static void main(String[] args) {
        int N = 100;
        int M = 5;
        int W = DatasetGenerator.DEFAULT_W;
        int salto = DatasetGenerator.BYTES_SALTO_LINEA;
        File dataset = new File("verificar_serial_test.txt");
        File resultados = new File("verificar_serial_resultados.bin");
        boolean ok = true;

        try {
            DatasetGenerator.generarDatasetCorrelacionado(dataset.getPath(), N, M, W, 2026L);
            SerialEngine.ResultadoAsociacion res = SerialEngine.procesarSerial(
                    dataset, N, M, W, salto, null, resultados);

            long paresEsperados = (long) M * (M - 1) / 2;
            ok &= verificar(res.totalPares == paresEsperados,
                    "Se procesan todos los pares unicos");
            ok &= verificar(res.colMax1 == 0 && res.colMax2 == 1,
                    "El maximo corresponde a las columnas (0,1)");
            ok &= verificar(Math.abs(res.valorMax - 1.0) < 1e-12,
                    "La correlacion conocida es aproximadamente +1");
            ok &= verificar(resultados.length() == paresEsperados * ResultFileManager.BYTES_POR_RESULTADO,
                    "Se persiste un resultado fijo de 8 bytes por par");
            ok &= verificar(res.tiempoNs > 0,
                    "El tiempo se registra con System.nanoTime");

        } catch (Exception e) {
            e.printStackTrace();
            ok = false;
        } finally {
            dataset.delete();
            resultados.delete();
        }

        if (!ok) {
            System.err.println("VerificarSerial: FALLO");
            System.exit(1);
        }
        System.out.println("VerificarSerial: OK");
    }

    private static boolean verificar(boolean condicion, String mensaje) {
        System.out.printf("[%s] %s%n", condicion ? "OK" : "ERROR", mensaje);
        return condicion;
    }
}
