/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Procesos;

import Estructuras.Cola;

/**
 * Planificador de disco general.
 *
 * NO guarda una cola interna; trabaja sobre la cola de procesos LISTOS
 * que le pasa el GestorProcesosES.
 *
 * Decide el orden en que se atienden las solicitudes según la política
 * seleccionada (FIFO, SSTF, SCAN, CSCAN) y lleva estadísticas simples
 * de movimiento del cabezal.
 */
public class Planificador {

    private String politicaActual;      // "FIFO", "SSTF", "SCAN", "CSCAN"

    // Para simular el movimiento del cabezal
    private int posicionActualCabezal;
    private int movimientoTotal;        // movimiento acumulado del cabezal

    // Para SCAN necesitamos saber si el cabezal va hacia arriba o hacia abajo
    private boolean direccionAscendenteSCAN;

    // Estadísticas sencillas
    private int totalSolicitudesAtendidas;

    public Planificador() {
        politicaActual = "FIFO";    // por defecto
        posicionActualCabezal = 0;  // cabezal inicia en pista 0
        movimientoTotal = 0;
        direccionAscendenteSCAN = true; // SCAN empieza subiendo
        totalSolicitudesAtendidas = 0;
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
    // Selección del siguiente proceso
    // ==========================

    /**
     * Obtiene el siguiente proceso según la política actual, trabajando
     * sobre la cola de LISTOS que le pasa el GestorProcesosES.
     *
     * IMPORTANTE:
     *  - Este método SACA el proceso de la cola (usa desencolar/removeAt).
     *  - Cambia el estado del proceso a "ejecutando".
     *  - Actualiza el movimiento del cabezal y las estadísticas.
     *
     * @param colaListos cola de procesos en estado LISTO
     * @return ProcesoES seleccionado o null si no hay listos
     */
    public ProcesoES obtenerSiguienteProceso(Cola<ProcesoES> colaListos) {
        if (colaListos == null || colaListos.esVacia()) {
            return null;
        }

        ProcesoES p = null;

        if (politicaActual.equals("FIFO")) {
            p = siguienteFIFO(colaListos);
        } else if (politicaActual.equals("SSTF")) {
            p = siguienteSSTF(colaListos);
        } else if (politicaActual.equals("SCAN")) {
            p = siguienteSCAN(colaListos);
        } else if (politicaActual.equals("CSCAN")) {
            p = siguienteCSCAN(colaListos);
        } else {
            // Por si acaso la política es inválida, usamos FIFO
            p = siguienteFIFO(colaListos);
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

    //Politicas de planificacion

    /**
     * FIFO: toma el primer proceso de la cola.
     */
    private ProcesoES siguienteFIFO(Cola<ProcesoES> cola) {
        return cola.desencolar();
    }

    /**
     * SSTF (Shortest Seek Time First):
     * Busca el proceso cuya posición de pista esté más cerca
     * de la posición actual del cabezal.
     */
    private ProcesoES siguienteSSTF(Cola<ProcesoES> cola) {
        int n = cola.verTamano();
        if (n == 0) {
            return null;
        }

        int mejorIndice = 0;
        ProcesoES mejorProceso = cola.getAt(0);
        int mejorDistancia = Math.abs(mejorProceso.getPosicionCabezal() - posicionActualCabezal);

        for (int i = 1; i < n; i++) {
            ProcesoES candidato = cola.getAt(i);
            int distancia = Math.abs(candidato.getPosicionCabezal() - posicionActualCabezal);
            if (distancia < mejorDistancia) {
                mejorDistancia = distancia;
                mejorIndice = i;
                mejorProceso = candidato;
            }
        }

        return cola.removeAt(mejorIndice);
    }

    /**
     * SCAN ("ascensor").
     */
    private ProcesoES siguienteSCAN(Cola<ProcesoES> cola) {
        int n = cola.verTamano();
        if (n == 0) {
            return null;
        }

        int mejorIndice = -1;
        ProcesoES mejorProceso = null;

        if (direccionAscendenteSCAN) {
            // Buscar procesos hacia arriba (pos >= cabezalActual)
            for (int i = 0; i < n; i++) {
                ProcesoES p = cola.getAt(i);
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
                    ProcesoES p = cola.getAt(i);
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
                ProcesoES p = cola.getAt(i);
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
                    ProcesoES p = cola.getAt(i);
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
            return cola.desencolar();
        }

        return cola.removeAt(mejorIndice);
    }

    /**
     * C-SCAN (Circular SCAN).
     */
    private ProcesoES siguienteCSCAN(Cola<ProcesoES> cola) {
        int n = cola.verTamano();
        if (n == 0) {
            return null;
        }

        int mejorIndice = -1;
        ProcesoES mejorProceso = null;

        // Primero buscar posiciones >= cabezalActual
        for (int i = 0; i < n; i++) {
            ProcesoES p = cola.getAt(i);
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
                ProcesoES p = cola.getAt(i);
                int pos = p.getPosicionCabezal();
                if (mejorIndice == -1 || pos < mejorProceso.getPosicionCabezal()) {
                    mejorIndice = i;
                    mejorProceso = p;
                }
            }
        }

        return cola.removeAt(mejorIndice);
    }

    // Getters para estadisticas
  

    public int getTotalSolicitudesAtendidas() {
        return totalSolicitudesAtendidas;
    }

    // Devuelve el promedio de movimiento del cabezal por solicitud atendida. Si todavía no se ha atendido ninguna, devuelve 0.
    public double getPromedioMovimientoPorSolicitud() {
        if (totalSolicitudesAtendidas == 0) {
            return 0.0;
        }
        return (double) movimientoTotal / (double) totalSolicitudesAtendidas;
    }

    //Reinicia las estadísticas (por si el usuario quiere "limpiar" los datos).
    public void reiniciarEstadisticas() {
        totalSolicitudesAtendidas = 0;
        movimientoTotal = 0;
    }
}