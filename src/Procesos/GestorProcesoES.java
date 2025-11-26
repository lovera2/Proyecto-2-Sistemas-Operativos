/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/*
 * Gestor de procesos de E/S: mantiene las colas
 * Nuevos, Listos, Ejecución, Bloqueados y Terminados
 * y simula el avance del tiempo (ticks).
 */
package Procesos;

import Componentes.GestorDisco;
import Estructuras.Cola;
import java.util.concurrent.Semaphore;

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

    // Flag para avisar a la GUI que cambió algo en el sistema de archivos
    private boolean huboCambioSistemaArchivos = false;

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

    public boolean isHuboCambioSistemaArchivos() {
        return huboCambioSistemaArchivos;
    }

    public void limpiarCambioSistemaArchivos() {
        this.huboCambioSistemaArchivos = false;
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
        int pistaLogica = calcularPistaDesdeRuta(ruta);   // se ignora "pista" y se calcula por ruta
        ProcesoES p = new ProcesoES(generarId(), "CREAR", ruta, pistaLogica);
        p.setEstado("nuevo");
        p.setTamanoEnBloques(tamBloques);

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
        } catch (InterruptedException e) {
            // opcional: loggear
        } finally {
            mutexColas.release();
        }
    }

    /** Proceso ELIMINAR. */
    public void crearProcesoEliminar(String ruta, int pista) {
        int pistaLogica = calcularPistaDesdeRuta(ruta);
        ProcesoES p = new ProcesoES(generarId(), "ELIMINAR", ruta, pistaLogica);
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
        int pistaLogica = calcularPistaDesdeRuta(ruta);
        ProcesoES p = new ProcesoES(generarId(), "LEER", ruta, pistaLogica);
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
        int pistaLogica = calcularPistaDesdeRuta(rutaCompleta);

        ProcesoES p = new ProcesoES(
                generarId(),
                "RENOMBRAR_ARCHIVO",
                rutaCompleta,
                pistaLogica
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
        int pistaLogica = calcularPistaDesdeRuta(rutaCompleta);

        ProcesoES p = new ProcesoES(
                generarId(),
                "RENOMBRAR_DIRECTORIO",
                rutaCompleta,
                pistaLogica
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
                if (p != null) {
                    p.setEstado("listo");
                    colaListos.encolar(p);
                }
            }
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    // =========================================================
    //      LISTO -> EJECUTANDO -> BLOQUEADO -> TERMINADO
    // =========================================================

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

            p.setEstado("ejecutando");
            p.setTiempoRestanteES(1);
            colaEjecucion.encolar(p);

            return p;

        } catch (InterruptedException e) {
            return null;
        } finally {
            mutexColas.release();
        }
    }

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

                        continue;
                    }
                }
                i++;
            }

        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

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
        return bloques * 2;
    }

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

                        continue;
                    }
                }
                i++;
            }

        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }
    
    private void ejecutarOperacionReal(ProcesoES p) {
        if (gestorDisco == null || p == null) return;

        String tipo = p.getTipoOperacion();
        String rutaCompleta = p.getRutaArchivo();

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
            huboCambioSistemaArchivos = true;

        } else if ("ELIMINAR".equals(tipo)) {
            gestorDisco.getSistemaArchivos()
                       .eliminarArchivo(rutaDirectorio, nombreArchivo);
            huboCambioSistemaArchivos = true;

        } else if ("LEER".equals(tipo)) {
            gestorDisco.getSistemaArchivos()
                       .leerArchivo(rutaDirectorio, nombreArchivo);
            // leer NO modifica el árbol → no marcamos cambio

        } else if ("RENOMBRAR_ARCHIVO".equals(tipo)) {
            String nuevoNombre = p.getNuevoNombre();
            if (nuevoNombre != null && !nuevoNombre.isEmpty()) {
                gestorDisco.getSistemaArchivos()
                           .renombrarArchivo(rutaDirectorio, nombreArchivo, nuevoNombre);
                huboCambioSistemaArchivos = true;
            }

        } else if ("RENOMBRAR_DIRECTORIO".equals(tipo)) {
            String nuevoNombre = p.getNuevoNombre();
            if (nuevoNombre != null && !nuevoNombre.isEmpty()) {
                gestorDisco.getSistemaArchivos()
                           .renombrarDirectorio(rutaDirectorio, nombreArchivo, nuevoNombre);
                huboCambioSistemaArchivos = true;
            }
        }
    }

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

    public Object[][] obtenerTablaEjecutados() {
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
                data[i][5] = p.getPista();   // pista lógica para política de disco
            }
            i++;
        }
        return data;
    }

    // Número de pistas lógicas = cantidad de bloques del disco
    private int calcularPistaDesdeRuta(String ruta) {
        if (gestorDisco == null || gestorDisco.getSistemaArchivos() == null) {
            return 0;
        }

        int totalPistas = gestorDisco.getSistemaArchivos()
                                     .getDisco()
                                     .getCantidadBloques();   // 64 en tu caso

        if (ruta == null) return 0;

        int h = Math.abs(ruta.hashCode());
        return h % totalPistas;   // valor entre 0 y totalPistas-1
    }
}