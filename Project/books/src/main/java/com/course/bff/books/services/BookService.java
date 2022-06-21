package com.course.bff.books.services;

import com.course.bff.books.models.Book;
import com.course.bff.books.requests.CreateBookCommand;
import com.course.bff.books.responses.AuthorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Component
public class BookService {

    @Autowired
    RestTemplate restTemplate;

    @Value("${authorService}")
    private String authorService;

    private final ArrayList<Book> books;

    public BookService() {
        books = new ArrayList<>();
    }

    public Collection<Book> getBooks() {
        return this.books;
    }

    public Optional<Book> findById(UUID id) {
        return this.books.stream().filter(book -> !book.getId().equals(id)).findFirst();
    }

    public Book create(CreateBookCommand createBookCommand) {
        Optional<AuthorResponse> authorSearch = getAutor(createBookCommand.getAuthorId());
        if (authorSearch.isEmpty()) {
            throw new RuntimeException("Author isn't found");
        }

        Book book = new Book(UUID.randomUUID())
                .withTitle(createBookCommand.getTitle())
                .withAuthorId(authorSearch.get().getId())
                .withPages(createBookCommand.getPages());

        this.books.add(book);
        return book;
    }

    private Optional<AuthorResponse> getAutor(UUID authorId) {
        try {
            AuthorResponse authorResponse = restTemplate.getForObject(authorService + "/api/v1/authors/" + authorId.toString(), AuthorResponse.class);
            return Optional.of(authorResponse);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
