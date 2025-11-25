/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Estructuras;

/**
 * Implementación de una lista simplemente enlazada genérica.
 * Permite operaciones básicas como inserción al final, verificación de vacío,
 * vaciado de lista y representación en cadena de caracteres.
 * 
 * @param <T> Tipo genérico de los elementos almacenados en la lista
 * @author Luis Mariano Lovera
 */
public class Lista<T> {
    private Nodo first;
    private Nodo last;
    private int tamaño;
    
    /**
     * Constructor que inicializa una lista vacía.
     */
    public Lista() {
        this.first = null;
        this.last = null;
        this.tamaño = 0;
    }
    
    /**
     * Verifica si la lista está vacía.
     * 
     * @return true si la lista no contiene elementos, false en caso contrario
     */
    public boolean esVacia(){
        return first == null;
    }
    
    /**
     * Vacía la lista, eliminando todos sus elementos.
     */
    public void vaciar(){
        first=null;
        last=null;
        tamaño=0;
    }
    
    /**
     * Inserta un nuevo elemento al final de la lista.
     * 
     * @param newDato Elemento a insertar
     */
    public void insertarAlFinal(T newDato){
        Nodo aux = new Nodo(newDato);
        
        if(this.esVacia()){
            first = last = aux;
        }else{
            last.setNext(aux);
            last = aux;
        }
        tamaño++;        
    }
    
    /**
     * Genera una representación en cadena de todos los elementos de la lista.
     * Cada elemento se muestra en una línea separada.
     * 
     * @return Cadena con los elementos de la lista separados por saltos de línea
     */
    public String mostrarLista(){
        String cadena="";
        Nodo aux=first;
        
        while (aux!=null){
            cadena=cadena+aux.getDato()+"\n";
            aux=aux.getNext();
        }
        return cadena;
    } 
    
    /**
    * Verifica si un elemento específico existe en la lista.
    * La comparación se realiza usando el método equals() del elemento.
    * 
    * @param dato Elemento a buscar en la lista
    * @return true si el elemento se encuentra en la lista, false en caso contrario.
    */
    public boolean contieneDato(T dato) {
        Nodo actual=first;
    
        while (actual!=null) {
            if (actual.getDato().equals(dato)) {
                return true;
            }
            actual=actual.getNext();
        }
    return false;
    }   


    /**
     * Getters y Setters 
     */
    public Nodo getFirst() {
        return first;
    }

    public void setFirst(Nodo first) {
        this.first = first;
    }

    public Nodo getLast() {
        return last;
    }

    public void setLast(Nodo last) {
        this.last = last;
    }

    public int getTamaño() {
        return tamaño;
    }

    public void setTamaño(int tamaño) {
        this.tamaño = tamaño;
    }
    
    
}
