import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Gestiona archivos de resultados de correlacion en formato binario fijo.
 * Cada par ocupa exactamente 8 bytes (un double IEEE-754).
 */
public final class ResultFileManager implements AutoCloseable {

    public static final int BYTES_POR_RESULTADO = Long.BYTES;

    private final RandomAccessFile raf;
    private final long totalResultados;

    public ResultFileManager(File archivo, long totalResultados, boolean preparar) throws IOException {
        if (archivo == null) {
            throw new IllegalArgumentException("El archivo de resultados no puede ser null");
        }
        if (totalResultados < 0) {
            throw new IllegalArgumentException("El total de resultados no puede ser negativo");
        }

        this.totalResultados = totalResultados;
        this.raf = new RandomAccessFile(archivo, "rw");

        if (preparar) {
            this.raf.setLength(totalResultados * BYTES_POR_RESULTADO);
        } else {
            long esperado = totalResultados * BYTES_POR_RESULTADO;
            if (this.raf.length() != esperado) {
                this.raf.close();
                throw new IOException("Tamano invalido del archivo de resultados. Esperado="
                        + esperado + " bytes, real=" + archivo.length() + " bytes");
            }
        }
    }

    public void escribir(long indice, double valor) throws IOException {
        validarIndice(indice);
        raf.seek(indice * BYTES_POR_RESULTADO);
        raf.writeLong(Double.doubleToRawLongBits(valor));
    }

    public long leerBits(long indice) throws IOException {
        validarIndice(indice);
        raf.seek(indice * BYTES_POR_RESULTADO);
        return raf.readLong();
    }

    private void validarIndice(long indice) {
        if (indice < 0 || indice >= totalResultados) {
            throw new IndexOutOfBoundsException("Indice de resultado fuera de rango: " + indice);
        }
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    /**
     * Compara todos los resultados mediante sus bits IEEE-754.
     * Retorna -1 si ambos archivos son identicos; de lo contrario retorna
     * el primer indice lineal cuyo double difiere bit a bit.
     */
    public static long encontrarPrimeraDiferencia(File archivoA, File archivoB,
                                                   long totalResultados) throws IOException {
        try (ResultFileManager a = new ResultFileManager(archivoA, totalResultados, false);
             ResultFileManager b = new ResultFileManager(archivoB, totalResultados, false)) {

            for (long i = 0; i < totalResultados; i++) {
                if (a.leerBits(i) != b.leerBits(i)) {
                    return i;
                }
            }
            return -1L;
        }
    }
}
