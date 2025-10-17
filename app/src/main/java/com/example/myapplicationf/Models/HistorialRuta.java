package com.example.myapplicationf.Models;

import com.google.firebase.Timestamp;

import java.util.Date;

public class HistorialRuta {
    private String userId;
    private String origenNombre;
    private String destinoNombre;
    private double origenLat;
    private double origenLng;
    private double destinoLat;
    private double destinoLng;
    private Timestamp timestamp;

    // Constructor vacío requerido por Firestore
    public HistorialRuta() {}

    // Constructor completo
    public HistorialRuta(String userId, String origenNombre, String destinoNombre,
                         double origenLat, double origenLng, double destinoLat, double destinoLng,
                         Timestamp timestamp) {
        this.userId = userId;
        this.origenNombre = origenNombre;
        this.destinoNombre = destinoNombre;
        this.origenLat = origenLat;
        this.origenLng = origenLng;
        this.destinoLat = destinoLat;
        this.destinoLng = destinoLng;
        this.timestamp = timestamp;
    }

    // Getters y Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOrigenNombre() { return origenNombre; }
    public void setOrigenNombre(String origenNombre) { this.origenNombre = origenNombre; }

    public String getDestinoNombre() { return destinoNombre; }
    public void setDestinoNombre(String destinoNombre) { this.destinoNombre = destinoNombre; }

    public double getOrigenLat() { return origenLat; }
    public void setOrigenLat(double origenLat) { this.origenLat = origenLat; }

    public double getOrigenLng() { return origenLng; }
    public void setOrigenLng(double origenLng) { this.origenLng = origenLng; }

    public double getDestinoLat() { return destinoLat; }
    public void setDestinoLat(double destinoLat) { this.destinoLat = destinoLat; }

    public double getDestinoLng() { return destinoLng; }
    public void setDestinoLng(double destinoLng) { this.destinoLng = destinoLng; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}

