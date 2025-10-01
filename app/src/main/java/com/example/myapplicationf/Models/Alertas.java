package com.example.myapplicationf.Models;

public class Alertas {
    private String mensaje;
    private long timestamp;

    // Constructor vacío necesario para Firestore
    public Alertas() {}

    public Alertas(String mensaje, long timestamp) {
        this.mensaje = mensaje;
        this.timestamp = timestamp;
    }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
