/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 * Clase simple para probar el sistema de archivos y el disco simulado.
 */
public class PruebaSistemaArchivos {
    
    public static void main(String[] args) {
        
        // 1. Crear un sistema de archivos con 10 bloques en el disco
        SistemaArchivos sistema = new SistemaArchivos(10);
        
        // 2. Crear un directorio /Documentos
        boolean creadoDir = sistema.crearDirectorio("/", "Documentos");
        System.out.println("Directorio /Documentos creado: " + creadoDir);
        
        // 3. Crear un subdirectorio /Documentos/SO
        boolean creadoSub = sistema.crearDirectorio("/Documentos", "SO");
        System.out.println("Directorio /Documentos/SO creado: " + creadoSub);
        
        // 4. Crear un archivo de 3 bloques en /Documentos/SO
        Archivo archivo1 = sistema.crearArchivo("/Documentos/SO", "Proyecto2.txt", 3);
        if (archivo1 != null) {
            System.out.println("Archivo creado: " + archivo1.getNombre() +
                               ", primer bloque: " + archivo1.getPrimerBloque());
        } else {
            System.out.println("No se pudo crear el archivo Proyecto2.txt");
        }
        
        // 5. Ver estado del disco después de crear el archivo
        System.out.println("\nEstado del disco después de crear el archivo:");
        sistema.getDisco().imprimirEstadoBloques();
        
        // 6. Eliminar el archivo Proyecto2.txt
        boolean eliminado = sistema.eliminarArchivo("/Documentos/SO", "Proyecto2.txt");
        System.out.println("\nArchivo Proyecto2.txt eliminado: " + eliminado);
        
        // 7. Ver estado del disco después de eliminar el archivo
        System.out.println("\nEstado del disco después de eliminar el archivo:");
        sistema.getDisco().imprimirEstadoBloques();
    }
}