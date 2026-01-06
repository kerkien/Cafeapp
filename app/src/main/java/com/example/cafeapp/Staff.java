package com.example.cafeapp;

public class Staff {
    private String uid;
    private String email;
    private String role;
    private boolean active;

    public Staff() {}

    public Staff(String uid, String email, String role, boolean active) {
        this.uid = uid;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}