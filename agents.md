# Directrices y Reglas de Desarrollo del Proyecto (`agents.md`)

Este documento define las restricciones tecnicas que deben conservarse en el trabajo de Programacion Concurrente y Paralela. La fuente de datos debe procesarse **out-of-core**: el dataset completo nunca se materializa en RAM.

## Restricciones obligatorias

1. **Dataset fuera de RAM**
   - No crear matrices `N x M`, listas, mapas o estructuras equivalentes que almacenen el dataset completo.
   - Para el procesamiento numerico se permiten acumuladores primitivos y buffers fijos de tamano constante.

2. **Acceso directo al dataset**
   - Cada celda se localiza mediante un offset determinista, sin recorrer registros anteriores.
   - Para un archivo con `M` columnas, ancho de celda `W` y `B` bytes de salto de linea:

     `offset(fila, columna) = fila * (M * W + B) + columna * W`

3. **Formato fisico fijo**
   - Cada celda debe ocupar exactamente `W` bytes.
   - Si un valor no cabe en `W`, el generador debe rechazarlo; nunca debe ampliar silenciosamente la celda.
   - El archivo debe validarse antes de procesarse: `tamano = N * (M * W + B)`.

4. **Sin bibliotecas externas**
   - Solo se utiliza la biblioteca estandar de Java.
   - No se emplean frameworks de calculo, concurrencia o persistencia.

5. **Concurrencia explicita**
   - La version paralela utiliza hilos creados y sincronizados explicitamente.
   - Los `T = M(M-1)/2` pares se reparten en rangos lineales disjuntos `[ini, fin)`.
   - Si se solicitan mas hilos que pares, el numero efectivo de hilos se limita a `T`.

6. **Aislamiento de descriptores**
   - Cada hilo posee su propio `RAFManager` para el dataset.
   - Si los hilos persisten resultados, cada uno abre su propio descriptor de salida y escribe exclusivamente en su rango de offsets.

7. **Resultados out-of-core**
   - Los coeficientes de todos los pares se guardan en un archivo binario de ancho fijo: un `double` IEEE-754 de 8 bytes por indice lineal de par.
   - No se permite almacenar los `T` coeficientes en una coleccion en RAM solo para compararlos.

8. **Equivalencia exacta**
   - La validacion serial/paralela debe comparar **todos** los coeficientes persistidos.
   - La expresion "identico bit a bit" solo puede emitirse si los 64 bits IEEE-754 coinciden para los `T` resultados y los metadatos de extremos tambien coinciden.

9. **Benchmark reproducible y justo**
   - Los intervalos se miden con `System.nanoTime()`.
   - La barra de progreso, impresiones y persistencia de archivos de validacion quedan fuera del benchmark usado para calcular Speedup.
   - Debe ejecutarse al menos una ronda de calentamiento de JVM antes de las mediciones.
   - Deben realizarse multiples repeticiones y resumirse con una medida estable (en la implementacion actual: mediana de 3 repeticiones).
   - Se reporta `Speedup = Ts / Tp` y `Eficiencia = Speedup / H`.

10. **Pruebas con codigo de salida**
    - Una prueba fallida debe terminar con codigo distinto de cero.
    - Deben cubrirse, como minimo: ancho fijo, archivo truncado, correlacion conocida, 1/2/4/8 hilos y `H > T`.

## Separacion de responsabilidades de ejecucion

- **Modo demostrativo:** puede mostrar barra de progreso y persiste todos los coeficientes para demostrar equivalencia.
- **Modo benchmark:** no imprime durante la medicion ni persiste resultados; mide exclusivamente el nucleo de calculo bajo condiciones equivalentes.

## Archivos compartidos clave

- `DatasetGenerator.java`: genera y valida el formato fisico.
- `RAFManager.java`: lectura directa por offset.
- `ResultFileManager.java`: persistencia fija de los coeficientes y comparacion bit a bit.
- `SerialEngine.java`: linea base serial.
- `ParallelEngine.java`: procesamiento concurrente.
- `Main.java`: orquestacion, equivalencia y benchmark.
