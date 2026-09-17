import java.io.File;
import java.io.IOException;

/**
 * Script de verificacion rapida de SerialEngine.
 * Genera un dataset correlacionado (Col0 * 10 = Col1 -> Pearson = 1.0)
 * y ejecuta el procesamiento serial para validar resultados.
 */
public class VerificarSerial {
    public static void main(String[] args) throws IOException {
        int N = 100; // filas
        int M = 5; // columnas
        int W = DatasetGenerator.DEFAULT_W;
        int salto = DatasetGenerator.BYTES_SALTO_LINEA;
        String archivo = "verificar_serial_test.txt";

        System.out.println("============================================================");
        System.out.println("  VERIFICACION DE SerialEngine");
        System.out.println("============================================================");

        // Genera dataset con correlacion perfecta entre Col0 y Col1
        DatasetGenerator.generarDatasetCorrelacionado(archivo, N, M, W);
        System.out.println("Dataset generado: N=" + N + ", M=" + M);
        System.out.println("Total pares esperados: T = " + (M * (M - 1) / 2));

        // Ejecutar procesamiento serial
        File f = new File(archivo);
        SerialEngine.ResultadoAsociacion res = SerialEngine.procesarSerial(f, N, M, W, salto);

        // Imprimir resultados
        System.out.println("------------------------------------------------------------");
        System.out.println("RESULTADOS SERIAL:");
        System.out.println("  Pares evaluados: " + res.totalPares);
        System.out.printf("  MAX: Columnas (%d, %d) -> Pearson = %.10f%n",
                res.colMax1, res.colMax2, res.valorMax);
        System.out.printf("  MIN: Columnas (%d, %d) -> Pearson = %.10f%n",
                res.colMin1, res.colMin2, res.valorMin);
        System.out.println("  Tiempo serial T_s = " + res.tiempoMs + " ms");

        // Validacion automatica
        System.out.println("------------------------------------------------------------");
        boolean okMax = (res.colMax1 == 0 && res.colMax2 == 1);
        boolean okVal = (Math.abs(res.valorMax - 1.0) < 0.0001);

        if (okMax && okVal) {
            System.out.println("  >>> CORRELACION MAXIMA CORRECTA: (0,1) = 1.0 [OK] <<<");
        } else {
            System.out.println("  >>> ERROR: Se esperaba max en (0,1) con valor 1.0 <<<");
        }
        System.out.println("============================================================");

        // Limpieza
        if (f.exists())
            f.delete();
    }
}
