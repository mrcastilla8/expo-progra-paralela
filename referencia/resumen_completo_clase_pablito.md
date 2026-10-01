# Guia y Resumen Conceptual: Trabajo Grupal de Programacion Concurrente y Paralela

## 1. Idea central

El objetivo es calcular asociaciones entre todos los pares de atributos de un dataset multidimensional sin cargar el conjunto completo en RAM. El dataset permanece en almacenamiento secundario y cada celda se obtiene mediante acceso aleatorio por offset.

La arquitectura combina:

- Formato de ancho fijo en disco.
- Acceso directo con `RandomAccessFile`.
- Implementacion serial de referencia.
- Implementacion paralela con hilos explicitos.
- Persistencia out-of-core de todos los coeficientes.
- Comparacion bit a bit entre serial y paralelo.
- Benchmark separado de la interfaz de consola.

## 2. Ejemplo de correlacion conocido

El ejemplo de clase utiliza dos columnas donde `Y = 10 * X`. Al existir una relacion lineal positiva perfecta, el coeficiente de Pearson esperado es aproximadamente `+1.0`. Este caso permite comprobar que el calculo serial funciona antes de comparar la version paralela.

## 3. Formato fisico y acceso directo

Cada valor ocupa exactamente `W` bytes. Cada fila contiene `M * W` bytes de datos mas `B` bytes de salto de linea.

La posicion de una celda es:

```text
offset(fila, columna) = fila * (M * W + B) + columna * W
```

Con CRLF, `B = 2`.

Esta formula incluye los saltos de linea; omitirlos desplaza incorrectamente todas las filas posteriores a la primera.

## 4. Pares de atributos

Solo se procesan pares unicos `j < k`:

```text
T = M * (M - 1) / 2
```

El indice lineal de cada par permite dividir el trabajo paralelo en rangos continuos y balanceados:

```text
hilo t:
ini = t * T / H
fin = (t + 1) * T / H
```

Cada hilo trabaja exclusivamente en `[ini, fin)`.

## 5. Ejecucion serial

La version serial:

1. Abre un `RAFManager` propio.
2. Recorre los `T` pares.
3. Para cada par recorre las `N` filas mediante `seek(offset)`.
4. Calcula Pearson con acumuladores escalares.
5. Registra maximo y minimo.
6. Cuando se ejecuta en modo de validacion, escribe cada coeficiente en un archivo de resultados de 8 bytes por par.

Los `T` coeficientes **no se almacenan en un arreglo en RAM**.

## 6. Ejecucion paralela

La version paralela:

1. Limita el numero efectivo de hilos a `min(H, T)`.
2. Asigna a cada hilo un rango lineal disjunto.
3. Cada hilo abre su propio `RAFManager`.
4. Cada hilo calcula los pares de su rango.
5. En modo de validacion, cada hilo abre su propio descriptor del archivo de resultados y escribe unicamente en offsets correspondientes a su rango.
6. El hilo principal utiliza `join()` para esperar la finalizacion de todos los trabajadores.
7. Los extremos locales se reducen a maximo y minimo global.

## 7. Equivalencia correcta

Comparar solo el maximo y el minimo no demuestra equivalencia del algoritmo completo. La validacion correcta compara los `T` coeficientes seriales y paralelos.

Cada resultado se guarda como los 64 bits IEEE-754 de un `double`. Los archivos se comparan registro por registro mediante offsets fijos. Solo si todos los registros coinciden se puede afirmar:

> Los resultados seriales y paralelos son identicos bit a bit.

Una tolerancia numerica (`1e-6`, por ejemplo) puede ser valida para contrastar un resultado con un valor teorico, pero **no equivale a una comparacion bit a bit**.

## 8. Benchmark y Speedup

El benchmark debe estar separado de la demostracion visual. La barra de progreso, `println`, `flush` y los archivos usados para validar equivalencia no deben contaminar la medicion del Speedup.

La implementacion corregida utiliza:

- `System.nanoTime()` para medir intervalos.
- 1 ronda de warm-up de JVM.
- 3 repeticiones medidas por configuracion.
- Mediana como tiempo representativo.
- Configuraciones serial, 2, 4 y 8 hilos.

Formulas:

```text
Speedup S = Ts / Tp
Eficiencia E = S / H
```

`Main.java` genera tambien `benchmark_resultados.csv`, que contiene los datos necesarios para construir la curva de Speedup sin mezclarla con la ejecucion demostrativa.

## 9. Pruebas minimas

La suite corregida valida:

- Tamano fisico exacto del dataset.
- Rechazo de valores que exceden `W`.
- Acceso directo a primera y ultima fila.
- Rechazo de archivo truncado.
- Correlacion conocida entre columnas 0 y 1.
- Persistencia de un resultado de 8 bytes por par.
- Equivalencia bit a bit con 1, 2, 4 y 8 hilos.
- Limitacion automatica cuando `H > T`.
- Codigo de salida distinto de cero si una prueba falla.

## 10. Material de referencia del repositorio

Los archivos de `referencia/` deben entenderse como material de apoyo y ejemplos del docente. La fuente de verdad de la implementacion actual son `agents.md` y las clases Java del directorio raiz.
