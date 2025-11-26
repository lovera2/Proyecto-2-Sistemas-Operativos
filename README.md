# Simulador de Sistema de Archivos y Planificación de Disco

Proyecto de simulación en Java que modela un **sistema de archivos simple** sobre un disco lógico y un **planificador de E/S** con varias políticas clásicas (FIFO, SSTF, SCAN y C-SCAN).  

La aplicación incluye una interfaz gráfica en Swing que permite ver:

- La estructura de directorios y archivos en un `JTree`.
- Un disco de 64 bloques representado como una grilla de botones.
- Una tabla de asignación de archivos (bloques ocupados por cada archivo).
- Las colas de procesos de E/S: **Nuevos, Listos, Ejecución, Bloqueados y Terminados**.

---

## Características principales

- Sistema de archivos jerárquico con directorios y archivos.
- Asignación **encadenada** de bloques en disco.
- Simulación de procesos de E/S con estados:
  - `nuevo`, `listo`, `ejecutando`, `bloqueado`, `terminado`.
- Políticas de planificación de disco:
  - **FIFO**
  - **SSTF**
  - **SCAN**
  - **C-SCAN**
- Modos de uso:
  - **Modo Usuario** (solo lectura).
  - **Modo Administrador** (crear, renombrar, eliminar).
- Carga/guardado de estado mediante archivos **JSON**.

---

## Requisitos

- Java 21 o superior
- NetBeans (recomendado) o cualquier IDE compatible con proyectos Swing.
- Biblioteca estándar de Java (no se usan librerías externas raras).

---

## Cómo ejecutar el proyecto

Clonar el repositorio o descargar el código.
