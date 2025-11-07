package com.example.myapplicationf.Models;

public class Reporte {
    private String id; // Nuevo: ID para facilitar la manipulación en Firestore
    private double lat;
    private double lng;
    private String descripcion;
    private String nombreLugar;
    private int riesgo; // 1 = Seguro, 2 = Riesgo Moderado, 3 = Inseguro

    // 🛡️ NUEVOS CAMPOS PARA LA HU-29: VALIDACIÓN Y VERIFICACIÓN AUTOMÁTICA
    private int confirmaciones; // Contador de votos a favor
    private int denuncias;      // Contador de votos en contra
    private String estado;      // "Pendiente", "Verificado", "Falso"

    // Constructor vacío, necesario para Firestore
    public Reporte() {}

    // Constructor completo
    public Reporte(double lat, double lng, String descripcion, String nombreLugar, int riesgo) {
        this.lat = lat;
        this.lng = lng;
        this.descripcion = descripcion;
        this.nombreLugar = nombreLugar;
        this.riesgo = riesgo;

        // 🛡️ Inicialización de campos de validación
        this.confirmaciones = 0;
        this.denuncias = 0;
        this.estado = "Pendiente"; // Todo reporte nuevo comienza como Pendiente
    }

    // --- Getters y Setters ---

    // ID del Documento (necesario para las transacciones)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getNombreLugar() { return nombreLugar; }
    public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }

    public int getRiesgo() { return riesgo; }
    public void setRiesgo(int riesgo) { this.riesgo = riesgo; }

    // 🛡️ Getters y Setters para validación
    public int getConfirmaciones() { return confirmaciones; }
    public void setConfirmaciones(int confirmaciones) { this.confirmaciones = confirmaciones; }

    public int getDenuncias() { return denuncias; }
    public void setDenuncias(int denuncias) { this.denuncias = denuncias; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}