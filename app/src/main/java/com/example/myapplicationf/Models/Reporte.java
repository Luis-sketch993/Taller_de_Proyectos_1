package com.example.myapplicationf.Models;

public class Reporte {
    private double lat;
    private double lng;
    private String descripcion;
    private String nombreLugar; //  nuevo campo para el lugar

    // Constructor vacío necesario para Firestore
    public Reporte() {}

    // Constructor completo
    public Reporte(double lat, double lng, String descripcion, String nombreLugar) {
        this.lat = lat;
        this.lng = lng;
        this.descripcion = descripcion;
        this.nombreLugar = nombreLugar;
    }

    // Getters y Setters
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getNombreLugar() { return nombreLugar; }
    public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }
}
