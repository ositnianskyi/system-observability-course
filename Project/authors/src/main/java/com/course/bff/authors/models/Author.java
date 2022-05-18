package com.course.bff.authors.models;

import java.util.UUID;

public class Author {
    private UUID id;
    private String firstName;
    private String lastName;
    private String address;
    private String language;

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Author withId(UUID id) {
        this.setId(id);
        return this;
    }

    public Author withLanguage(String language) {
        this.setLanguage(language);
        return this;
    }

    public Author withAddres(String addres) {
        this.setAddress(addres);
        return this;
    }

    public Author withFirstName(String firstName) {
        this.setFirstName(firstName);
        return this;
    }

    public Author withLastName(String lastName) {
        this.setLastName(lastName);
        return this;
    }
}
