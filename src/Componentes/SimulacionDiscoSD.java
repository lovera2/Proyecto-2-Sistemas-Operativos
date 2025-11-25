/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

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
     * Marca un bloque como ocupado si estaba libre.
     * No modifica el encadenamiento (campo siguiente).
     */
    public void marcarBloqueOcupado(int indice) {
        if (indice < 0 || indice >= cantidadBloques) {
            return;
        }
        if (!bloques[indice].isOcupado()) {
            bloques[indice].setOcupado(true);
            bloquesLibres--;
        }
    }

    /**
     * Marca un bloque como libre y rompe cualquier enlace de asignación encadenada.
     */
    public void marcarBloqueLibre(int indice) {
        if (indice < 0 || indice >= cantidadBloques) {
            return;
        }
        if (bloques[indice].isOcupado()) {
            bloques[indice].setOcupado(false);
            bloques[indice].setSiguiente(-1); // -1 = no hay siguiente bloque
            bloquesLibres++;
        }
    }
    
    /**
     * Reserva una cantidad de bloques libres en el SD y los enlaza
     * mediante asignación encadenada.
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
                // Marcar como ocupado
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

        /** Seguridad/fiabilidad: si por algún motivo no se logra
        * reservar la cantidad completa de bloques, se revierte
        * todo lo que se había hecho para dejar el disco en un
        * estado consistente (todo o nada).
        */
        if (reservados < cantidad) {
            int actual = primero;
            while (actual != -1) {
                Bloque b = bloques[actual];
                int sig = b.getSiguiente();
                b.setOcupado(false);
                b.setSiguiente(-1);
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
                b.setOcupado(false);
                b.setSiguiente(-1);
                bloquesLibres++;
            }
            actual = siguiente;
        }
    }
    
}