/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 * Representa un archivo dentro del sistema de archivos simulado.
 * Cada archivo tiene un nombre, un tamaño en bloques y
 * un índice al primer bloque que ocupa en el disco simulado.
 * 
 * @author Luis Mariano Lovera
 */
public class Archivo {
    
    private String nombre;
    private int tamañoEnBloques;
    private int primerBloque; // índice del primer bloque en el SD, -1 si no tiene bloques

    /**
     * Constructor básico para crear un archivo.
     * 
     * @param nombre nombre lógico del archivo (ej: "notas.txt")
     * @param tamañoEnBloques tamaño del archivo medido en cantidad de bloques
     * @param primerBloque índice del primer bloque asignado en el disco
     */
    public Archivo(String nombre, int tamañoEnBloques, int primerBloque) {
        this.nombre = nombre;
        this.tamañoEnBloques = tamañoEnBloques;
        this.primerBloque = primerBloque;
    }

    // Getters y setters básicos

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getTamañoEnBloques() {
        return tamañoEnBloques;
    }

    public void setTamañoEnBloques(int tamañoEnBloques) {
        this.tamañoEnBloques = tamañoEnBloques;
    }

    public int getPrimerBloque() {
        return primerBloque;
    }

    public void setPrimerBloque(int primerBloque) {
        this.primerBloque = primerBloque;
    }
    
    @Override
    public String toString() {
        // Esto se puede usar para mostrar el archivo en algún lugar
        return nombre + " (" + tamañoEnBloques + " bloques)";
    }
}