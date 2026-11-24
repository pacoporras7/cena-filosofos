# La cena de los filósofos

Ejercicio evaluable de PSP (Programación de Servicios y Procesos) - 2.º DAM.

## Integrantes del grupo

- Samuel Martinez
- Adrian Garde

## Descripción

Simulación en Java del problema clásico de la cena de los filósofos (5 filósofos y 5 tenedores).
La usamos como laboratorio para provocar y resolver tres cosas:

- **Recurso compartido:** el tenedor, protegido con un monitor (`synchronized`, `wait` y `notifyAll`).
- **Race condition:** en el modo `sin-control`, un tenedor lo usan dos filósofos a la vez (entre 250 y 300 conflictos en 10 s).
- **Deadlock:** en el modo `deadlock`, todos cogen su tenedor izquierdo y se bloquean. Se soluciona con `orden` o con `camarero`.

## Archivos

- `Cena.java`: código de la simulación (paquete `ultima_cena`), comentado.
- `Cena_Filosofos_Presentacion_v2.pdf`: presentación de 8 minutos.

## Cómo ejecutarlo

Ejecuta `Cena.java` sin argumentos y sale un menú en la consola:

1. sin-control (el fallo)
2. correcto (arreglado)
3. deadlock (bloqueo)
4. orden (solución 1)
5. camarero (solución 2)
0. salir

Escribe el número y pulsa Enter.
