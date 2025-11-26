/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Procesos;

import Componentes.GestorDisco;
import Estructuras.Cola;
import java.util.concurrent.Semaphore;

/*
 * Gestor de procesos de E/S: mantiene las colas
 * Nuevos, Listos, Ejecución, Bloqueados y Terminados
 * y simula el avance del tiempo (ticks).
 */

public class GestorProcesoES {

    // Colas de estado
    private Cola<ProcesoES> colaNuevos;
    private Cola<ProcesoES> colaListos;
    private Cola<ProcesoES> colaEjecucion;
    private Cola<ProcesoES> colaBloqueados;
    private Cola<ProcesoES> colaTerminados;

    private final Semaphore mutexColas;  // exclusión mutua

    private Planificador planificador;   // FIFO / SSTF / SCAN / CSCAN
    private GestorDisco gestorDisco;     // para ejecutar la operación real en disco

    // contador simple de ids
    private int siguienteId = 1;

    public GestorProcesoES(Planificador planificador, GestorDisco gestorDisco) {
        this.colaNuevos     = new Cola<>();
        this.colaListos     = new Cola<>();
        this.colaEjecucion  = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.mutexColas     = new Semaphore(1);
        this.planificador   = planificador;
        this.gestorDisco    = gestorDisco;
    }

    public Planificador getPlanificador() {
        return planificador;
    }

    // =========================================================
    //               CREACIÓN / ADMISIÓN
    // =========================================================

    private int generarId() {
        int id = siguienteId;
        siguienteId = siguienteId + 1;
        return id;
    }

    /** Proceso CREAR: se crea en NUEVOS. */
    public void crearProcesoCrear(String ruta, int pista, int tamBloques) {
        ProcesoES p = new ProcesoES(generarId(), "CREAR", ruta, pista);
        p.setEstado("nuevo");
        p.setTamanoEnBloques(tamBloques);

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /** Proceso ELIMINAR. */
    public void crearProcesoEliminar(String ruta, int pista) {
        ProcesoES p = new ProcesoES(generarId(), "ELIMINAR", ruta, pista);
        p.setEstado("nuevo");

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /** Proceso LEER. */
    public void crearProcesoLeer(String ruta, int pista) {
        ProcesoES p = new ProcesoES(generarId(), "LEER", ruta, pista);
        p.setEstado("nuevo");

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /** Proceso RENOMBRAR ARCHIVO (Update). */
    public void crearProcesoRenombrarArchivo(String rutaDirectorio,
                                             String nombreViejo,
                                             String nombreNuevo,
                                             int pista) {

        String rutaCompleta = rutaDirectorio + "/" + nombreViejo;

        ProcesoES p = new ProcesoES(
                generarId(),
                "RENOMBRAR_ARCHIVO",
                rutaCompleta,
                pista
        );
        p.setEstado("nuevo");
        p.setNuevoNombre(nombreNuevo);
        p.setEsDirectorio(false);

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /** Proceso RENOMBRAR DIRECTORIO (Update). */
    public void crearProcesoRenombrarDirectorio(String rutaPadre,
                                                String nombreViejo,
                                                String nombreNuevo,
                                                int pista) {
        String rutaCompleta = rutaPadre + "/" + nombreViejo;

        ProcesoES p = new ProcesoES(
                generarId(),
                "RENOMBRAR_DIRECTORIO",
                rutaCompleta,
                pista
        );
        p.setEstado("nuevo");
        p.setNuevoNombre(nombreNuevo);
        p.setEsDirectorio(true);

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Admite TODOS los procesos de NUEVOS a LISTOS.
     * Llamar una vez por tick desde la GUI.
     */
    public void admitirNuevosAListos() {
        try {
            mutexColas.acquire();
            while (colaNuevos.verTamano() > 0) {
                ProcesoES p = colaNuevos.desencolar();
                p.setEstado("listo");
                colaListos.encolar(p);
            }
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    // =========================================================
    //      LISTO -> EJECUTANDO -> BLOQUEADO -> TERMINADO
    // =========================================================

    /**
     * Despacha un proceso de LISTOS a EJECUCIÓN
     * (si no hay ninguno ejecutando).
     */
    public ProcesoES despacharSiguiente() {
        try {
            mutexColas.acquire();

            // Solo un proceso de E/S en ejecución (un solo disco)
            if (colaEjecucion.verTamano() > 0) {
                return null;
            }

            ProcesoES p = planificador.obtenerSiguienteProceso(colaListos);
            if (p == null) {
                return null;
            }

            // El planificador puede ya marcarlo como "ejecutando",
            // pero por si acaso lo volvemos a fijar.
            p.setEstado("ejecutando");
            // 1 "tick" de CPU para emitir la petición de E/S
            p.setTiempoRestanteES(1);
            colaEjecucion.encolar(p);

            return p;

        } catch (InterruptedException e) {
            return null;
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Avanza un tick para los procesos en EJECUCIÓN.
     * Cuando terminan su ráfaga de CPU pasan a BLOQUEADOS
     * con un tiempo de E/S calculado.
     */
    public void tickEjecucion() {
        try {
            mutexColas.acquire();

            int n = colaEjecucion.verTamano();
            int i = 0;
            while (i < n) {
                ProcesoES p = colaEjecucion.getAt(i);
                if (p != null) {
                    int t = p.getTiempoRestanteES() - 1;
                    p.setTiempoRestanteES(t);

                    if (t <= 0) {
                        // pasa a BLOQUEADO (esperando disco)
                        colaEjecucion.removeAt(i);
                        n--;

                        p.setEstado("bloqueado");
                        p.setTiempoRestanteES(calcularTiempoES(p));
                        colaBloqueados.encolar(p);

                        continue; // no incrementamos i
                    }
                }
                i++;
            }

        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Tiempo de servicio de disco simulado.
     */
    private int calcularTiempoES(ProcesoES p) {
        String tipo = p.getTipoOperacion();

        // renombrar: costo pequeño
        if (tipo != null && tipo.startsWith("RENOMBRAR")) {
            return 2; // 2 ticks
        }

        int bloques = p.getTamanoEnBloques();
        if (bloques <= 0) {
            bloques = 1;
        }
        return bloques * 2; // cada bloque = 2 ticks
    }

    /**
     * Avanza un tick para los BLOQUEADOS.
     * Cuando tES llega a 0: ejecuta la operación real en disco
     * y pasa a TERMINADO.
     */
    public void tickBloqueados() {
        try {
            mutexColas.acquire();

            int n = colaBloqueados.verTamano();
            int i = 0;
            while (i < n) {
                ProcesoES p = colaBloqueados.getAt(i);
                if (p != null) {
                    int t = p.getTiempoRestanteES() - 1;
                    p.setTiempoRestanteES(t);

                    if (t <= 0) {
                        // Termina su E/S
                        colaBloqueados.removeAt(i);
                        n--;

                        ejecutarOperacionReal(p);

                        p.setEstado("terminado");
                        colaTerminados.encolar(p);

                        continue; // no incrementamos i
                    }
                }
                i++;
            }

        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    // =========================================================
    //              EJECUCIÓN REAL EN DISCO
    // =========================================================

    private void ejecutarOperacionReal(ProcesoES p) {
        if (gestorDisco == null || p == null) return;

        String tipo = p.getTipoOperacion();
        String rutaCompleta = p.getRutaArchivo();

        // separar ruta en directorio + nombre
        String rutaDirectorio = "/";
        String nombreArchivo = rutaCompleta;

        if (rutaCompleta != null && rutaCompleta.contains("/")) {
            int ultimaBarra = rutaCompleta.lastIndexOf("/");
            if (ultimaBarra == 0) {
                rutaDirectorio = "/";
                nombreArchivo = rutaCompleta.substring(1);
            } else {
                rutaDirectorio = rutaCompleta.substring(0, ultimaBarra);
                nombreArchivo = rutaCompleta.substring(ultimaBarra + 1);
            }
        }

        if ("CREAR".equals(tipo)) {
            int tam = p.getTamanoEnBloques();
            gestorDisco.getSistemaArchivos()
                       .crearArchivo(rutaDirectorio, nombreArchivo, tam);

        } else if ("ELIMINAR".equals(tipo)) {
            gestorDisco.getSistemaArchivos()
                       .eliminarArchivo(rutaDirectorio, nombreArchivo);

        } else if ("LEER".equals(tipo)) {
            gestorDisco.getSistemaArchivos()
                       .leerArchivo(rutaDirectorio, nombreArchivo);

        } else if ("RENOMBRAR_ARCHIVO".equals(tipo)) {
            String nuevoNombre = p.getNuevoNombre();
            if (nuevoNombre != null && !nuevoNombre.isEmpty()) {
                gestorDisco.getSistemaArchivos()
                           .renombrarArchivo(rutaDirectorio, nombreArchivo, nuevoNombre);
            }

        } else if ("RENOMBRAR_DIRECTORIO".equals(tipo)) {
            String nuevoNombre = p.getNuevoNombre();
            if (nuevoNombre != null && !nuevoNombre.isEmpty()) {
                gestorDisco.getSistemaArchivos()
                           .renombrarDirectorio(rutaDirectorio, nombreArchivo, nuevoNombre);
            }
        }
    }

    // =========================================================
    //              HELPERS PARA LAS TABLAS (JTable)
    // =========================================================

    public Object[][] obtenerTablaNuevos() {
        try {
            mutexColas.acquire();
            return construirMatrizDesdeCola(colaNuevos);
        } catch (InterruptedException e) {
            return new Object[0][0];
        } finally {
            mutexColas.release();
        }
    }

    public Object[][] obtenerTablaListos() {
        try {
            mutexColas.acquire();
            return construirMatrizDesdeCola(colaListos);
        } catch (InterruptedException e) {
            return new Object[0][0];
        } finally {
            mutexColas.release();
        }
    }

    public Object[][] obtenerTablaEjecucion() {
        try {
            mutexColas.acquire();
            return construirMatrizDesdeCola(colaEjecucion);
        } catch (InterruptedException e) {
            return new Object[0][0];
        } finally {
            mutexColas.release();
        }
    }

    public Object[][] obtenerTablaBloqueados() {
        try {
            mutexColas.acquire();
            return construirMatrizDesdeCola(colaBloqueados);
        } catch (InterruptedException e) {
            return new Object[0][0];
        } finally {
            mutexColas.release();
        }
    }

    public Object[][] obtenerTablaTerminados() {
        try {
            mutexColas.acquire();
            return construirMatrizDesdeCola(colaTerminados);
        } catch (InterruptedException e) {
            return new Object[0][0];
        } finally {
            mutexColas.release();
        }
    }

    /** Construye filas: {ID, Operación, Ruta, Estado, tES, Pista}. */
    private Object[][] construirMatrizDesdeCola(Cola<ProcesoES> cola) {
        int n = cola.verTamano();
        Object[][] data = new Object[n][6];

        int i = 0;
        while (i < n) {
            ProcesoES p = cola.getAt(i);
            if (p != null) {
                data[i][0] = p.getIdProceso();
                data[i][1] = p.getTipoOperacion();
                data[i][2] = p.getRutaArchivo();
                data[i][3] = p.getEstado();
                data[i][4] = p.getTiempoRestanteES();
                data[i][5] = p.getPista();
            }
            i++;
        }
        return data;
    }
}