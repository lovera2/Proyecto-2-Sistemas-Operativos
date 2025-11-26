/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 * Representa el sistema de archivos lógico que se apoya
 * en la Simulación de Disco (SD) para asignar bloques.
 * 
 * Gestiona la creación, eliminación, lectura y renombrado de
 * archivos y directorios, apoyándose en la estructura de
 * Directorio/Archivo y en el disco simulado (SimulacionDiscoSD).
 * 
 * @author Luis Mariano Lovera
 */
public class SistemaArchivos {
    
    private Directorio raiz;            //directorio raíz "/"
    private SimulacionDiscoSD disco;    //disco simulado

    public SistemaArchivos(int cantidadBloques) {
        this.disco = new SimulacionDiscoSD(cantidadBloques);
        this.raiz = new Directorio("root", null);
    }

    public Directorio getRaiz() {
        return raiz;
    }

    public SimulacionDiscoSD getDisco() {
        return disco;
    }

    //Navegación por ruta simple

    /**
     * Busca un directorio a partir de una ruta simple tipo:
     * "/", "/Documentos", "/Documentos/SO", etc.
     * 
     * @param ruta ruta del directorio
     * @return el Directorio si existe, null en caso contrario
     */
    public Directorio obtenerDirectorioDesdeRuta(String ruta) {
        if (ruta == null || ruta.equals("") || ruta.equals("/")) {
            return raiz;
        }

        String sinBarraInicial = ruta;
        if (sinBarraInicial.startsWith("/")) {
            sinBarraInicial = sinBarraInicial.substring(1);
        }

        String[] partes = sinBarraInicial.split("/");

        Directorio actual = raiz;
        for (int i = 0; i < partes.length; i++) {
            String nombreDir = partes[i];
            if (nombreDir.equals("")) {
                continue;
            }
            Directorio hijo = actual.buscarSubdirectorio(nombreDir);
            if (hijo == null) {
                return null; // ruta inválida
            }
            actual = hijo;
        }

        return actual;
    }

    //Crea un subdirectorio dentro de la ruta dada.
    public boolean crearDirectorio(String rutaPadre, String nombreDirectorio) {
        Directorio padre = obtenerDirectorioDesdeRuta(rutaPadre);
        if (padre == null) {
            //ruta padre no existe
            return false;
        }

        //Evitar duplicados de subdirectorio en el mismo padre
        if (padre.buscarSubdirectorio(nombreDirectorio) != null) {
            return false;
        }

        Directorio nuevo = new Directorio(nombreDirectorio, padre);
        padre.agregarSubdirectorio(nuevo);
        return true;
    }

    // Creación de archivos
    
    /**
     * Crea un archivo dentro de un directorio dado,
     * reservando bloques en el disco simulado.
     * 
     * @param rutaDirectorio ruta del directorio donde se creará el archivo
     * @param nombreArchivo nombre lógico del archivo
     * @param tamañoEnBloques tamaño del archivo en bloques
     * @return el Archivo creado, o null si hubo algún error
     */
    public Archivo crearArchivo(String rutaDirectorio, String nombreArchivo, int tamañoEnBloques) {
        Directorio dir = obtenerDirectorioDesdeRuta(rutaDirectorio);
        if (dir == null) {
            return null;
        }

        // Evitar duplicados de archivo en el mismo directorio
        if (dir.buscarArchivo(nombreArchivo) != null) {
            return null;
        }

        // ¿Hay espacio suficiente en el disco?
        if (tamañoEnBloques > disco.getBloquesLibres()) {
            // no hay suficientes bloques libres
            return null;
        }

        // 1) Crear el archivo lógico (sin bloques todavía)
        //    Usa -1 como valor temporal para primerBloque
        Archivo nuevo = new Archivo(nombreArchivo, tamañoEnBloques, -1);

        // 2) Reservar bloques en el SD pasando el color del archivo
        int primerBloque = disco.reservarBloques(tamañoEnBloques, nuevo.getColorArchivo());
        if (primerBloque == -1) {
            // no se pudo reservar la cadena de bloques
            return null;
        }

        // 3) Actualizar el primer bloque del archivo
        nuevo.setPrimerBloque(primerBloque);

        // 4) Agregar el archivo al directorio
        dir.agregarArchivo(nuevo);

        return nuevo;
}
    /**
    * Elimina un archivo dentro de un directorio dado.
    * Libera los bloques que usaba en el disco simulado.
    * 
    * @param rutaDirectorio ruta del directorio donde está el archivo
    * @param nombreArchivo  nombre del archivo a eliminar
    * @return true si se eliminó, false si hubo algún error
    */
    public boolean eliminarArchivo(String rutaDirectorio, String nombreArchivo) {
        // 1. Buscar el directorio
        Directorio dir = obtenerDirectorioDesdeRuta(rutaDirectorio);
        if (dir == null) {
            return false;
        }

        // 2. Buscar el archivo dentro de ese directorio
        Archivo archivo = dir.buscarArchivo(nombreArchivo);
        if (archivo == null) {
            return false;
        }

        // 3. Liberar los bloques en el disco
        int primerBloque = archivo.getPrimerBloque();
        if (primerBloque != -1) {
            disco.liberarCadenaBloques(primerBloque);
        }

        // 4. Eliminar el archivo de la lista del directorio
        boolean eliminado = dir.eliminarArchivo(nombreArchivo);
        if (!eliminado) {
            return false;
        }

        return true;
    }
    
    /**
    * "Lee" un archivo dentro de un directorio dado.
    * En esta simulación, leer significa simplemente devolver el objeto Archivo si existe.
    * 
    * @param rutaDirectorio ruta del directorio
    * @param nombreArchivo nombre del archivo
    * @return el Archivo si existe, null si no
    */
    public Archivo leerArchivo(String rutaDirectorio, String nombreArchivo) {
        Directorio dir = obtenerDirectorioDesdeRuta(rutaDirectorio);
        if (dir == null) {
            return null;
        }

        Archivo archivo = dir.buscarArchivo(nombreArchivo);
        if (archivo == null) {
            return null;
        }
        return archivo;
    }
    
    /**
    * Renombra un archivo dentro de un directorio.
    * 
    * @param rutaDirectorio ruta del directorio donde está el archivo
    * @param nombreViejo nombre actual del archivo
    * @param nombreNuevo nuevo nombre deseado
    * @return true si se renombró, false si hubo algún problema
    */
    public boolean renombrarArchivo(String rutaDirectorio, String nombreViejo, String nombreNuevo) {
        Directorio dir = obtenerDirectorioDesdeRuta(rutaDirectorio);
        if (dir == null) {
            return false;
        }

        Archivo archivo = dir.buscarArchivo(nombreViejo);
        if (archivo == null) {
            return false;
        }

        // Evitar duplicados con mismo nombre
        if (dir.buscarArchivo(nombreNuevo) != null) {
            return false;
        }

        archivo.setNombre(nombreNuevo);
        return true;
    }

    /**
    * Renombra un subdirectorio dentro de un directorio padre.
    * 
    * @param rutaPadre ruta del directorio padre (por ejemplo "/Documentos")
    * @param nombreViejo nombre actual del subdirectorio
    * @param nombreNuevo nuevo nombre deseado
    * @return true si se renombró, false en caso contrario
    */
    public boolean renombrarDirectorio(String rutaPadre, String nombreViejo, String nombreNuevo) {
        Directorio padre = obtenerDirectorioDesdeRuta(rutaPadre);
        if (padre == null) {
            return false;
        }

        Directorio dir = padre.buscarSubdirectorio(nombreViejo);
        if (dir == null) {
            return false;
        }

        // Evitar duplicados
        if (padre.buscarSubdirectorio(nombreNuevo) != null) {
            return false;
        }

        dir.setNombre(nombreNuevo);
        return true;
    }


    /**
     * Elimina un directorio completoapartir de su ruta.
     * 
     * El proceso es:
     *  1. Buscar el directorio a eliminar usando la ruta.
     *  2. No permitir eliminar la raíz.
     *  3. Eliminar recursivamente todos los archivos y subdirectorios que contiene,
     *     liberando los bloques de cada archivo en el disco simulado.
     *  4. Finalmente, pedirle al directorio padre que lo elimine de su lista
     *     de subdirectorios (usando eliminarSubdirectorio).
     * 
     * @param rutaDirectorio ruta del directorio a eliminar (por ejemplo "/Documentos/SO")
     * @return true si se eliminó correctamente, false si hubo algún problema
     */
    public boolean eliminarDirectorio(String rutaDirectorio) {
        // No permitir eliminar la raíz
        if (rutaDirectorio == null || rutaDirectorio.equals("/") || rutaDirectorio.equals("")) {
            return false;
        }

        Directorio dir = obtenerDirectorioDesdeRuta(rutaDirectorio);
        if (dir == null) {
            return false;
        }

        Directorio padre = dir.getPadre();
        if (padre == null) {
            // por seguridad extra: si no tiene padre, lo consideramos raíz
            return false;
        }

        // 1. Eliminar recursivamente todo el contenido (archivos y subdirectorios)
        eliminarContenidoDirectorio(dir);

        // 2. Desenganchar el directorio del padre
        boolean eliminado = padre.eliminarSubdirectorio(dir.getNombre());
        if (!eliminado) {
            return false;
        }

        return true;
    }

    /**
     * Elimina recursivamente todos los archivos y subdirectorios dentro de un directorio.
     * 
     * Para cada archivo:
     *  - Libera la cadena de bloques en el disco.
     *  - Lo elimina de la lista del directorio.
     * 
     * Para cada subdirectorio:
     *  - Llama recursivamente a este método para vaciarlo.
     *  - Luego, lo desengancha de la lista del directorio actual.
     * 
     * Importante: este método NO elimina al directorio actual de su padre,
     * solo vacía su contenido. El desenganche del propio directorio se hace
     * en eliminarDirectorio().
     * 
     * @param dir directorio cuyo contenido será eliminado
     */
    private void eliminarContenidoDirectorio(Directorio dir) {
        // 1. Eliminar todos los archivos de este directorio
        while (dir.getArchivos().getFirst() != null) {
            // Tomar siempre el primer archivo de la lista
            Archivo archivo = (Archivo) dir.getArchivos().getFirst().getDato();
            int primerBloque = archivo.getPrimerBloque();
            if (primerBloque != -1) {
                disco.liberarCadenaBloques(primerBloque);
            }
            dir.eliminarArchivo(archivo.getNombre());
        }

        // 2. Eliminar recursivamente todos los subdirectorios
        while (dir.getSubdirectorios().getFirst() != null) {
            Directorio subdir = (Directorio) dir.getSubdirectorios().getFirst().getDato();
            // Vaciar el contenido del subdirectorio
            eliminarContenidoDirectorio(subdir);
            // Desenganchar el subdirectorio de la lista del directorio actual
            dir.eliminarSubdirectorio(subdir.getNombre());
        }
    }
}