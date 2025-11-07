package com.example.myapplicationf.Models;

// Modelo para guardar el promedio en Firestore
public class CalificacionZona {

    private Double lat;
    private Double lng;
    private Long sumaCalificaciones; // Suma total de estrellas
    private Long numCalificaciones;  // Total de votos

    public CalificacionZona() {}

    public CalificacionZona(Double lat, Double lng, Long sumaCalificaciones, Long numCalificaciones) {
        this.lat = lat;
        this.lng = lng;
        this.sumaCalificaciones = sumaCalificaciones;
        this.numCalificaciones = numCalificaciones;
    }

    // --- Getters y Setters ---
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Long getSumaCalificaciones() { return sumaCalificaciones; }
    public void setSumaCalificaciones(Long sumaCalificaciones) { this.sumaCalificaciones = sumaCalificaciones; }
    public Long getNumCalificaciones() { return numCalificaciones; }
    public void setNumCalificaciones(Long numCalificaciones) { this.numCalificaciones = numCalificaciones; }
}