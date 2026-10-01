# Trabajo de Aplicación 1: Coeficiente de Correlación Out-of-Core

**Universidad Nacional Mayor de San Marcos**  
**Facultad de Ingeniería de Sistemas e Informática**  
**Curso:** Programación Concurrente y Paralela  

---

## 1. Descripción del Proyecto

Este proyecto implementa el cálculo del **Coeficiente de Correlación de Pearson** entre todas las combinaciones posibles de columnas ($T = \frac{M(M-1)}{2}$ pares únicos) a partir de conjuntos de datos multidimensionales masivos almacenados en almacenamiento secundario.

El sistema fue desarrollado bajo una estricta **arquitectura Out-of-Core** y de bajo nivel con la biblioteca estándar de Java:

1. **Cero almacenamiento del dataset en RAM:** El dataset completo nunca se carga en memoria. Se leen únicamente las celdas necesarias mediante punteros deterministas de bytes (`RandomAccessFile.seek`). La memoria RAM sólo aloja acumuladores primitivos escalares y buffers fijos de lectura.
2. **Acceso directo por offset matemático:** Para un dataset con $M$ columnas, ancho fijo de celda $W$ y $B$ bytes de salto de línea, el desplazamiento se calcula sin recorrer registros previos:
   $$\text{offset}(\text{fila}, \text{columna}) = \text{fila} \times (M \times W + B) + \text{columna} \times W$$
3. **Aislamiento de descriptores de archivo:** Cada hilo de ejecución concurrente instancia su propio `RandomAccessFile` y manejador independiente (`RAFManager`), garantizando aislamiento total y evitando condiciones de carrera en el cursor del archivo.
4. **Persistencia out-of-core de resultados:** Los coeficientes calculados para todos los pares se escriben directamente en un archivo binario de ancho fijo (8 bytes por par correspondiente a un `double` IEEE-754).
5. **Certificación de equivalencia bit a bit:** Comparación exhaustiva de los $T$ coeficientes generados por la versión serial y la versión paralela, verificando coincidencia exacta en sus 64 bits de representación binaria.
6. **Benchmark riguroso y reproducible:** Medición de tiempos de ejecución con `System.nanoTime()`, ronda previa de calentamiento (warm-up) de la JVM, cálculo de medianas sobre múltiples repeticiones, y evaluación de Speedup ($S = T_s / T_p$) y Eficiencia ($E = S / H$) para 1, 2, 4 y 8 hilos.

---

## 2. Estructura del Código Fuente

| Archivo | Responsabilidad / Rol |
| :--- | :--- |
| `DatasetGenerator.java` | Generador y validador de datasets sintéticos con formato físico de ancho fijo ($W$) y columnas con correlación lineal conocida. |
| `RAFManager.java` | Abstracción de acceso aleatorio directo por cálculo matemático determinista de offsets sobre el archivo en disco. |
| `SerialEngine.java` | Motor de procesamiento serial: cálculo de correlación de Pearson sobre los $T$ pares combinatorios sin repetición. |
| `ParallelEngine.java` | Motor concurrente: partición equitativa del espacio lineal de tareas en rangos disjuntos $[ini, fin)$ entre $H$ hilos explícitos. |
| `ResultFileManager.java` | Gestor de persistencia binaria out-of-core para coeficientes (8 bytes por par) y validador de equivalencia exacta bit a bit. |
| `ConsoleUI.java` | Interfaz de consola: renderizado de menús, barras de progreso en vivo, tablas ASCII formateadas y diagnósticos del sistema. |
| `Main.java` | Orquestador principal: flujo guiado interactivo, verificación formal de equivalencia, ejecución de benchmarks y exportación CSV. |
| `DatasetGeneratorTest.java` | Pruebas unitarias de integridad física del dataset, validación de anchos fijos y detección de archivos truncados. |
| `RAFManagerTest.java` | Pruebas de acceso directo, lectura en extremos y límites de desplazamiento. |
| `ParallelEngineTest.java` | Pruebas de concurrencia: consistencia multihilo, balance de carga y equivalencia contra línea base. |
| `VerificarSerial.java` | Utilidad de verificación rápida e independiente para la versión serial. |

---

## 3. Instrucciones de Compilación y Ejecución

### Requisitos
- **Java SE Development Kit (JDK) 8** o superior instalado y configurado en el `PATH`.

### Compilación
Abra una terminal en la raíz del proyecto y compile todos los archivos fuente:
```bash
javac *.java
```

### Modos de Ejecución

#### A. Modo Interactivo (Asistente en Consola)
Ejecute el programa sin argumentos para iniciar el asistente interactivo:
```bash
java Main
```
El asistente solicitará los parámetros del experimento (puede presionar `[ENTER]` en cada prompt para emplear los valores recomendados por defecto):
- Nombre del archivo de dataset (se genera automáticamente si no existe).
- Número de observaciones ($N$).
- Número de atributos / variables ($M$).
- Número de hilos de procesamiento ($H$).

#### B. Modo Parametrizado (Línea de Comandos)
Puede especificar los parámetros directamente al invocar la aplicación:
```bash
java Main <nombre_archivo> <N_filas> <M_columnas> <H_hilos>
```

Ejemplo de ejecución:
```bash
java Main dataset.txt 2000 15 4
```
- $N = 2000$ filas / observaciones.
- $M = 15$ atributos ($T = 105$ pares únicos).
- $H = 4$ hilos de procesamiento concurrente.

---

## 4. Pruebas Automatizadas

El proyecto incluye suites de pruebas unitarias y de integración que validan el cumplimiento de las restricciones físicas y la corrección numérica:

```bash
# Prueba del generador de datasets y formato físico
java DatasetGeneratorTest

# Prueba del gestor de acceso aleatorio (RAF)
java RAFManagerTest

# Prueba del motor paralelo y consistencia multihilo
java ParallelEngineTest

# Verificación independiente del motor serial
java VerificarSerial
```

---

## 5. Salida del Programa

Durante su ejecución, el sistema muestra:
1. **Configuración y Diagnóstico del Entorno:** Parámetros de ejecución, núcleos lógicos de la CPU y validación física del archivo en disco.
2. **Generación / Validación del Dataset:** Verificación de tamaño exacto ($N \times (M \times W + B)$ bytes).
3. **Fase Demostrativa:** Ejecución serial y paralela con barras de progreso en tiempo real y persistencia binaria out-of-core.
4. **Certificación de Equivalencia:** Comprobación bit a bit de todos los $T$ coeficientes y metadatos de extremos (mínimo y máximo).
5. **Benchmark Formal:** Calentamiento previo de la JVM, mediciones repetidas sin sobrecarga de consola y resumen de tiempos representativos (mediana).
6. **Tabla de Escalabilidad:** Evaluación comparativa con 1, 2, 4 y 8 hilos reportando tiempo medio, aceleración (*Speedup*) y eficiencia (*Efficiency*), exportada adicionalmente a `benchmark_resultados.csv`.
