import java.io.File;

public final class ParallelEngineTest {

    private ParallelEngineTest() {
    }

    public static void main(String[] args) {
        boolean ok = true;
        File dataset = new File("parallel_test_dataset.txt");
        File serialOut = new File("parallel_test_serial.bin");
        int N = 200;
        int M = 10;
        int W = DatasetGenerator.DEFAULT_W;
        int salto = DatasetGenerator.BYTES_SALTO_LINEA;
        long T = (long) M * (M - 1) / 2;

        try {
            DatasetGenerator.generarDatasetCorrelacionado(dataset.getPath(), N, M, W, 9876L);
            SerialEngine.ResultadoAsociacion serial = SerialEngine.procesarSerial(
                    dataset, N, M, W, salto, null, serialOut);

            int[] configuraciones = {1, 2, 4, 8};
            for (int h : configuraciones) {
                File parallelOut = new File("parallel_test_" + h + ".bin");
                try {
                    ParallelEngine.ResultadoParalelo paralelo = ParallelEngine.procesarParalelo(
                            dataset, N, M, W, salto, h, null, parallelOut);

                    long diferencia = ResultFileManager.encontrarPrimeraDiferencia(
                            serialOut, parallelOut, T);
                    ok &= verificar(diferencia == -1L,
                            h + " hilo(s): todos los coeficientes coinciden bit a bit");
                    ok &= verificar(paralelo.totalPares == serial.totalPares,
                            h + " hilo(s): mismo total de pares");
                    ok &= verificar(paralelo.colMax1 == serial.colMax1
                                    && paralelo.colMax2 == serial.colMax2,
                            h + " hilo(s): mismo par maximo");
                    ok &= verificar(Double.doubleToRawLongBits(paralelo.valorMax)
                                    == Double.doubleToRawLongBits(serial.valorMax),
                            h + " hilo(s): mismo valor maximo bit a bit");
                } finally {
                    parallelOut.delete();
                }
            }

            File pequeno = new File("parallel_small_dataset.txt");
            try {
                DatasetGenerator.generarDatasetCorrelacionado(pequeno.getPath(), 20, 3, W, 44L);
                ParallelEngine.ResultadoParalelo limitado = ParallelEngine.procesarParalelo(
                        pequeno, 20, 3, W, salto, 8);
                ok &= verificar(limitado.numHilos == 3,
                        "Si H > T se limita el numero efectivo de hilos a T");
            } finally {
                pequeno.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
            ok = false;
        } finally {
            dataset.delete();
            serialOut.delete();
        }

        if (!ok) {
            System.err.println("ParallelEngineTest: FALLO");
            System.exit(1);
        }
        System.out.println("ParallelEngineTest: OK");
    }

    private static boolean verificar(boolean condicion, String mensaje) {
        System.out.printf("[%s] %s%n", condicion ? "OK" : "ERROR", mensaje);
        return condicion;
    }
}
