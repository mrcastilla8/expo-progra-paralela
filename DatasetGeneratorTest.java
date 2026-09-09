import java.io.File;
import java.io.IOException;

/**
 * DatasetGeneratorTest
 * 
 * Verificación de las tareas exclusivas del Integrante 1:
 * - Padding de ancho fijo (W = 10).
 * - Exactitud de la cuadrícula de bytes en disco (Rows * Cols + Saltos de Línea).
 * - Escritura directa sin cargar estructuras a memoria RAM.
 */
public class DatasetGeneratorTest {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("               TEST UNITARIO: DATASET GENERATOR                ");
        System.out.println("===============================================================");

        boolean ok = true;

        // 1. Verificación de padding de espacios
        System.out.println("[TEST 1] Verificando padding con formato String.format(\"%10d\", 785)...");
        String formatted = DatasetGenerator.formatearNumero(785, 10);
        if (formatted.equals("       785") && formatted.length() == 10) {
            System.out.println(" -> Formato correcto: '" + formatted + "' (longitud = " + formatted.length() + ") [OK]");
        } else {
            System.err.println(" -> Formato incorrecto: '" + formatted + "' [ERROR]");
            ok = false;
        }

        // 2. Verificación de cálculo de bytes
        System.out.println("\n[TEST 2] Verificando formulas de bytes por fila y tamano total...");
        int N = 100;
        int M = 5;
        int W = 10;
        long filaBytes = DatasetGenerator.calcularBytesPorFila(M, W); // 5*10 + 2 = 52 bytes
        long tamanoTotal = DatasetGenerator.calcularTamanoArchivo(N, M, W); // 100 * 52 = 5200 bytes

        System.out.println(" -> Bytes por fila: " + filaBytes + " (Esperado: 52)");
        System.out.println(" -> Tamano total esperado en disco: " + tamanoTotal + " bytes (Esperado: 5200)");

        if (filaBytes == 52 && tamanoTotal == 5200) {
            System.out.println(" -> Formulas exactas [OK]");
        } else {
            System.err.println(" -> Error en calculo de formulas [ERROR]");
            ok = false;
        }

        // 3. Generación y comprobación de tamaño físico del archivo
        System.out.println("\n[TEST 3] Generando archivo 'dataset_test.txt' en disco...");
        String filename = "dataset_test.txt";
        try {
            DatasetGenerator.generarDatasetAleatorio(filename, N, M, 100, 9999, W);
            File f = new File(filename);
            long bytesReales = f.length();
            System.out.println(" -> Tamano en disco del archivo generado: " + bytesReales + " bytes");

            if (bytesReales == tamanoTotal) {
                System.out.println(" -> El tamano fisico coincide EXACTAMENTE con la formula matematica [OK]");
            } else {
                System.err.println(" -> DISCREPANCIA de bytes: Real=" + bytesReales + " vs Esperado=" + tamanoTotal + " [ERROR]");
                ok = false;
            }

            // Limpieza
            if (f.exists()) f.delete();

        } catch (IOException e) {
            System.err.println(" -> Error de I/O al generar archivo: " + e.getMessage());
            ok = false;
        }

        System.out.println("\n===============================================================");
        if (ok) {
            System.out.println("  >>> TODAS LAS PRUEBAS DEL DATASET GENERATOR PASARON EXITOSAMENTE <<<");
        } else {
            System.out.println("  >>> HUBIERON ERRORES EN LAS PRUEBAS <<<");
        }
        System.out.println("===============================================================");
    }
}
