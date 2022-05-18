package com.course.bff.books.models;

import java.util.UUID;

public class Book {
    private UUID id;
    private String title;
    private int pages;
    private UUID authorId;

    public Book(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID autorId) {
        this.authorId = autorId;
    }

    public Book withTitle(String title) {
        this.setTitle(title);
        return this;
    }

    public Book withAuthorId(UUID author) {
        this.setAuthorId(author);
        return this;
    }

    public Book withPages(int pages) {
        this.setPages(pages);
        return this;
    }
}
