/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Procesos;

import Componentes.GestorDisco;
import Estructuras.Cola;
import java.util.concurrent.Semaphore;

public class GestorProcesoES {

    private Cola<ProcesoES> colaNuevos;
    private Cola<ProcesoES> colaListos;
    private Cola<ProcesoES> colaBloqueados;
    private Cola<ProcesoES> colaTerminados;

    private final Semaphore mutexColas;  // exclusión mutua

    private Planificador planificador;   // políticas FIFO/SSTF/SCAN/CSCAN
    private GestorDisco gestorDisco;     // para ejecutar la operación real en disco

    // contador simple de ids
    private int siguienteId = 1;

    public GestorProcesoES(Planificador planificador, GestorDisco gestorDisco) {
        this.colaNuevos     = new Cola<>();
        this.colaListos     = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.mutexColas     = new Semaphore(1);

        this.planificador   = planificador;
        this.gestorDisco    = gestorDisco;
    }

    public Planificador getPlanificador() {
        return planificador;
    }

    // ====== creación / admisión ======

    private int generarId() {
        int id = siguienteId;
        siguienteId = siguienteId + 1;
        return id;
    }

    /**
     * Crea un ProcesoES tipo CREAR y lo mete en NUEVOS -> LISTOS.
     */
    public void crearProcesoCrear(String ruta, int pista, int tamBloques) {
        ProcesoES p = new ProcesoES(
                generarId(),
                "CREAR",
                ruta,
                pista
        );
        p.setEstado("nuevo");
        p.setTamanoEnBloques(tamBloques);

        try {
            mutexColas.acquire();

            colaNuevos.encolar(p);

            // admisión inmediata -> LISTO
            moverAListosSinLock(p);

        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Crea un ProcesoES tipo ELIMINAR.
     */
    public void crearProcesoEliminar(String ruta, int pista) {
        ProcesoES p = new ProcesoES(
                generarId(),
                "ELIMINAR",
                ruta,
                pista
        );
        p.setEstado("nuevo");

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
            moverAListosSinLock(p);
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Crea un ProcesoES tipo LEER.
     */
    public void crearProcesoLeer(String ruta, int pista) {
        ProcesoES p = new ProcesoES(
                generarId(),
                "LEER",
                ruta,
                pista
        );
        p.setEstado("nuevo");

        try {
            mutexColas.acquire();
            colaNuevos.encolar(p);
            moverAListosSinLock(p);
        } catch (InterruptedException e) {
        } finally {
            mutexColas.release();
        }
    }

    private void moverAListosSinLock(ProcesoES p) {
        p.setEstado("listo");
        colaListos.encolar(p);
    }

    // ====== LISTO -> EJECUTANDO -> BLOQUEADO ======

    /**
     * Usa el planificador para elegir el siguiente proceso a ejecutar.
     * Lo saca de LISTOS y lo pasa a BLOQUEADO simulando que está esperando E/S.
     */
    public ProcesoES despacharSiguiente() {
        try {
            mutexColas.acquire();

            //Ahora el planificador necesita la cola de LISTOS
            ProcesoES p = planificador.obtenerSiguienteProceso(colaListos);
            if (p == null) {
                return null;
            }

            // El planificador ya lo sacó de colaListos y lo marcó "ejecutando".
            //Aquí simulamos que va a disco: pasa a BLOQUEADO.
            p.setEstado("bloqueado");
            p.setTiempoRestanteES(calcularTiempoES(p));
            colaBloqueados.encolar(p);

            return p;

        } catch (InterruptedException e) {
            return null;
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Tiempo de servicio de disco simulado.
     * Puedes ajustar el factor según quieras que "tarde" más o menos.
     */
    private int calcularTiempoES(ProcesoES p) {
        int bloques = p.getTamanoEnBloques();
        if (bloques <= 0) {
            bloques = 1;
        }
        return bloques * 2; // por ejemplo, cada bloque son 2 "ticks"
    }

    // ====== TICK: BLOQUEADO -> TERMINADO ======

    /**
     * Se llama periódicamente (por un Timer en la GUI).
     * Resta 1 al tiempoRestanteES de los bloqueados.
     * Cuando llega a 0, se ejecuta la operación real en disco
     * y el proceso pasa a TERMINADO.
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
                        // Termina su E/S: ejecutar operación real en disco
                        colaBloqueados.removeAt(i);
                        n--;

                        // Ejecutar CRUD en el sistema de archivos
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

    /**
     * Llama a GestorDisco para que haga CREAR/ELIMINAR/LEER de verdad.
     */
    private void ejecutarOperacionReal(ProcesoES p) {
        if (gestorDisco == null || p == null) return;

        String tipo = p.getTipoOperacion();
        String rutaCompleta = p.getRutaArchivo();

        // separar ruta en directorio + nombre, igualito a tu GestorDisco
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

        if (tipo.equals("CREAR")) {
            int tam = p.getTamanoEnBloques();
            gestorDisco.getSistemaArchivos().crearArchivo(rutaDirectorio, nombreArchivo, tam);
        } else if (tipo.equals("ELIMINAR")) {
            gestorDisco.getSistemaArchivos().eliminarArchivo(rutaDirectorio, nombreArchivo);
        } else if (tipo.equals("LEER")) {
            gestorDisco.getSistemaArchivos().leerArchivo(rutaDirectorio, nombreArchivo);
        }
        // si quieres, aquí podrías guardar mensajes en un log para la GUI
    }

    // helpers internos

    /**
     * Quita un proceso de una cola dada si está ahí (comparando por referencia).
     * NO usa semáforo, se asume que ya está tomado.
     */
    private void quitarDeColaSinLock(Cola<ProcesoES> cola, ProcesoES objetivo) {
        int n = cola.verTamano();
        int i = 0;
        while (i < n) {
            ProcesoES p = cola.getAt(i);
            if (p == objetivo) {
                cola.removeAt(i);
                n--;
                continue;
            }
            i++;
        }
    }

    // Helpers para JTable

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

    private Object[][] construirMatrizDesdeCola(Cola<ProcesoES> cola) {
        int n = cola.verTamano();
        Object[][] data = new Object[n][5];

        int i = 0;
        while (i < n) {
            ProcesoES p = cola.getAt(i);
            if (p != null) {
                data[i][0] = p.getIdProceso();
                data[i][1] = p.getTipoOperacion();
                data[i][2] = p.getRutaArchivo();
                data[i][3] = p.getEstado();
                data[i][4] = p.getTiempoRestanteES();
            }
            i++;
        }
        return data;
    }
}