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
 *  - un color lógico para poder dibujarlo en la interfaz,
 *  - un propietario,
 *  - permisos de acceso,
 *  - el id del proceso que lo creó.
 * 
 * @author Luis Mariano Lovera
 */
public class Archivo {
    
    private String nombre;
    private int tamañoEnBloques;
    private int primerBloque;   //índice del primer bloque en SimulacionDiscoSD
    private String colorArchivo; //Color lógico para la interfaz
    private String propietario;      
    private String permisos;          

    //Proceso que creó el archivo (para estadísticas / tabla de asignación)
    private int idProcesoCreador;

  
    public Archivo(String nombre, int tamañoEnBloques, int primerBloque) {
        this.nombre = nombre;
        this.tamañoEnBloques = tamañoEnBloques;
        this.primerBloque = primerBloque;
        this.colorArchivo = "gris";   //color por defecto

        //Valores por defecto para permisos/propietario/proceso creador
        this.propietario = null;      
        this.permisos = "rw";         
        this.idProcesoCreador = -1;  
    }

    //Getters y setters básicos

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

    //Getters y setters de permisos

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        this.propietario = propietario;
    }

    public String getPermisos() {
        return permisos;
    }

    public void setPermisos(String permisos) {
        this.permisos = permisos;
    }

    public int getIdProcesoCreador() {
        return idProcesoCreador;
    }

    public void setIdProcesoCreador(int idProcesoCreador) {
        this.idProcesoCreador = idProcesoCreador;
    }
}