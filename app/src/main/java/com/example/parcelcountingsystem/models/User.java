package com.example.parcelcountingsystem.models;

public class User {
    public String name;
    public String password;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email) {
        this.name = name;
        this.password = email;
    }
}

