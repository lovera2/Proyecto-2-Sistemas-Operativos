/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

import Estructuras.Lista;
import Estructuras.Nodo;

/**
 * Representa un directorio (carpeta) dentro del sistema de archivos simulado.
 * Un directorio puede contener archivos y otros subdirectorios.
 * 
 * @author Luis Mariano Lovera
 */
public class Directorio {
    
    private String nombre;
    private Directorio padre;
    private Lista<Archivo> archivos;
    private Lista<Directorio> subdirectorios;

    /**
     * Crea un directorio con un nombre y referencia a su directorio padre.
     * Si es la raíz, el padre puede ser null.
     * 
     * @param nombre nombre del directorio (ej: "Documentos")
     * @param padre directorio que lo contiene (null si es el root)
     */
    public Directorio(String nombre, Directorio padre) {
        this.nombre = nombre;
        this.padre = padre;
        this.archivos = new Lista<>();
        this.subdirectorios = new Lista<>();
    }

    // Métodos para archivos
 
    /**
     * Agrega un archivo a este directorio.
     * 
     * @param archivo archivo a agregar
     */
    public void agregarArchivo(Archivo archivo) {
        archivos.insertarAlFinal(archivo);
    }

    /**
     * Busca un archivo por nombre dentro de este directorio.
     * 
     * @param nombreArchivo nombre del archivo a buscar
     * @return el Archivo si lo encuentra, null si no existe
     */
    public Archivo buscarArchivo(String nombreArchivo) {
        Nodo actual = archivos.getFirst();
        
        while (actual != null) {
            Archivo a = (Archivo) actual.getDato();
            if (a.getNombre().equals(nombreArchivo)) {
                return a;
            }
            actual = actual.getNext();
        }
        
        return null;
    }

    /**
     * Elimina un archivo por nombre dentro de este directorio.
     * 
     * @param nombreArchivo nombre del archivo a eliminar
     * @return true si lo encontró y eliminó, false si no existía
     */
    public boolean eliminarArchivo(String nombreArchivo) {
        Nodo actual = archivos.getFirst();
        Nodo anterior = null;
        
        while (actual != null) {
            Archivo a = (Archivo) actual.getDato();
            if (a.getNombre().equals(nombreArchivo)) {
                // eliminar el nodo de la lista "a mano"
                if (anterior == null) {
                    // estaba de primero
                    archivos.setFirst(actual.getNext());
                } else {
                    anterior.setNext(actual.getNext());
                }
                
                // si era el último, actualizar last
                if (actual == archivos.getLast()) {
                    archivos.setLast(anterior);
                }
                
                // ajustar tamaño
                int nuevoTamaño = archivos.getTamaño() - 1;
                archivos.setTamaño(nuevoTamaño);
                return true;
            }
            anterior = actual;
            actual = actual.getNext();
        }
        
        return false;
    }

    // Métodos para subdirectorios

    /**
     * Agrega un subdirectorio a este directorio.
     * 
     * @param dir subdirectorio a agregar
     */
    public void agregarSubdirectorio(Directorio dir) {
        subdirectorios.insertarAlFinal(dir);
    }

    /**
     * Busca un subdirectorio por nombre dentro de este directorio.
     * (NO busca recursivamente en los nietos, solo en el nivel actual).
     * 
     * @param nombreDirectorio nombre del subdirectorio a buscar
     * @return el Directorio si lo encuentra, null si no existe
     */
    public Directorio buscarSubdirectorio(String nombreDirectorio) {
        Nodo actual = subdirectorios.getFirst();
        
        while (actual != null) {
            Directorio d = (Directorio) actual.getDato();
            if (d.getNombre().equals(nombreDirectorio)) {
                return d;
            }
            actual = actual.getNext();
        }
        
        return null;
    }


    // Getters

    public String getNombre() {
        return nombre;
    }

    public Directorio getPadre() {
        return padre;
    }

    public Lista<Archivo> getArchivos() {
        return archivos;
    }

    public Lista<Directorio> getSubdirectorios() {
        return subdirectorios;
    }

    @Override
    public String toString() {
        return nombre;
    }
}