/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 * Representa el sistema de archivos lógico que se apoya
 * en la Simulación de Disco (SD) para asignar bloques.
 * 
 * @author Luis Mariano Lovera
 */
public class SistemaArchivos {
    
    private Directorio raiz;            // directorio raíz "/"
    private SimulacionDiscoSD disco;    // disco simulado

    /**
     * Crea un sistema de archivos con un disco de cierta cantidad de bloques.
     * 
     * @param cantidadBloques número total de bloques del disco simulado
     */
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

    // =========================
    // Navegación por ruta simple
    // =========================

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

    // Creacion de directorios
 
    /**
     * Crea un subdirectorio dentro de la ruta dada.
     * 
     * @param rutaPadre ruta del directorio padre (Ej: "/", "/Documentos")
     * @param nombreDirectorio nombre del nuevo directorio
     * @return true si se creó, false si no (ruta inválida o ya existe)
     */
    public boolean crearDirectorio(String rutaPadre, String nombreDirectorio) {
        Directorio padre = obtenerDirectorioDesdeRuta(rutaPadre);
        if (padre == null) {
            return false; // ruta no existe
        }

        // Evitar duplicados
        if (padre.buscarSubdirectorio(nombreDirectorio) != null) {
            return false; // ya existe un subdirectorio con ese nombre
        }

        Directorio nuevo = new Directorio(nombreDirectorio, padre);
        padre.agregarSubdirectorio(nuevo);
        return true;
    }

    // Creacion de archivos
    
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
            System.out.println("Ruta inválida: " + rutaDirectorio);
            return null;
        }

        // ¿Hay espacio suficiente en el disco?
        if (tamañoEnBloques > disco.getBloquesLibres()) {
            System.out.println("No hay suficientes bloques libres en el disco.");
            return null;
        }

        // Reservar bloques en el SD (asignación encadenada)
        int primerBloque = disco.reservarBloques(tamañoEnBloques);
        if (primerBloque == -1) {
            System.out.println("No se pudo reservar la cadena de bloques.");
            return null;
        }

        // Crear el archivo lógico y agregarlo al directorio
        Archivo nuevo = new Archivo(nombreArchivo, tamañoEnBloques, primerBloque);
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
            System.out.println("Ruta inválida: " + rutaDirectorio);
            return false;
        }

        // 2. Buscar el archivo dentro de ese directorio
        Archivo archivo = dir.buscarArchivo(nombreArchivo);
        if (archivo == null) {
            System.out.println("El archivo no existe en ese directorio.");
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
            System.out.println("No se pudo eliminar el archivo de la lista del directorio.");
            return false;
        }

        System.out.println("Archivo \"" + nombreArchivo + "\" eliminado correctamente.");
        return true;
    }
    
    /**
    * "Lee" un archivo dentro de un directorio dado.
    * En esta simulación, leer significa simplemente
    * devolver el objeto Archivo si existe.
    * 
    * @param rutaDirectorio ruta del directorio
    * @param nombreArchivo nombre del archivo
    * @return el Archivo si existe, null si no
    */
    public Archivo leerArchivo(String rutaDirectorio, String nombreArchivo) {
        Directorio dir = obtenerDirectorioDesdeRuta(rutaDirectorio);
        if (dir == null) {
            System.out.println("Ruta inválida: " + rutaDirectorio);
            return null;
        }

        Archivo archivo = dir.buscarArchivo(nombreArchivo);
        if (archivo == null) {
            System.out.println("El archivo no existe en ese directorio.");
            return null;
        }
        
        // Aquí podrías, si quisieras, simular lectura de bloques, etc.
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
            System.out.println("Ruta inválida: " + rutaDirectorio);
            return false;
        }

        Archivo archivo = dir.buscarArchivo(nombreViejo);
        if (archivo == null) {
            System.out.println("El archivo a renombrar no existe.");
            return false;
        }

        // Opcional: evitar duplicados con mismo nombre
        if (dir.buscarArchivo(nombreNuevo) != null) {
            System.out.println("Ya existe un archivo con el nombre nuevo.");
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
            System.out.println("Ruta padre inválida: " + rutaPadre);
            return false;
        }

        Directorio dir = padre.buscarSubdirectorio(nombreViejo);
        if (dir == null) {
            System.out.println("El subdirectorio a renombrar no existe.");
            return false;
        }

        // Opcional: evitar duplicados
        if (padre.buscarSubdirectorio(nombreNuevo) != null) {
            System.out.println("Ya existe un subdirectorio con el nombre nuevo.");
            return false;
        }

        dir.setNombre(nombreNuevo);
        return true;
    }
    
    
}
