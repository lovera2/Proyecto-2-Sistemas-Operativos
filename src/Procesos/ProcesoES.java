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
    private String tipoOperacion;   // "CREAR", "ELIMINAR", "LEER", etc.
    private String rutaArchivo;     // Ej: "/Documentos/SO/Proyecto2.txt"
    private int posicionCabezal;    // posición en el disco (pista)
    private String estado;          // "nuevo", "listo", "ejecutando", "terminado"

    // Tamaño del archivo en bloques (solo se usa para operaciones CREAR)
    private int tamanoEnBloques;

    /**
     * Constructor básico del proceso de E/S.
     */
    public ProcesoES(int idProceso, String tipoOperacion, String rutaArchivo, int posicionCabezal) {
        this.idProceso = idProceso;
        this.tipoOperacion = tipoOperacion;
        this.rutaArchivo = rutaArchivo;
        this.posicionCabezal = posicionCabezal;
        this.estado = "nuevo";
        this.tamanoEnBloques = 0;   // por defecto, si no se usa (ELIMINAR, LEER, etc.)
    }

    // Getters y setters

    public int getIdProceso() {
        return idProceso;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public int getPosicionCabezal() {
        return posicionCabezal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public int getTamanoEnBloques() {
        return tamanoEnBloques;
    }

    public void setTamanoEnBloques(int tamanoEnBloques) {
        this.tamanoEnBloques = tamanoEnBloques;
    }
}