/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

import java.awt.Color;

/**
 * Representa un bloque del disco simulado.
 * Cada bloque tiene un id, sabe si está ocupado,
 * guarda el índice del siguiente bloque del archivo (asignación encadenada)
 * y un color lógico para poder dibujarlo en la interfaz.
 * 
 * @author Luis Mariano Lovera
 */
public class Bloque {
    
    private int id;
    private boolean ocupado;
    private int siguiente;      // -1 si no hay siguiente bloque
    private Color colorBloque;  // color del archivo que ocupa este bloque (null si está libre)

    public Bloque(int id) {
        this.id = id;
        this.ocupado = false;
        this.siguiente = -1;
        this.colorBloque = null; // bloque libre, sin color
    }

    public int getId() {
        return id;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public void setOcupado(boolean ocupado) {
        this.ocupado = ocupado;
    }

    public int getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(int siguiente) {
        this.siguiente = siguiente;
    }

    // ====== NUEVO: manejo de color del bloque ======

    public Color getColorBloque() {
        return colorBloque;
    }

    public void setColorBloque(Color colorBloque) {
        this.colorBloque = colorBloque;
    }

    /**
     * Marca el bloque como ocupado y asigna un color.
     */
    public void ocupar(Color colorArchivo) {
        this.ocupado = true;
        this.colorBloque = colorArchivo;
    }

    /**
     * Libera el bloque y limpia su color y enlace.
     */
    public void liberar() {
        this.ocupado = false;
        this.siguiente = -1;
        this.colorBloque = null;
    }
}