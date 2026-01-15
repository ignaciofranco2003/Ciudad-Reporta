package com.example.ciudad_reporta.menu_principal.mis_reportes;

public class Reporte {

    // Atributos del reporte
    private int id;
    private String categoria;
    private String descripcion;
    private String estado;
    private String imagenUrl;

    // Constructor para inicializar un objeto Reporte
    public Reporte(int id, String categoria, String descripcion, String estado, String imagenUrl) {
        this.id = id;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.estado = estado;
        this.imagenUrl = imagenUrl;
    }

    // Getter para obtener el estado del reporte
    public String getEstado() {
        return estado;
    }

    // Otros getters opcionales
    public int getId() { return id; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public String getImagenUrl() { return imagenUrl; }
}