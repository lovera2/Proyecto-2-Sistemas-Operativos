/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

import java.awt.Color;

/**
 * Representa la Simulación de un Disco (SD) como un arreglo de bloques.
 * Cada bloque puede estar libre u ocupado y puede estar enlazado a otro
 * bloque mediante asignación encadenada.
 * 
 * @author Luis MarianoLovera
 */
public class SimulacionDiscoSD {
    private Bloque[] bloques;
    private int cantidadBloques;
    private int bloquesLibres;

    public SimulacionDiscoSD(int cantidadBloques) {
        this.cantidadBloques = cantidadBloques;
        this.bloques = new Bloque[cantidadBloques];
        
        for (int i = 0; i < cantidadBloques; i++) {
            bloques[i] = new Bloque(i);
        }
        
        this.bloquesLibres = cantidadBloques;
    }

    public int getCantidadBloques() {
        return cantidadBloques;
    }

    public int getBloquesLibres() {
        return bloquesLibres;
    }

    public Bloque getBloque(int indice) {
        if (indice < 0 || indice >= cantidadBloques) {
            return null;
        }
        return bloques[indice];
    }

    /**
     * Marca un bloque como ocupado si estaba libre (sin preocuparse por color).
     * Esto se usa solo en casos genéricos. Para archivos, es mejor usar
     * reservarBloques(cantidad, colorArchivo).
     */
    public void marcarBloqueOcupado(int indice) {
        if (indice < 0 || indice >= cantidadBloques) {
            return;
        }
        if (!bloques[indice].isOcupado()) {
            // lo marcamos ocupado sin color específico
            bloques[indice].setOcupado(true);
            bloquesLibres--;
        }
    }

    /**
     * Marca un bloque como libre y rompe cualquier enlace de asignación encadenada,
     * limpiando también el color.
     */
    public void marcarBloqueLibre(int indice) {
        if (indice < 0 || indice >= cantidadBloques) {
            return;
        }
        if (bloques[indice].isOcupado()) {
            bloques[indice].liberar();   // <-- usa el método nuevo del Bloque
            bloquesLibres++;
        }
    }
    
    /**
     * Reserva una cantidad de bloques libres en el SD y los enlaza
     * mediante asignación encadenada (sin asignar color).
     * 
     * @param cantidad número de bloques que necesita el archivo
     * @return índice del primer bloque de la cadena, o -1 si no hay espacio suficiente
     */
    public int reservarBloques(int cantidad) {
        if (cantidad <= 0) {
            return -1;
        }
        if (cantidad > bloquesLibres) {
            // No hay suficientes bloques libres
            return -1;
        }

        int primero = -1;
        int anterior = -1;
        int reservados = 0;

        for (int i = 0; i < cantidadBloques && reservados < cantidad; i++) {
            if (!bloques[i].isOcupado()) {
                // Marcar como ocupado (sin color todavía)
                bloques[i].setOcupado(true);
                bloquesLibres--;

                if (primero == -1) {
                    primero = i; // este es el primer bloque de la cadena
                } else {
                    bloques[anterior].setSiguiente(i);
                }

                anterior = i;
                reservados++;
            }
        }

        // all-or-nothing
        if (reservados < cantidad) {
            int actual = primero;
            while (actual != -1) {
                Bloque b = bloques[actual];
                int sig = b.getSiguiente();
                b.liberar();      // libera y limpia color / siguiente
                bloquesLibres++;
                actual = sig;
            }
            return -1;
        }

        // El último bloque apunta a -1 (fin de la cadena)
        bloques[anterior].setSiguiente(-1);
        return primero;
    }

    /**
     * Versión cómoda: reserva bloques y les asigna el color del archivo.
     * No rompe nada de tu código actual porque es un método nuevo.
     */
    public int reservarBloques(int cantidad, Color colorArchivo) {
        int primerBloque = reservarBloques(cantidad);
        if (primerBloque == -1) {
            return -1;
        }

        int actual = primerBloque;
        while (actual != -1) {
            Bloque b = bloques[actual];
            b.setColorBloque(colorArchivo);
            actual = b.getSiguiente();
        }

        return primerBloque;
    }

    /**
     * Libera una cadena de bloques que pertenece a un archivo,
     * empezando desde el primer bloque.
     * 
     * @param primerBloque índice del primer bloque de la cadena a liberar
     */
    public void liberarCadenaBloques(int primerBloque) {
        int actual = primerBloque;

        while (actual != -1) {
            Bloque b = bloques[actual];
            int siguiente = b.getSiguiente(); // se guarda antes de romper el enlace
            if (b.isOcupado()) {
                b.liberar();   // limpia ocupado, siguiente y color
                bloquesLibres++;
            }
            actual = siguiente;
        }
    }
}