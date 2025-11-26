/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 * Conecta la GUI con el Sistema de Archivos.
 *
 * YA NO maneja colas ni políticas de disco.
 * Eso lo hace GestorProcesoES + Planificador.
 */
public class GestorDisco {

    private SistemaArchivos sistemaArchivos;

    public GestorDisco(int cantidadBloquesDisco) {
        this.sistemaArchivos = new SistemaArchivos(cantidadBloquesDisco);
    }

    public SistemaArchivos getSistemaArchivos() {
        return sistemaArchivos;
    }
}