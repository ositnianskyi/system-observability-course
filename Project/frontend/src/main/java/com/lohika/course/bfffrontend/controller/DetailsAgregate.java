package com.lohika.course.bfffrontend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/details")
public class DetailsAgregate {

    @Value("${books.url}")
    private String booksUrl;

    @Value("${authors.url}")
    private String authorsUrl;

    @GetMapping
    public Mono<Map> getBooksAndAuthors() {
        WebClient authorClient = WebClient
                .builder()
                .baseUrl(authorsUrl)
                .build();

        WebClient bookClient = WebClient
                .builder()
                .baseUrl(booksUrl)
                .build();

        Mono<Object> authors = authorClient.get().retrieve().bodyToMono(Object.class);
        Mono<Object> books = bookClient.get().retrieve().bodyToMono(Object.class);
        return authors.zipWith(books).map(t -> {
            Map<String, Object> result = new HashMap<>();
            result.put("authors", t.getT1());
            result.put("books", t.getT2());
            return result;
        });
    }
}
