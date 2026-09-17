import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/*
 * Administra la lectura directa del dataset guardado en disco.
 *
 * En la parte paralela, cada hilo debe crear su propio RAFManager. De esta
 * forma, cada hilo obtiene un RandomAccessFile y un puntero independientes.
 * Esto evita que un hilo cambie la posicion de lectura de otro hilo.
 */
public final class RAFManager implements AutoCloseable {
    // Archivo de acceso aleatorio perteneciente solamente a esta instancia.
    private final RandomAccessFile raf;

    // Informacion necesaria para tratar el archivo como una matriz en disco.
    private final int filas;
    private final int columnas;
    private final int anchoFijo;
    private final int bytesSaltoDeLinea;

    // Buffer pequeno y fijo: solo guarda una celda, nunca todo el dataset.
    private final byte[] buffer;

    /*
     * Recibe el formato fisico creado por DatasetGenerator y abre el archivo
     * solamente para lectura. El numero de bytes del salto debe ser el mismo
     * usado al generar el dataset: normalmente 1 para \n o 2 para \r\n.
     */
    public RAFManager(File archivo, int filas, int columnas, int anchoFijo,
            int bytesSaltoDeLinea) throws IOException {
        // Evita trabajar con dimensiones que producirian offsets incorrectos.
        if (filas < 0 || columnas <= 0 || anchoFijo <= 0 || bytesSaltoDeLinea < 0) {
            throw new IllegalArgumentException("Dimensiones o anchos invalidos");
        }

        // "r" significa que el archivo se abre unicamente para lectura.
        this.raf = new RandomAccessFile(archivo, "r");
        this.filas = filas;
        this.columnas = columnas;
        this.anchoFijo = anchoFijo;
        this.bytesSaltoDeLinea = bytesSaltoDeLinea;
        this.buffer = new byte[anchoFijo];
    }

    /*
     * Lee una celda sin recorrer las anteriores. Los indices empiezan en cero:
     * la primera celda del archivo se encuentra en fila 0, columna 0.
     */
    public double leerCelda(int fila, int columna) throws IOException {
        validarPosicion(fila, columna);

        /*
         * Formula de acceso directo:
         * offset = (fila * columnas + columna) * anchoFijo
         *          + fila * bytesSaltoDeLinea
         *
         * Se usa long para soportar archivos grandes sin desbordar el offset.
         */
        long offset = ((long) fila * columnas + columna) * anchoFijo
                + (long) fila * bytesSaltoDeLinea;

        // seek mueve el puntero directamente al primer byte de la celda.
        raf.seek(offset);

        // Lee exactamente los bytes asignados a una celda de ancho fijo.
        raf.readFully(buffer);

        // Quita el padding de espacios y convierte el texto a numero.
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

    /*
     * Recorre una columna leyendo una celda por vez. Cada valor se entrega al
     * procesador inmediatamente, por lo que no se crea un arreglo con toda la
     * columna ni se carga el dataset completo en memoria.
     */
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

    // Comprueba los limites antes de intentar mover el puntero del archivo.
    private void validarPosicion(int fila, int columna) {
        if (fila < 0 || fila >= filas) {
            throw new IndexOutOfBoundsException("Fila fuera de rango: " + fila);
        }
        if (columna < 0 || columna >= columnas) {
            throw new IndexOutOfBoundsException("Columna fuera de rango: " + columna);
        }
    }

    // Libera el descriptor de archivo cuando el hilo termina su trabajo.
    @Override
    public void close() throws IOException {
        raf.close();
    }

    /*
     * Define que hacer con cada valor leido de una columna. Por ejemplo, el
     * calculo serial puede acumular una suma sin guardar todos los valores.
     */
    @FunctionalInterface
    public interface ProcesadorCelda {
        void procesar(int fila, double valor) throws IOException;
    }
}
