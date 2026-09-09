# Guía y Resumen Conceptual: Explicación de Pablito y Trabajo Grupal
## Curso: Programación Concurrente y Paralela / Computación Distribuida

**Fuentes Analizadas:**
- **Transcripción de la Sesión:** [`explicacion_pablito.md`](file:///C:/Users/marec/Desktop/explicacion_pablito.md) (Grabación de 2h 50m).
- **Notas de Clase Oficiales:** [`NOTAS DE CLASE PROG; CONCURRENTE PARALELA.txt`](file:///C:/Users/marec/Downloads/NOTAS%20DE%20CLASE%20PROG;%20CONCURRENTE%20PARALELA.txt).
- **Códigos Base del Docente:** [`Demo_PCP1.java`](file:///C:/Users/marec/Downloads/drive-download-20260909T032030Z-1-001/Demo_PCP1.java) y [`Demo_RAF2.java`](file:///C:/Users/marec/Downloads/drive-download-20260909T032030Z-1-001/Demo_RAF2.java).
- **Captura de Pantalla:** Imagen de Excel con columnas amarilla y celeste (factor multiplicador 10).

---

## 🧭 1. ¿Qué Quiere Decir el Profesor? (Visión y Filosofía de la Clase)

A lo largo de la sesión, el profesor Pablo ("Pablito") transmite una crítica profunda a la forma tradicional en que se enseña programación en la universidad y establece el estándar técnico que espera para los trabajos del curso:

### El Problema de la "Mentalidad Académica" (Enfoque en Memoria RAM)
- **La práctica común a erradicar:** El estudiante promedio suele resolver problemas cargando matrices completas ($N \times M$) directamente en la memoria RAM mediante arreglos bidimensionales o colecciones en memoria.
- **La realidad en proyectos de producción:** Cuando se procesan volúmenes reales de datos (millones de registros, cientos de variables, datos satelitales, sensores o transacciones bancarias), la memoria RAM se satura de inmediato. 
- **La tesis del docente:** Los grandes motores de bases de datos relacionales, sistemas de indexación y herramientas de búsqueda fueron diseñados para operar directamente sobre almacenamiento persistente (**procesamiento Out-of-Core**). Por ende, el programador concurrente debe aprender a diseñar soluciones que no dependan de la memoria principal para almacenar el conjunto de datos.

### La Exigencia de la Computación Explícita
- **Programación Implícita:** Apoyarse en librerías de alto nivel que ocultan la gestión de hilos y el almacenamiento.
- **Programación Explícita:** El programador debe diseñar y gobernar explícitamente:
  1. El mapeo de memoria física en disco (organización de bytes).
  2. La partición de la carga de trabajo entre hilos concurrentes.
  3. La sincronización y control de flujo entre procesos.

---

## 📊 2. Análisis de la Imagen de Excel Mostrada en Clase

En el transcurso de la explicación (minuto `[00:06:24]` a `[00:13:30]`), el profesor comparte su pantalla mostrando una hoja de cálculo con dos columnas y el número 10:

![Excel de Correlación](file:///C:/Users/marec/.gemini/antigravity/brain/375ccc3a-1a60-419d-a954-150a9b3bb8c9/.user_uploaded/media_1788923953575.png)

### Anatomía del Ejemplo Mostrado:
1. **Columna Amarilla ($X$):** Representa una serie de valores numéricos de una variable (por ejemplo, el sueldo o gasto de 10 personas: `785`, `1094`, `1086`, etc.).
2. **Celda Superior Izquierda (10):** Un factor escalar constante ($k = 10$).
3. **Columna Celeste ($Y$):** Cada elemento es exactamente el valor de la columna amarilla multiplicado por 10 (`7850`, `10940`, `10860`, etc.).

### Fundamentación Matemática y de Minería de Datos:
- **Interpretación Vectorial:** Al interpretar cada columna como un vector en un espacio de 10 dimensiones, el vector $Y$ apunta en la misma dirección y sentido que el vector $X$. El ángulo geométrico formado entre ambos vectores es $\theta = 0^\circ$. Por lo tanto, el coseno del ángulo es $\cos(0^\circ) = 1.0$ (máxima similitud).
- **Interpretación Estadística (Coeficiente de Correlación de Pearson):** 
  Dado que existe una relación lineal directa y exacta entre ambas columnas, la covarianza normalizada alcanza su cota superior:
  $$r = 1.0$$
  Esto demuestra una **correlación / asociación lineal perfecta**.
- **Propósito del Ejemplo en el Trabajo:**
  En conjuntos de datos reales (como datos demográficos, socioeconómicos o mediciones físicas), las variables no serán múltiplos exactos entre sí. Habrá perturbaciones, ruido y dispersión. El trabajo consiste en calcular sistemáticamente el nivel de asociación existente entre **todos los pares posibles de variables** presentes en una matriz.

---

## 🧩 3. Articulación entre las Notas de Clase y los Códigos Base

Los materiales compartidos por el docente representan una progresión pedagógica deliberada:

```
┌──────────────────────────────────────────────┐
│  NOTAS DE CLASE (Problemas QR y Matriz AxB)  │
│  • Enseña cómo particionar rangos entre hilos│
│  • Enseña a medir Speedup = Ts / Tp          │
│  • Enseña a usar padding de ancho fijo (HH)  │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│   Demo_PCP1.java (Lógica Combinatoria)       │
│   • Enseña la combinatoria de pares únicos:  │
│     T = M * (M - 1) / 2                      │
│   • Enseña la estructura: Serial vs Paralelo │
│   • Enseña a buscar extremos (mínimo/máximo) │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│   Demo_RAF2.java (Lógica de Disco Directo)   │
│   • Enseña la fórmula de desplazamiento:     │
│     Offset = (fila * M + col) * AnchoFijo    │
│   • Doble bucle j (columna 1) y k (columna 2)│
│     iterando sobre todas las filas en disco  │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│               TRABAJO GRUPAL                 │
│  Asociación de Columnas en Disco + Multihilo │
└──────────────────────────────────────────────┘
```

### 1. El Aprendizaje de las Notas de Clase:
- **Partición Equitativa de Cargas:** El profesor divide el espacio de trabajo en bloques proporcionales al número de hilos disponibles, calculando un índice de inicio y un índice de fin para cada hilo.
- **Sincronización:** Demuestra cómo pausar el hilo principal hasta que todos los hilos trabajadores completen su sección.
- **Métricas Obligatorias:** El tiempo serial ($T_s$), el tiempo paralelo ($T_p$) y el factor de aceleración (*Speedup* $S = \frac{T_s}{T_p}$).

### 2. El Aprendizaje de `Demo_PCP1.java`:
- Plantea la fórmula combinatoria para comparar elementos sin repetición ni pares invertidos:
  $$T = \frac{M(M - 1)}{2}$$
  Si hay 10 columnas, existen 45 pares únicos. Si hay 100 columnas, existen 4,950 pares únicos.
- Muestra cómo registrar y filtrar los resultados extremos (el par con mayor correlación y el par con menor correlación).

### 3. El Aprendizaje de `Demo_RAF2.java`:
- Establece la regla para tratar un archivo en disco como una cuadrícula matricial sin cargarla a la memoria:
  - Cada celda de datos se guarda con un número fijo e invariable de caracteres o bytes ($W$).
  - Para leer la intersección entre la fila $i$ y la columna $j$, se calcula directamente el puntero de bytes:
    $$\text{Posición en Disco} = (i \cdot M + j) \cdot W$$
  - El puntero del archivo salta instantáneamente a esa posición sin leer los datos previos.

---

## 📋 4. Especificación Paso a Paso del Trabajo Grupal

El grupo debe estructurar la solución siguiendo los siguientes requerimientos técnicos y metodológicos:

### Fase 1: Creación del Dataset Persistente en Disco
- El conjunto de datos debe residir en un archivo físico en disco (no en memoria RAM).
- Debe constar de $N$ registros (filas) y $M$ atributos (columnas numéricas).
- **Estructuración de ancho fijo:** Cada valor numérico en el archivo debe tener exactamente la misma longitud en bytes (por ejemplo, rellenando con espacios a la izquierda), de tal modo que la distancia en bytes entre cualquier fila y columna sea predecible matemáticamente.

### Fase 2: Definición de la Métrica de Asociación
- Para cada combinación de columnas $(j, k)$, con $j < k$, se debe calcular su grado de asociación a lo largo de las $N$ filas.
- Las métricas admitidas por la explicación del docente son:
  - **Coeficiente de Correlación de Pearson:** Mide la tendencia lineal entre ambas variables normalizada entre $-1.0$ y $+1.0$.
  - **Similitud del Coseno:** Mide el coseno del ángulo entre los vectores de ambas columnas normalizado entre $-1.0$ y $+1.0$.
- **Restricción estricta:** Los valores de las columnas $j$ y $k$ para cada fila $i$ deben ser extraídos directamente del disco mediante posicionamiento absoluto de puntero.

### Fase 3: Ejecución Serial
- Un único proceso o hilo recorre los $T = \frac{M(M-1)}{2}$ pares de columnas.
- Mide el tiempo total en milisegundos desde el inicio del primer cálculo hasta la finalización del último par.
- Almacena los coeficientes de correlación calculados para su posterior contraste.

### Fase 4: Ejecución Paralela con Hilos Concurrentes
- Se define un número configurable de hilos de procesamiento (por ejemplo: 2, 4 u 8 hilos).
- Los $T$ pares de columnas se distribuyen uniformemente entre el grupo de hilos.
- **Punto crítico de diseño concurrente:**
  - Cuando múltiples hilos leen simultáneamente de un archivo físico, si intentan compartir un mismo puntero de lectura, se producirán colisiones y lecturas erróneas.
  - La arquitectura debe garantizar que **cada hilo posea su propio descriptor o manejador independiente de apertura del archivo**, permitiéndole posicionar su puntero y leer sin interferir con los demás hilos.
- El hilo principal debe aguardar a que la totalidad de los hilos de trabajo concluyan sus respectivas particiones para detener el cronómetro.

### Fase 5: Análisis de Resultados y Benchmarking
El informe final del grupo debe incluir:
1. **Demostración de Equivalencia:** Comprobar que los coeficientes de asociación obtenidos en modo paralelo son exactamente iguales a los obtenidos en modo serial.
2. **Cuadro Comparativo de Tiempos:** Tiempos de ejecución registrados en milisegundos para la versión serial y la versión paralela con diferentes cantidades de hilos.
3. **Curva de Factor de Aceleración (*Speedup*):** Evaluación de la ganancia de velocidad ($S = \frac{T_s}{T_p}$) frente al número de procesadores/hilos utilizados.
4. **Identificación de Extremos:** Indicar qué par de columnas presentó la **máxima asociación** (mayor correlación positiva) y cuál presentó la **mínima asociación** (menor correlación o correlación inversa).

---

## ⚠️ 5. Errores Críticos que el Docente Penalizará

1. **Cargar la matriz a memoria:** Crear matrices o listas dinámicas en memoria para alojar el archivo de datos antes de calcular las correlaciones. Todo acceso a los datos debe ser directo desde el medio de almacenamiento secundario.
2. **Uso de lectura secuencial:** Utilizar lectores que avancen línea por línea desde el principio del archivo en lugar de saltar a la dirección exacta calculada por desplazamiento de bytes.
3. **Hilos sin aislamiento de puntero:** Compartir un único cursor de archivo entre varios hilos concurrentes, lo cual provoca corrupción en la lectura de celdas contiguas.
4. **Falta de verificación entre Serial y Paralelo:** Presentar tiempos de ejecución paralelos sin haber contrastado previamente que los resultados numéricos de las correlaciones son idénticos a los de la versión serial.

---

## 📅 6. Aspectos Administrativos y Próximos Pasos

- **Evaluación y Revisión:** El profesor programó una sesión presencial/práctica en horario de clase (viernes) o en laboratorio (sábado a las 10:00 AM) para validar el avance del trabajo y tomar evaluaciones de desarrollo.
- **Canal de Consulta Directa:** El profesor facilitó su número de WhatsApp (`948-521-609`) para coordinaciones con la delegación del curso.
