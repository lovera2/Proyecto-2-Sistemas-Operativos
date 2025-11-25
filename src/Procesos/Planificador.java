/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Procesos;

import Estructuras.Cola;

/**
 * Planificador de disco general.
 * 
 * Mantiene la cola de procesos de E/S y decide el orden
 * en que se atienden las solicitudes según la política
 * seleccionada (FIFO, SSTF, SCAN, CSCAN).
 * 
 * Cada solicitud es un ProcesoES que tiene:
 *  - tipo de operación (CREAR, ELIMINAR, LEER, etc.)
 *  - ruta del archivo
 *  - posición del cabezal (pista del disco)
 */
public class Planificador {
    
    private Cola<ProcesoES> colaES;     // cola de solicitudes de E/S
    private String politicaActual;      // "FIFO", "SSTF", "SCAN", "CSCAN"
    
    // Para simular el movimiento del cabezal
    private int posicionActualCabezal;  
    private int movimientoTotal;        // movimiento acumulado del cabezal

    // Para numerar los procesos de forma sencilla
    private int siguienteId;

    // Para SCAN necesitamos saber si el cabezal va hacia arriba o hacia abajo
    private boolean direccionAscendenteSCAN;

    public Planificador() {
        colaES = new Cola<ProcesoES>();
        politicaActual = "FIFO";    // por defecto
        posicionActualCabezal = 0;  // cabezal inicia en pista 0
        movimientoTotal = 0;
        siguienteId = 1;
        direccionAscendenteSCAN = true; // SCAN empieza subiendo
    }

    /**
     * Cambia la política de planificación.
     * La interfaz debe llamar a este método cuando el usuario elija
     * FIFO, SSTF, SCAN o CSCAN.
     */
    public void setPolitica(String nuevaPolitica) {
        this.politicaActual = nuevaPolitica;
    }

    public String getPoliticaActual() {
        return politicaActual;
    }

    /**
     * Permite cambiar la posición actual del cabezal desde afuera,
     * si alguna vez lo necesitas.
     */
    public void setPosicionActualCabezal(int nuevaPosicion) {
        this.posicionActualCabezal = nuevaPosicion;
    }

    public int getPosicionActualCabezal() {
        return posicionActualCabezal;
    }

    public int getMovimientoTotal() {
        return movimientoTotal;
    }

    /**
     * Agrega una nueva solicitud de E/S a la cola.
     * Crea un ProcesoES con un id, tipo de operación, ruta y posición.
     */
    public void agregarSolicitud(String tipoOperacion, String rutaArchivo, int posicionCabezal) {
        ProcesoES p = new ProcesoES(siguienteId, tipoOperacion, rutaArchivo, posicionCabezal);
        siguienteId = siguienteId + 1;

        // Cuando entra a la cola lo marcamos como "listo"
        p.setEstado("listo");
        colaES.encolar(p);
    }

    /**
     * Indica si hay solicitudes pendientes en la cola de E/S.
     */
    public boolean haySolicitudesPendientes() {
        return !colaES.esVacia();
    }

    /**
     * Devuelve cuántos procesos hay en la cola (para mostrar en la interfaz).
     */
    public int getCantidadEnCola() {
        return colaES.verTamano();
    }

    /**
     * Devuelve el proceso en la posición i de la cola sin sacarlo.
     * Útil para mostrar la cola en la interfaz (tabla, lista, etc.).
     */
    public ProcesoES getProcesoEnPosicion(int i) {
        return colaES.getAt(i);
    }
    
    /**
    * Agrega una solicitud de tipo CREAR con tamaño en bloques.
    */
    public void agregarSolicitudCrear(String rutaArchivo, int posicionCabezal, int tamanoBloques) {
        ProcesoES p = new ProcesoES(siguienteId, "CREAR", rutaArchivo, posicionCabezal);
        siguienteId = siguienteId + 1;
        p.setEstado("listo");
        p.setTamanoEnBloques(tamanoBloques); // aquí usamos el campo nuevo
        colaES.encolar(p);
    }

    /**
     * Obtiene el siguiente proceso según la política actual.
     * 
     * - FIFO: primero en entrar, primero en salir.
     * - SSTF: proceso con pista más cercana al cabezal actual.
     * - SCAN: "ascensor", recorre en una dirección hasta que no haya más,
     *         luego cambia de dirección.
     * - CSCAN: "circular", recorre hacia arriba; si no hay más, salta al inicio.
     */
    public ProcesoES obtenerSiguienteProceso() {
        if (colaES.esVacia()) {
            return null;
        }

        ProcesoES p = null;

        if (politicaActual.equals("FIFO")) {
            p = siguienteFIFO();
        } else if (politicaActual.equals("SSTF")) {
            p = siguienteSSTF();
        } else if (politicaActual.equals("SCAN")) {
            p = siguienteSCAN();
        } else if (politicaActual.equals("CSCAN")) {
            p = siguienteCSCAN();
        } else {
            // Por si acaso la política es inválida, usamos FIFO como fallback
            p = siguienteFIFO();
        }

        if (p != null) {
            // Cambiamos estado a "ejecutando"
            p.setEstado("ejecutando");

            // Actualizamos movimiento del cabezal
            int destino = p.getPosicionCabezal();
            int movimiento = Math.abs(destino - posicionActualCabezal);
            movimientoTotal = movimientoTotal + movimiento;
            posicionActualCabezal = destino;
        }

        return p;
    }

    // =====================================================
    // Métodos privados para cada política de planificación
    // =====================================================

    /**
     * FIFO: toma el primer proceso de la cola.
     */
    private ProcesoES siguienteFIFO() {
        return colaES.desencolar();
    }

    /**
     * SSTF (Shortest Seek Time First):
     * Busca el proceso cuya posición de pista esté más cerca
     * de la posición actual del cabezal.
     */
    private ProcesoES siguienteSSTF() {
        int n = colaES.verTamano();
        if (n == 0) {
            return null;
        }

        int mejorIndice = 0;
        ProcesoES mejorProceso = colaES.getAt(0);
        int mejorDistancia = Math.abs(mejorProceso.getPosicionCabezal() - posicionActualCabezal);

        for (int i = 1; i < n; i++) {
            ProcesoES candidato = colaES.getAt(i);
            int distancia = Math.abs(candidato.getPosicionCabezal() - posicionActualCabezal);
            if (distancia < mejorDistancia) {
                mejorDistancia = distancia;
                mejorIndice = i;
                mejorProceso = candidato;
            }
        }

        // removeAt(i) saca el elemento i de la cola
        return colaES.removeAt(mejorIndice);
    }

    /**
     * SCAN ("ascensor"):
     * 
     * - Si la dirección es ascendente:
     *      busca el proceso con posición >= cabezalActual
     *      que tenga la menor posición (el más cercano hacia arriba).
     *      Si no hay, cambia dirección a descendente y busca hacia abajo.
     * 
     * - Si la dirección es descendente:
     *      busca el proceso con posición <= cabezalActual
     *      que tenga la mayor posición (el más cercano hacia abajo).
     *      Si no hay, cambia dirección a ascendente y busca hacia arriba.
     */
    private ProcesoES siguienteSCAN() {
        int n = colaES.verTamano();
        if (n == 0) {
            return null;
        }

        int mejorIndice = -1;
        ProcesoES mejorProceso = null;

        if (direccionAscendenteSCAN) {
            // Buscar procesos hacia arriba (pos >= cabezalActual)
            for (int i = 0; i < n; i++) {
                ProcesoES p = colaES.getAt(i);
                int pos = p.getPosicionCabezal();
                if (pos >= posicionActualCabezal) {
                    if (mejorIndice == -1 || pos < mejorProceso.getPosicionCabezal()) {
                        mejorIndice = i;
                        mejorProceso = p;
                    }
                }
            }

            // Si no se encontró nadie hacia arriba, cambiar de dirección y buscar hacia abajo
            if (mejorIndice == -1) {
                direccionAscendenteSCAN = false; // ahora bajamos
                for (int i = 0; i < n; i++) {
                    ProcesoES p = colaES.getAt(i);
                    int pos = p.getPosicionCabezal();
                    if (pos <= posicionActualCabezal) {
                        if (mejorIndice == -1 || pos > mejorProceso.getPosicionCabezal()) {
                            mejorIndice = i;
                            mejorProceso = p;
                        }
                    }
                }
            }
        } else {
            // Direccion descendente: buscar procesos hacia abajo (pos <= cabezalActual)
            for (int i = 0; i < n; i++) {
                ProcesoES p = colaES.getAt(i);
                int pos = p.getPosicionCabezal();
                if (pos <= posicionActualCabezal) {
                    if (mejorIndice == -1 || pos > mejorProceso.getPosicionCabezal()) {
                        mejorIndice = i;
                        mejorProceso = p;
                    }
                }
            }

            // Si no se encontró nadie hacia abajo, cambiar de dirección y buscar hacia arriba
            if (mejorIndice == -1) {
                direccionAscendenteSCAN = true; // ahora subimos
                for (int i = 0; i < n; i++) {
                    ProcesoES p = colaES.getAt(i);
                    int pos = p.getPosicionCabezal();
                    if (pos >= posicionActualCabezal) {
                        if (mejorIndice == -1 || pos < mejorProceso.getPosicionCabezal()) {
                            mejorIndice = i;
                            mejorProceso = p;
                        }
                    }
                }
            }
        }

        if (mejorIndice == -1) {
            // Por seguridad, si algo raro pasa, usamos FIFO
            return colaES.desencolar();
        }

        return colaES.removeAt(mejorIndice);
    }

    /**
     * C-SCAN (Circular SCAN):
     * 
     * - Siempre se mueve en dirección ascendente.
     * - Busca el proceso con posición >= cabezalActual y con menor posición.
     * - Si no hay, "salta" al inicio lógico y toma el que tenga la menor posición de todos.
     */
    private ProcesoES siguienteCSCAN() {
        int n = colaES.verTamano();
        if (n == 0) {
            return null;
        }

        int mejorIndice = -1;
        ProcesoES mejorProceso = null;

        // Primero buscar posiciones >= cabezalActual
        for (int i = 0; i < n; i++) {
            ProcesoES p = colaES.getAt(i);
            int pos = p.getPosicionCabezal();
            if (pos >= posicionActualCabezal) {
                if (mejorIndice == -1 || pos < mejorProceso.getPosicionCabezal()) {
                    mejorIndice = i;
                    mejorProceso = p;
                }
            }
        }

        // Si no hay procesos hacia arriba, "saltamos" y tomamos el de menor posición global
        if (mejorIndice == -1) {
            for (int i = 0; i < n; i++) {
                ProcesoES p = colaES.getAt(i);
                int pos = p.getPosicionCabezal();
                if (mejorIndice == -1 || pos < mejorProceso.getPosicionCabezal()) {
                    mejorIndice = i;
                    mejorProceso = p;
                }
            }
        }

        return colaES.removeAt(mejorIndice);
    }
}