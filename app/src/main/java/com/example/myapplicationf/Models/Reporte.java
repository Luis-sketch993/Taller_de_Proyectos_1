package com.example.myapplicationf.Models;

public class Reporte {
    private double lat;
    private double lng;
    private String descripcion;
    private String nombreLugar;
    // 🔹 CAMPO AÑADIDO: Guardará el nivel de riesgo del reporte.
    // Ejemplo: 1 = Seguro, 2 = Riesgo Moderado, 3 = Inseguro.
    private int riesgo;

    // Constructor vacío, es muy importante para que Firestore funcione correctamente.
    public Reporte() {}

    // 🔹 CONSTRUCTOR ACTUALIZADO: Ahora incluye el parámetro 'riesgo'.
    public Reporte(double lat, double lng, String descripcion, String nombreLugar, int riesgo) {
        this.lat = lat;
        this.lng = lng;
        this.descripcion = descripcion;
        this.nombreLugar = nombreLugar;
        this.riesgo = riesgo; // Se asigna el nuevo valor.
    }

    // --- Getters y Setters ---

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getNombreLugar() { return nombreLugar; }
    public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }

    // 🔹 MÉTODOS AÑADIDOS: Estos son los que solucionan el error en HomeFragment.
    public int getRiesgo() {
        return riesgo;
    }

    public void setRiesgo(int riesgo) {
        this.riesgo = riesgo;
    }
}