import java.io.File;
import java.io.IOException;

public class ParallelEngineTest {

    public static void main(String[] args) {
        int N = 200;
        int M = 10;
        int W = DatasetGenerator.DEFAULT_W;
        int salto = DatasetGenerator.BYTES_SALTO_LINEA;
        String archivo = "parallel_test_dataset.txt";

        System.out.println("===============================================================");
        System.out.println("        TEST DE VERIFICACION Y BENCHMARK: ParallelEngine       ");
        System.out.println("===============================================================");
        System.out.println("Configuracion de prueba:");
        System.out.println(" - Filas (N): " + N);
        System.out.println(" - Columnas (M): " + M);
        System.out.println(" - Total de pares unicos (T): " + ((long) M * (M - 1) / 2));
        System.out.println(" - Ancho de celda (W): " + W + " bytes");
        System.out.println("---------------------------------------------------------------");

        File f = new File(archivo);
        boolean todoOk = true;

        try {

            System.out.println("[PASO 1] Generando dataset de prueba en disco...");
            DatasetGenerator.generarDatasetCorrelacionado(archivo, N, M, W);
            System.out.println(" -> Dataset generado: " + f.length() + " bytes en disco.");

            System.out.println("\n[PASO 2] Ejecutando SerialEngine (linea base)...");
            SerialEngine.ResultadoAsociacion resSerial = SerialEngine.procesarSerial(f, N, M, W, salto);
            System.out.printf(" -> Serial (1 hilo): %d ms | Pares: %d%n", resSerial.tiempoMs, resSerial.totalPares);
            System.out.printf("    MAX: Col(%d, %d) = %.8f%n", resSerial.colMax1, resSerial.colMax2, resSerial.valorMax);
            System.out.printf("    MIN: Col(%d, %d) = %.8f%n", resSerial.colMin1, resSerial.colMin2, resSerial.valorMin);

            int[] configuracionesHilos = {2, 4, 8};
            ParallelEngine.ResultadoParalelo[] resultadosParalelos = new ParallelEngine.ResultadoParalelo[configuracionesHilos.length];

            System.out.println("\n[PASO 3] Ejecutando ParallelEngine con concurrencia...");
            for (int i = 0; i < configuracionesHilos.length; i++) {
                int h = configuracionesHilos[i];
                System.out.printf(" -> Ejecutando con %d hilos...", h);
                resultadosParalelos[i] = ParallelEngine.procesarParalelo(f, N, M, W, salto, h);
                System.out.printf(" Completado en %d ms.%n", resultadosParalelos[i].tiempoMs);
            }

            System.out.println("\n---------------------------------------------------------------");
            System.out.println("              VERIFICACION DE CONSISTENCIA NUMERICA            ");
            System.out.println("---------------------------------------------------------------");

            for (int i = 0; i < configuracionesHilos.length; i++) {
                int h = configuracionesHilos[i];
                ParallelEngine.ResultadoParalelo rp = resultadosParalelos[i];

                boolean paresCorrectos = (rp.totalPares == resSerial.totalPares);
                boolean maxParCorrecto = (rp.colMax1 == resSerial.colMax1 && rp.colMax2 == resSerial.colMax2);
                boolean maxValCorrecto = (Math.abs(rp.valorMax - resSerial.valorMax) < 1e-6);
                boolean minParCorrecto = (rp.colMin1 == resSerial.colMin1 && rp.colMin2 == resSerial.colMin2);
                boolean minValCorrecto = (Math.abs(rp.valorMin - resSerial.valorMin) < 1e-6);

                boolean hOk = paresCorrectos && maxParCorrecto && maxValCorrecto && minParCorrecto && minValCorrecto;
                if (!hOk) todoOk = false;

                System.out.printf("[%s] Configuracion con %d hilos:%n", (hOk ? "OK" : "ERROR"), h);
                System.out.printf("     - Total pares evaluados: %d (Esperado: %d) -> %s%n",
                        rp.totalPares, resSerial.totalPares, (paresCorrectos ? "OK" : "FALLO"));
                System.out.printf("     - Par Maximo: (%d, %d) r=%.6f (Esperado: (%d, %d) r=%.6f) -> %s%n",
                        rp.colMax1, rp.colMax2, rp.valorMax, resSerial.colMax1, resSerial.colMax2, resSerial.valorMax,
                        (maxParCorrecto && maxValCorrecto ? "OK" : "FALLO"));
                System.out.printf("     - Par Minimo: (%d, %d) r=%.6f (Esperado: (%d, %d) r=%.6f) -> %s%n",
                        rp.colMin1, rp.colMin2, rp.valorMin, resSerial.colMin1, resSerial.colMin2, resSerial.valorMin,
                        (minParCorrecto && minValCorrecto ? "OK" : "FALLO"));
            }

            System.out.println("\n===============================================================");
            System.out.println("         TABLA COMPARATIVA DE RENDIMIENTO Y SPEEDUP            ");
            System.out.println("===============================================================");
            System.out.printf("%-12s | %-12s | %-12s | %-12s%n", "Modalidad", "Tiempo (ms)", "Speedup (S)", "Eficiencia (E)");
            System.out.println("---------------------------------------------------------------");
            System.out.printf("%-12s | %-12d | %-12.2f | %-12.2f%n", "Serial (1)", resSerial.tiempoMs, 1.0, 1.0);

            for (int i = 0; i < configuracionesHilos.length; i++) {
                int h = configuracionesHilos[i];
                long tp = resultadosParalelos[i].tiempoMs;
                double speedup = (tp > 0) ? ((double) resSerial.tiempoMs / tp) : Double.POSITIVE_INFINITY;
                double eficiencia = speedup / h;
                System.out.printf("%-12s | %-12d | %-12.2f | %-12.2f%n",
                        h + " Hilos", tp, speedup, eficiencia);
            }
            System.out.println("===============================================================");

            if (todoOk) {
                System.out.println(" >>> TODAS LAS PRUEBAS PARALELAS PASARON EXITOSAMENTE [OK] <<< ");
            } else {
                System.out.println(" >>> SE ENCONTRARON DISCREPANCIAS EN LAS PRUEBAS PARALELAS <<< ");
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
            todoOk = false;
        } finally {
            if (f.exists()) {
                f.delete();
            }
        }
    }
}
