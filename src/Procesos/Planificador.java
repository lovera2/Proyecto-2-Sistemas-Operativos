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
 * Además, lleva estadísticas simples sobre:
 *  - cuántas solicitudes llegaron,
 *  - cuántas se atendieron,
 *  - movimiento total del cabezal,
 *  - cuántas solicitudes CREAR / ELIMINAR / LEER.
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

    // ==========================
    // Campos de ESTADÍSTICAS
    // ==========================
    private int totalSolicitudesRecibidas;
    private int totalSolicitudesAtendidas;

    private int totalCrear;
    private int totalEliminar;
    private int totalLeer;

    public Planificador() {
        colaES = new Cola<ProcesoES>();
        politicaActual = "FIFO";    // por defecto
        posicionActualCabezal = 0;  // cabezal inicia en pista 0
        movimientoTotal = 0;
        siguienteId = 1;
        direccionAscendenteSCAN = true; // SCAN empieza subiendo

        totalSolicitudesRecibidas = 0;
        totalSolicitudesAtendidas = 0;
        totalCrear = 0;
        totalEliminar = 0;
        totalLeer = 0;
    }

    // ==========================
    // Política de planificación
    // ==========================

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

    // ==========================
    // Cabezal y movimiento
    // ==========================

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

    // ==========================
    // Manejo de solicitudes
    // ==========================

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

        // Actualizar estadísticas
        totalSolicitudesRecibidas = totalSolicitudesRecibidas + 1;
        if (tipoOperacion.equals("CREAR")) {
            totalCrear = totalCrear + 1;
        } else if (tipoOperacion.equals("ELIMINAR")) {
            totalEliminar = totalEliminar + 1;
        } else if (tipoOperacion.equals("LEER")) {
            totalLeer = totalLeer + 1;
        }
    }

    /**
     * Agrega una solicitud de tipo CREAR con tamaño en bloques.
     */
    public void agregarSolicitudCrear(String rutaArchivo, int posicionCabezal, int tamanoBloques) {
        ProcesoES p = new ProcesoES(siguienteId, "CREAR", rutaArchivo, posicionCabezal);
        siguienteId = siguienteId + 1;

        p.setEstado("listo");
        p.setTamanoEnBloques(tamanoBloques);

        colaES.encolar(p);

        // Actualizar estadísticas
        totalSolicitudesRecibidas = totalSolicitudesRecibidas + 1;
        totalCrear = totalCrear + 1;
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

    // ==========================
    // Obtener siguiente proceso
    // ==========================

    /**
     * Obtiene el siguiente proceso según la política actual.
     *
     * - FIFO: primero en entrar, primero en salir.
     * - SSTF: proceso con pista más cercana al cabezal actual.
     * - SCAN: "ascensor".
     * - CSCAN: "circular".
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
            // Por si acaso la política es inválida, usamos FIFO
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

            // Actualizar estadísticas de atendidos
            totalSolicitudesAtendidas = totalSolicitudesAtendidas + 1;
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

        return colaES.removeAt(mejorIndice);
    }

    /**
     * SCAN ("ascensor").
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
     * C-SCAN (Circular SCAN).
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

    // ==========================
    // Getters de ESTADÍSTICAS
    // ==========================

    public int getTotalSolicitudesRecibidas() {
        return totalSolicitudesRecibidas;
    }

    public int getTotalSolicitudesAtendidas() {
        return totalSolicitudesAtendidas;
    }

    public int getTotalCrear() {
        return totalCrear;
    }

    public int getTotalEliminar() {
        return totalEliminar;
    }

    public int getTotalLeer() {
        return totalLeer;
    }

    /**
     * Devuelve el promedio de movimiento del cabezal por solicitud atendida.
     * Si todavía no se ha atendido ninguna, devuelve 0.
     */
    public double getPromedioMovimientoPorSolicitud() {
        if (totalSolicitudesAtendidas == 0) {
            return 0.0;
        }
        return (double) movimientoTotal / (double) totalSolicitudesAtendidas;
    }

    /**
     * Reinicia las estadísticas (por si el usuario quiere "limpiar" los datos).
     * NO borra la cola, solo los contadores.
     */
    public void reiniciarEstadisticas() {
        totalSolicitudesRecibidas = 0;
        totalSolicitudesAtendidas = 0;
        totalCrear = 0;
        totalEliminar = 0;
        totalLeer = 0;
        movimientoTotal = 0;
    }
}