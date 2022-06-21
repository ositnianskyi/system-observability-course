package com.course.bff.books.controlles;

import brave.Span;
import brave.Tracer;
import com.course.bff.books.models.Book;
import com.course.bff.books.requests.CreateBookCommand;
import com.course.bff.books.responses.BookResponse;
import com.course.bff.books.services.BookService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.util.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("api/v1/books")
public class BookController {

    @Autowired
    private Tracer tracer;

    private final static Logger logger = LoggerFactory.getLogger(BookController.class);
    private final BookService bookService;
    private final RedisTemplate<String, Object> redisTemplate;
    private MeterRegistry meterRegistry;

    private Counter request_count;
    private Counter error_count;
    private LongTaskTimer execution_duration;

    @Value("${redis.topic}")
    private String redisTopic;

    public BookController(BookService bookService, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.bookService = bookService;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        this.request_count = meterRegistry.counter("request_count", "ControllerName", BookController.class.getSimpleName());
        this.error_count = meterRegistry.counter("error_count", "ControllerName", BookController.class.getSimpleName());
        this.execution_duration = LongTaskTimer
                .builder("execution_duration")
                .tags("ControllerName", BookController.class.getSimpleName())
                .register(meterRegistry);

    }

    @GetMapping()
    public Collection<BookResponse> getBooks() {
        logger.info("Get book list");
        request_count.increment();
        final LongTaskTimer.Sample task = execution_duration.start();
        List<BookResponse> bookResponses = new ArrayList<>();
        this.bookService.getBooks().forEach(book -> {
            BookResponse bookResponse = createBookResponse(book);
            bookResponses.add(bookResponse);
        });

        task.stop();
        return bookResponses;
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable UUID id) {
        logger.info(String.format("Find book by id %s", id));
        request_count.increment();
        final LongTaskTimer.Sample task = execution_duration.start();
        Optional<Book> bookSearch = this.bookService.findById(id);
        if (bookSearch.isEmpty()) {
            task.stop();
            error_count.increment();
            throw new RuntimeException("Book isn't found");
        }
        task.stop();

        return createBookResponse(bookSearch.get());
    }

    @PostMapping()
    public BookResponse createBooks(@RequestBody CreateBookCommand createBookCommand) {
        logger.info("Create books");
        request_count.increment();
        final LongTaskTimer.Sample task = execution_duration.start();
        Book book = this.bookService.create(createBookCommand);
        BookResponse authorResponse = createBookResponse(book);
        this.sendPushNotification(authorResponse);
        task.stop();
        return authorResponse;
    }

    private void sendPushNotification(BookResponse bookResponse) {
        Span redisSpan = tracer.nextSpan().name("redisPushNotification");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Tracer.SpanInScope scope = tracer.withSpanInScope(redisSpan.start())) {
            redisTemplate.convertAndSend(redisTopic, gson.toJson(bookResponse));
        } catch (Exception e) {
            logger.error("Push Notification Error", e);
            error_count.increment();
        } finally {
            redisSpan.finish();
        }
    }

    private BookResponse createBookResponse(Book book) {
        BookResponse bookResponse = new BookResponse();
        bookResponse.setId(book.getId());
        bookResponse.setAuthorId(book.getAuthorId());
        bookResponse.setPages(book.getPages());
        bookResponse.setTitle(book.getTitle());
        return bookResponse;
    }
}
