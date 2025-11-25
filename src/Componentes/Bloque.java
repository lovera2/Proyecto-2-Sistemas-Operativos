/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 * Representa un bloque del disco simulado.
 * Cada bloque tiene un id, sabe si está ocupado
 * y guarda el índice del siguiente bloque del archivo (asignación encadenada).
 * 
 * @author Luis Mariano Lovera
 */
public class Bloque {
    private int id;
    private boolean ocupado;
    private int siguiente; // -1 si no hay siguiente bloque

    public Bloque(int id) {
        this.id = id;
        this.ocupado = false;
        this.siguiente = -1;
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
}