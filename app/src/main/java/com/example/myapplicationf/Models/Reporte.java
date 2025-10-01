package com.example.myapplicationf.Models;

public class Reporte {
    private double lat;
    private double lng;
    private String descripcion;

    // Constructor vacío necesario para Firestore
    public Reporte() {}

    public Reporte(double lat, double lng, String descripcion) {
        this.lat = lat;
        this.lng = lng;
        this.descripcion = descripcion;
    }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
