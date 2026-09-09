# Directrices y Reglas de Desarrollo del Proyecto (`agents.md`)

Este documento define el marco técnico, los principios arquitectónicos y las restricciones operativas obligatorias para cualquier agente o desarrollador que trabaje en este repositorio. Su propósito es garantizar el cumplimiento riguroso de las especificaciones de computación concurrente y paralela de bajo nivel.

---

## 🚫 LO QUE NO SE DEBE HACER (Prohibiciones Estrictas)

1. **No cargar datasets ni estructuras masivas en memoria RAM:**
   - Está terminantemente prohibido instanciar matrices bidimensionales o estructuras equivalentes en memoria principal para albergar todo el conjunto de datos de entrada o salida.
   - No se deben utilizar enfoques donde los datos se lean en su totalidad hacia la RAM antes de ser procesados. La memoria RAM debe usarse únicamente para buffers temporales de tamaño constante y acumuladores escalares.

2. **No utilizar colecciones complejas ni estructuras de alto nivel:**
   - Está prohibido el uso de colecciones dinámicas de alto nivel (como listas dinámicas, mapas, conjuntos u objetos envoltorios complejos).
   - No utilizar abstracciones que oculten la gestión directa de la memoria o introduzcan sobrecarga de recolección de basura (*garbage collection*).

3. **No utilizar bibliotecas ni frameworks externos:**
   - Queda prohibida la inclusión de dependencias o librerías externas para cálculo matricial, análisis estadístico, concurrencia o manipulación de archivos.
   - Solo se permite el uso de los componentes fundamentales del lenguaje estándar.

4. **No recurrir a la lectura o escritura secuencial:**
   - Está prohibido el uso de lectores secuenciales que avancen línea por línea desde el inicio del archivo para buscar registros intermedios o distantes.
   - No implementar soluciones que obliguen a cerrar y reabrir archivos para reiniciar punteros de lectura.

5. **No compartir el mismo descriptor o cursor de archivo entre hilos concurrentes:**
   - Ningún hilo de trabajo debe compartir la misma instancia de lectura/escritura en disco con otro hilo, ya que las modificaciones concurrentes de los punteros de desplazamiento causan condiciones de carrera y corrupción de datos.

6. **No asumir soluciones válidas sin verificación de consistencia numérica:**
   - No se debe dar por finalizada una implementación paralela sin haber certificado de manera automatizada que sus resultados coinciden con exactitud bit a bit frente a la implementación serial de referencia.

---

## ✅ LO QUE SE DEBE HACER (Requisitos Obligatorios y Buenas Prácticas)

1. **Procesamiento Directo en Almacenamiento Secundario (Out-of-Core):**
   - El procesamiento debe realizarse directamente sobre el disco, tratando los archivos como medios de acceso aleatorio.
   - Toda celda, fila o columna requerida para un cálculo debe consultarse y recuperarse mediante direccionamiento absoluto de punteros de bytes.

2. **Diseño de Registros y Celdas de Longitud Fija:**
   - Los datos almacenados en disco deben estructurarse con anchos fijos e invariables de caracteres o bytes para cada columna o celda.
   - Se debe formular y aplicar una ecuación matemática determinista de desplazamiento que permita calcular la posición física de cualquier elemento $(i, j)$ sin recorrer los elementos precedentes:
     $$\text{Desplazamiento en Bytes} = f(\text{fila}, \text{columna}, \text{ancho fijo})$$

3. **Concurrencia y Paralelización Explícita:**
   - La concurrencia debe implementarse de forma explícita mediante la creación, asignación y gestión directa de hilos del sistema.
   - La carga de trabajo global debe particionarse de manera equitativa y balanceada entre la cantidad de hilos configurados, calculando rangos disjuntos para cada unidad de ejecución.

4. **Aislamiento de Recursos por Hilo de Ejecución:**
   - Cada hilo concurrente que necesite leer o escribir en disco debe instanciar y operar su propio manejador independiente de archivo, garantizando la total independencia de su puntero de lectura.

5. **Uso Exclusivo de Tipos Primitivos y Buffers de Tamaño Fijo:**
   - Para la manipulación de datos en memoria intermedia solo deben emplearse tipos de datos primitivos y arreglos de tamaño estático predimensionados según el ancho de celda o bloque.

6. **Diseño Escalable:**
   - El diseño algorítmico debe estar preparado para escalar tanto en el número de observaciones ($N$) como en el número de atributos o dimensiones ($n$), soportando volúmenes que superen con creces la capacidad de la memoria RAM disponible.

7. **Medición Rigurosa de Rendimiento:**
   - Debe implementarse un mecanismo de cronometraje de alta precisión para medir:
     - El tiempo de ejecución en modalidad serial pura.
     - El tiempo de ejecución en modalidad paralela variando la cantidad de hilos (por ejemplo: 2, 4 y 8 unidades).
     - El Factor de Aceleración (*Speedup*), definido como el cociente entre el tiempo serial y el tiempo paralelo.

8. **Comprobación de Equivalencia de Resultados:**
   - Debe implementarse una rutina de validación que compare las salidas generadas por el flujo serial y el flujo paralelo, emitiendo una confirmación formal de identidad entre ambos resultados.
