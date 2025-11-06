package com.example.myapplicationf.Models;

public class ContactoEmergencia {
    private String id; // ID del documento de Firestore
    private String nombre;
    private String telefono;

    // Constructor vacío para Firestore
    public ContactoEmergencia() {}

    public ContactoEmergencia(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}