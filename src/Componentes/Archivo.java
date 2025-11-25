/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 * Representa un archivo dentro del sistema de archivos lógico.
 * Cada archivo tiene:
 *  - un nombre,
 *  - un tamaño en bloques,
 *  - el índice del primer bloque en el disco simulado (asignación encadenada),
 *  - un color lógico para poder dibujarlo en la interfaz.
 * 
 * @author Luis Mariano Lovera
 */
public class Archivo {
    
    private String nombre;
    private int tamañoEnBloques;
    private int primerBloque;   // índice del primer bloque en SimulacionDiscoSD

    // Color lógico para la interfaz (ej: "rojo", "azul", "#FF0000", etc.)
    private String colorArchivo;

    /**
     * Constructor básico.
     * Por ahora asignamos un color por defecto. 
     * Más adelante lo podemos cambiar al crear el archivo.
     */
    public Archivo(String nombre, int tamañoEnBloques, int primerBloque) {
        this.nombre = nombre;
        this.tamañoEnBloques = tamañoEnBloques;
        this.primerBloque = primerBloque;
        this.colorArchivo = "gris"; // color por defecto
    }

    // =============================
    // Getters y setters básicos
    // =============================

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nuevoNombre) {
        this.nombre = nuevoNombre;
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

    public String getColorArchivo() {
        return colorArchivo;
    }

    public void setColorArchivo(String colorArchivo) {
        this.colorArchivo = colorArchivo;
    }
}