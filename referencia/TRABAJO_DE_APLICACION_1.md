# Trabajo de Aplicacion 1 - Programacion Serial y Paralela

> Este archivo conserva el contenido funcional de la consigna recibida y aclara la aparente contradiccion entre el ejercicio previo "en memoria" y el trabajo grupal actual "en disco".

## Descripcion del problema

Uno de los problemas del aprendizaje estadistico y computacional es la busqueda de patrones y tendencias en datasets multidimensionales. A medida que aumenta el numero de observaciones `N` y atributos `n`, el costo computacional crece y la programacion paralela surge como alternativa para aprovechar mejor los recursos de procesamiento.

Tareas relacionadas:

- Asociaciones entre atributos.
- Reduccion de dimensionalidad.
- Deteccion de patrones lineales.
- Similaridades entre instancias.

## Propuesta de solucion vigente para el trabajo grupal

- Implementar una solucion serial y paralela usando Java.
- No utilizar bibliotecas externas.
- No utilizar estructuras complejas destinadas a mantener el dataset completo en RAM, como `ArrayList` o matrices `N x M`.
- Mantener la restriccion indicada por el docente respecto del uso de herramientas IA.
- **Procesar el dataset en disco; no cargarlo completo en memoria.**
- Escalar la solucion para distintos valores de `N` y `n`.
- Utilizar acceso directo a registros/celdas de ancho fijo.
- Medir tiempos seriales y paralelos y calcular Speedup.
- Demostrar que la version paralela produce exactamente los mismos coeficientes que la serial.

## Aclaracion sobre el bloque "en memoria"

En el material original tambien aparece la indicacion:

> "Procesamiento serial y paralelo en memoria (no en disco) usando 2 hilos para determinar distancias minima y maxima de N=1000 puntos 3-dimensionales".

Ese bloque se interpreta como **ejercicio previo/de referencia de clase**, no como la arquitectura del trabajo grupal out-of-core. La solucion actual del repositorio sigue la exigencia posterior y mas especifica: **procesamiento directo en almacenamiento secundario**.

## Modalidad

- Modalidad: grupal.
- Presentacion indicada en el material: 17/09/2026.
- Modalidad de presentacion: presencial.
