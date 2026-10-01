import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class RAFManager implements AutoCloseable {

    private final RandomAccessFile raf;
    private final int filas;
    private final int columnas;
    private final int anchoFijo;
    private final int bytesSaltoDeLinea;
    private final long bytesPorFila;
    private final byte[] buffer;

    public RAFManager(File archivo, int filas, int columnas, int anchoFijo,
                      int bytesSaltoDeLinea) throws IOException {
        if (filas < 2 || columnas < 2 || anchoFijo <= 0 || bytesSaltoDeLinea < 0) {
            throw new IllegalArgumentException("Dimensiones o anchos invalidos");
        }
        if (archivo == null || !archivo.isFile()) {
            throw new IOException("Dataset inexistente o invalido");
        }

        this.filas = filas;
        this.columnas = columnas;
        this.anchoFijo = anchoFijo;
        this.bytesSaltoDeLinea = bytesSaltoDeLinea;
        this.bytesPorFila = ((long) columnas * anchoFijo) + bytesSaltoDeLinea;
        this.buffer = new byte[anchoFijo];

        long esperado = (long) filas * bytesPorFila;
        if (archivo.length() != esperado) {
            throw new IOException("Tamano del dataset incompatible. Esperado="
                    + esperado + " bytes, real=" + archivo.length() + " bytes");
        }

        this.raf = new RandomAccessFile(archivo, "r");
    }

    public double leerCelda(int fila, int columna) throws IOException {
        validarPosicion(fila, columna);

        long offset = (long) fila * bytesPorFila + (long) columna * anchoFijo;
        raf.seek(offset);
        raf.readFully(buffer);
        return parseAsciiDouble(buffer, fila, columna);
    }

    private static double parseAsciiDouble(byte[] datos, int fila, int columna) throws IOException {
        int inicio = 0;
        int fin = datos.length;

        while (inicio < fin && datos[inicio] == ' ') {
            inicio++;
        }
        while (fin > inicio && datos[fin - 1] == ' ') {
            fin--;
        }
        if (inicio == fin) {
            throw new IOException("Celda vacia en fila " + fila + ", columna " + columna);
        }

        boolean negativo = false;
        if (datos[inicio] == '-' || datos[inicio] == '+') {
            negativo = datos[inicio] == '-';
            inicio++;
            if (inicio == fin) {
                throw new IOException("Valor numerico invalido en fila " + fila + ", columna " + columna);
            }
        }

        double valor = 0.0;
        boolean hayDigitos = false;
        while (inicio < fin && datos[inicio] >= '0' && datos[inicio] <= '9') {
            hayDigitos = true;
            valor = valor * 10.0 + (datos[inicio] - '0');
            inicio++;
        }

        if (inicio < fin && datos[inicio] == '.') {
            inicio++;
            double factor = 0.1;
            while (inicio < fin && datos[inicio] >= '0' && datos[inicio] <= '9') {
                hayDigitos = true;
                valor += (datos[inicio] - '0') * factor;
                factor *= 0.1;
                inicio++;
            }
        }

        if (!hayDigitos) {
            throw new IOException("Valor numerico invalido en fila " + fila + ", columna " + columna);
        }

        int exponente = 0;
        boolean exponenteNegativo = false;
        if (inicio < fin && (datos[inicio] == 'e' || datos[inicio] == 'E')) {
            inicio++;
            if (inicio < fin && (datos[inicio] == '-' || datos[inicio] == '+')) {
                exponenteNegativo = datos[inicio] == '-';
                inicio++;
            }
            if (inicio == fin || datos[inicio] < '0' || datos[inicio] > '9') {
                throw new IOException("Exponente invalido en fila " + fila + ", columna " + columna);
            }
            while (inicio < fin && datos[inicio] >= '0' && datos[inicio] <= '9') {
                exponente = exponente * 10 + (datos[inicio] - '0');
                inicio++;
            }
        }

        if (inicio != fin) {
            throw new IOException("Caracter invalido en fila " + fila + ", columna " + columna);
        }

        if (exponente != 0) {
            double potencia = Math.pow(10.0, exponenteNegativo ? -exponente : exponente);
            valor *= potencia;
        }
        return negativo ? -valor : valor;
    }

    private void validarPosicion(int fila, int columna) {
        if (fila < 0 || fila >= filas) {
            throw new IndexOutOfBoundsException("Fila fuera de rango: " + fila);
        }
        if (columna < 0 || columna >= columnas) {
            throw new IndexOutOfBoundsException("Columna fuera de rango: " + columna);
        }
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
