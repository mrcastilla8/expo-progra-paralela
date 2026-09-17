import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public final class RAFManager implements AutoCloseable {

    private final RandomAccessFile raf;

    private final int filas;
    private final int columnas;
    private final int anchoFijo;
    private final int bytesSaltoDeLinea;

    private final byte[] buffer;

    public RAFManager(File archivo, int filas, int columnas, int anchoFijo,
            int bytesSaltoDeLinea) throws IOException {

        if (filas < 0 || columnas <= 0 || anchoFijo <= 0 || bytesSaltoDeLinea < 0) {
            throw new IllegalArgumentException("Dimensiones o anchos invalidos");
        }

        this.raf = new RandomAccessFile(archivo, "r");
        this.filas = filas;
        this.columnas = columnas;
        this.anchoFijo = anchoFijo;
        this.bytesSaltoDeLinea = bytesSaltoDeLinea;
        this.buffer = new byte[anchoFijo];
    }

    public double leerCelda(int fila, int columna) throws IOException {
        validarPosicion(fila, columna);

        long offset = ((long) fila * columnas + columna) * anchoFijo
                + (long) fila * bytesSaltoDeLinea;

        raf.seek(offset);

        raf.readFully(buffer);

        String valor = new String(buffer, StandardCharsets.US_ASCII).trim();
        if (valor.isEmpty()) {
            throw new IOException("Celda vacia en fila " + fila + ", columna " + columna);
        }

        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            throw new IOException(
                    "Valor numerico invalido en fila " + fila + ", columna " + columna,
                    e);
        }
    }

    public void leerColumna(int columna, ProcesadorCelda procesador) throws IOException {
        if (columna < 0 || columna >= columnas) {
            throw new IndexOutOfBoundsException("Columna fuera de rango: " + columna);
        }
        if (procesador == null) {
            throw new IllegalArgumentException("El procesador no puede ser null");
        }

        for (int fila = 0; fila < filas; fila++) {
            procesador.procesar(fila, leerCelda(fila, columna));
        }
    }

    private void validarPosicion(int fila, int columna) {
        if (fila < 0 || fila >= filas) {
            throw new IndexOutOfBoundsException("Fila fuera de rango: " + fila);
        }
        if (columna < 0 || columna >= columnas) {
            throw new IndexOutOfBoundsException("Columna fuera de rango: " + columna);
        }
    }

    public int getFilas() {
        return filas;
    }

    public int getColumnas() {
        return columnas;
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    @FunctionalInterface
    public interface ProcesadorCelda {
        void procesar(int fila, double valor) throws IOException;
    }
}
