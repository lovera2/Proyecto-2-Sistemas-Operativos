/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Procesos;

/**
 * Representa un proceso de Entrada/Salida (E/S) hacia el disco.
 * Guarda la información básica de una petición al sistema de archivos.
 */
public class ProcesoES {
    
    private int idProceso;
    private String tipoOperacion;   // "CREAR", "ELIMINAR", "LEER", "RENOMBRAR_ARCHIVO", "RENOMBRAR_DIRECTORIO"
    private String rutaArchivo;     // Ej: "/Documentos/SO/Proyecto2.txt"
    private int posicionCabezal;    // posición en el disco (pista)
    private String estado;          
    private int tamanoEnBloques;
    private int tiempoRestanteES;

    private String nuevoNombre;     // nombre nuevo en un renombrado
    private boolean esDirectorio;   // true si es renombrar directorio

    public ProcesoES(int idProceso, String tipoOperacion, String rutaArchivo, int posicionCabezal) {
        this.idProceso = idProceso;
        this.tipoOperacion = tipoOperacion;
        this.rutaArchivo = rutaArchivo;
        this.posicionCabezal = posicionCabezal;
        this.estado = "nuevo";
        this.tamanoEnBloques = 0;
        this.tiempoRestanteES = 0;
        this.nuevoNombre = null;
        this.esDirectorio = false;
    }

    // Getters y setters

    public int getIdProceso() { return idProceso; }

    public String getTipoOperacion() { return tipoOperacion; }

    public String getRutaArchivo() { return rutaArchivo; }

    public int getPosicionCabezal() { return posicionCabezal; }

    public String getEstado() { return estado; }

    public void setEstado(String nuevoEstado) { this.estado = nuevoEstado; }

    public int getTamanoEnBloques() { return tamanoEnBloques; }

    public void setTamanoEnBloques(int tamanoEnBloques) { this.tamanoEnBloques = tamanoEnBloques; }

    public int getTiempoRestanteES() { return tiempoRestanteES; }

    public void setTiempoRestanteES(int tiempoRestanteES) { this.tiempoRestanteES = tiempoRestanteES; }

    public String getNuevoNombre() {
        return nuevoNombre;
    }

    public void setNuevoNombre(String nuevoNombre) {
        this.nuevoNombre = nuevoNombre;
    }

    public boolean isEsDirectorio() {
        return esDirectorio;
    }

    public void setEsDirectorio(boolean esDirectorio) {
        this.esDirectorio = esDirectorio;
    }
}