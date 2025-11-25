/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

import Procesos.Planificador;
import Procesos.ProcesoES;

/**
 * Conecta el Sistema de Archivos con el Planificador de Disco.
 * 
 * Funciones:
 *  - solicitar crear / eliminar / leer archivos (se encolan en el planificador),
 *  - atender la siguiente solicitud según la política de planificación,
 *  - ver el resultado como texto y mostrarlo en la interfaz.
 */
public class GestorDisco {
    
    private SistemaArchivos sistemaArchivos;
    private Planificador planificador;

    /**
     * Constructor del gestor.
     * Crea un sistema de archivos con cierta cantidad de bloques
     * y un planificador de disco.
     * 
     * @param cantidadBloquesDisco número total de bloques del disco simulado
     */
    public GestorDisco(int cantidadBloquesDisco) {
        this.sistemaArchivos = new SistemaArchivos(cantidadBloquesDisco);
        this.planificador = new Planificador();
    }

    public SistemaArchivos getSistemaArchivos() {
        return sistemaArchivos;
    }

    public Planificador getPlanificador() {
        return planificador;
    }

    // ==========================
    // Configuración de política
    // ==========================
    
    /**
     * Cambia la política de planificación del disco.
     * Ejemplos de valores: "FIFO", "SSTF", "SCAN", "CSCAN".
     */
    public void setPoliticaPlanificador(String politica) {
        planificador.setPolitica(politica);
    }
    
    
    // ==============================================
    // Manejo de solicitudes (entradas a la cola de E/S)
    // ==============================================
    
    /**
     * Crea una solicitud para CREAR un archivo.
     * NO crea el archivo en el acto, solo lo manda a la cola de E/S.
     * 
     * @param rutaDirectorio ruta del directorio (por ejemplo "/Documentos/SO")
     * @param nombreArchivo  nombre del archivo (por ejemplo "Proyecto2.txt")
     * @param tamanoBloques  tamaño del archivo en bloques
     * @param posicionCabezal pista a la que se asocia la operación en el disco
     */
    public void solicitarCrearArchivo(String rutaDirectorio, String nombreArchivo, int tamanoBloques, int posicionCabezal) {
        String rutaCompleta = rutaDirectorio + "/" + nombreArchivo;
        planificador.agregarSolicitudCrear(rutaCompleta, posicionCabezal, tamanoBloques);
    }

    /**
     * Crea una solicitud para ELIMINAR un archivo.
     */
    public void solicitarEliminarArchivo(String rutaDirectorio, String nombreArchivo, int posicionCabezal) {
        String rutaCompleta = rutaDirectorio + "/" + nombreArchivo;
        planificador.agregarSolicitud("ELIMINAR", rutaCompleta, posicionCabezal);
    }

    /**
     * Crea una solicitud para LEER un archivo.
     */
    public void solicitarLeerArchivo(String rutaDirectorio, String nombreArchivo, int posicionCabezal) {
        String rutaCompleta = rutaDirectorio + "/" + nombreArchivo;
        planificador.agregarSolicitud("LEER", rutaCompleta, posicionCabezal);
    }

    // ==========================
    // Atender solicitudes
    // ==========================

    /**
     * Atiende la siguiente solicitud según la política del planificador.
     * Ejecuta la operación real sobre el SistemaArchivos.
     * 
     * Devuelve un MENSAJE que luego se puede mostrar en un JTextArea, JLabel, etc.
     */
    public String atenderSiguienteSolicitud() {
        ProcesoES p = planificador.obtenerSiguienteProceso();
        if (p == null) {
            return "No hay solicitudes pendientes en la cola de E/S.";
        }

        String tipo = p.getTipoOperacion();
        String ruta = p.getRutaArchivo();

        // Separar ruta en directorio y nombre de archivo
        String rutaDirectorio = "/";
        String nombreArchivo = ruta;

        if (ruta != null && ruta.contains("/")) {
            int ultimaBarra = ruta.lastIndexOf("/");
            if (ultimaBarra == 0) {
                // Caso tipo "/Archivo.txt"
                rutaDirectorio = "/";
                nombreArchivo = ruta.substring(1); // sin la barra inicial
            } else {
                // Caso tipo "/Documentos/SO/Proyecto2.txt"
                rutaDirectorio = ruta.substring(0, ultimaBarra);
                nombreArchivo = ruta.substring(ultimaBarra + 1);
            }
        }

        String mensaje = "";

        if (tipo.equals("CREAR")) {
            int tamano = p.getTamanoEnBloques();  // aquí usamos el tamaño real

            Archivo nuevo = sistemaArchivos.crearArchivo(rutaDirectorio, nombreArchivo, tamano);
            if (nuevo != null) {
                // Color por defecto para el archivo (se usará luego en la interfaz)
                nuevo.setColorArchivo("azul");
                mensaje = "Archivo creado: " + ruta + " (bloques: " + tamano + ")";
            } else {
                mensaje = "No se pudo crear el archivo: " + ruta;
            }

        } else if (tipo.equals("ELIMINAR")) {

            boolean eliminado = sistemaArchivos.eliminarArchivo(rutaDirectorio, nombreArchivo);
            if (eliminado) {
                mensaje = "Archivo eliminado: " + ruta;
            } else {
                mensaje = "No se pudo eliminar el archivo: " + ruta;
            }

        } else if (tipo.equals("LEER")) {

            Archivo archivo = sistemaArchivos.leerArchivo(rutaDirectorio, nombreArchivo);
            if (archivo != null) {
                mensaje = "Lectura de archivo: " + ruta +
                          " | bloques: " + archivo.getTamañoEnBloques() +
                          " | primer bloque: " + archivo.getPrimerBloque();
            } else {
                mensaje = "No se pudo leer el archivo: " + ruta;
            }

        } else {
            mensaje = "Tipo de operación no reconocido: " + tipo;
        }

        // Marcamos el proceso como terminado
        p.setEstado("terminado");
        return mensaje;
    }

    // ==========================
    // Operaciones de ACTUALIZAR (Update) sin cola
    // ==========================

    /**
     * Renombra un archivo dentro de un directorio.
     * 
     * @param rutaDirectorio ruta del directorio (ej: "/Documentos/SO")
     * @param nombreViejo nombre actual del archivo
     * @param nombreNuevo nuevo nombre deseado
     * @return true si se renombró, false si hubo algún problema
     */
    public boolean renombrarArchivo(String rutaDirectorio, String nombreViejo, String nombreNuevo) {
        return sistemaArchivos.renombrarArchivo(rutaDirectorio, nombreViejo, nombreNuevo);
    }

    /**
     * Renombra un subdirectorio dentro de un directorio padre.
     * 
     * @param rutaPadre ruta del directorio padre (ej: "/Documentos")
     * @param nombreViejo nombre actual del subdirectorio
     * @param nombreNuevo nuevo nombre deseado
     * @return true si se renombró, false si hubo algún problema
     */
    public boolean renombrarDirectorio(String rutaPadre, String nombreViejo, String nombreNuevo) {
        return sistemaArchivos.renombrarDirectorio(rutaPadre, nombreViejo, nombreNuevo);
    }
}