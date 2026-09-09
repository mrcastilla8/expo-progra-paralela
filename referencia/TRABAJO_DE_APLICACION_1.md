═════════════════════
Programación Serial y Paralela
MultiThreading
Aplicación: Aprendizaje No Supervisado
═════════════════════
DESCRIPCIÓN DEL PROBLEMA
Uno de los grandes problemas en el ámbito de Aprendizaje
Estadístico y Computacional es la búsqueda de patrones y
tendencias de comportamiento en los datos contenidos en
| datasets          |     |     | multidimensionales  |                                        |     | de  | observaciones  |
| ----------------- | --- | --- | ------------------- | -------------------------------------- | --- | --- | -------------- |
| correspondientes  |     |     |                     | a  un determinado fenómeno o problema  |     |     |                |
del mundo real.
Sea el Dataset
|     | 𝑁    |     |           |                     |     |          |     |
| --- | ---- | --- | --------- | ------------------- | --- | -------- | --- |
| 𝐷 = | {𝑋 } | =   | { 𝑋  ,  𝑋 |  , 𝑋  , .....  ,  𝑋 |     |  ,  𝑋  } |     |
|     | 𝑖    |     | 1         | 2 3                 | 𝑁−1 | 𝑁        |     |
𝑖=1
donde
|     |     |     |           |                  |     |   es    | un  vector  |
| --- | --- | --- | --------- | ---------------- | --- | ------- | ----------- |
| ●   | 𝑋 = |  (𝑥 |  , 𝑥  , 𝑥 |  , . . . . . , 𝑥 |     |  , 𝑥  ) |             |
|     | 𝑖   | 𝑖   | 𝑖         | 𝑖                | 𝑖   | 𝑖       |             |
|     |     |     | 1 2       | 3                | 𝑁−1 | 𝑁       |             |
n-dimensional de atributos o características
A  medida  que  n  aumenta  el  problema  se  hace  más  complejo
involucrando un elevado costo computacional. La Programación
Paralela surge como alternativa para optimizar los recursos de
procesamiento.
Algunas de las tareas más relevantes son:
●  Asociaciones entre atributos
●  Reducción de Dimensionalidad
Detección de Patrones Lineales
●
Similaridades entre instancias
●
●  etc…

PROPUESTA DE SOLUCIÓN
- Implementar una solución serial y paralela para el problema
usando Java.
- No utilizar bibliotecas.
- No utilizar tipos de datos complejos como ArrayList, etc.
- No utilizar herramientas IA
- Procesar en disco; no en memoria
Por ejemplo: Usando memoria al incrementar N se
desencadena el error
- Escalar la Solución para N y n.
Por ejemplo n=2, n=3, ……. n=1000 y
N=1000, N=100000, N=100000000
MODALIDAD: GRUPAL
FECHA DE PRESENTACIÓN
17/09/2026
MODALIDAD: PRESENCIAL

Procesamiento serial y paralelo en memoria (no en
disco) usando 2 hilos para determinar distancias
mínima y máxima de N=1000 puntos (datos)
3-dimensionales