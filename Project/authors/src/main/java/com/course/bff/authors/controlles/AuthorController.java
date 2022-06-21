package com.course.bff.authors.controlles;

import com.course.bff.authors.models.Author;
import com.course.bff.authors.requests.CreateAuthorCommand;
import com.course.bff.authors.responses.AuthorResponse;
import com.course.bff.authors.services.AuthorService;
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
@RequestMapping("api/v1/authors")
public class AuthorController {

    @Value("${redis.topic}")
    private String redisTopic;

    private final static Logger logger = LoggerFactory.getLogger(AuthorController.class);
    private final AuthorService authorService;
    private final RedisTemplate<String, Object> redisTemplate;
    private MeterRegistry meterRegistry;

    private Counter request_count;
    private Counter error_count;
    private LongTaskTimer execution_duration;

    public AuthorController(AuthorService authorService, RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.authorService = authorService;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        this.request_count = meterRegistry.counter("request_count", "ControllerName", AuthorController.class.getSimpleName());
        this.error_count = meterRegistry.counter("error_count", "ControllerName", AuthorController.class.getSimpleName());
        this.execution_duration = LongTaskTimer
                .builder("execution_duration")
                .tags("ControllerName", AuthorController.class.getSimpleName())
                .register(meterRegistry);

    }

    @GetMapping()
    public Collection<AuthorResponse> getAuthors() {
        logger.info("Get authors");
        request_count.increment();
        final LongTaskTimer.Sample task = execution_duration.start();
        List<AuthorResponse> authorResponses = new ArrayList<>();
        this.authorService.getAuthors().forEach(author -> {
            AuthorResponse authorResponse = createAuthorResponse(author);
            authorResponses.add(authorResponse);
        });
        task.stop();

        return authorResponses;
    }

    @GetMapping("/{id}")
    public AuthorResponse getById(@PathVariable UUID id) {
        logger.info(String.format("Find authors by %s", id));
        request_count.increment();
        final LongTaskTimer.Sample task = execution_duration.start();
        Optional<Author> authorSearch = this.authorService.findById(id);
        if (authorSearch.isEmpty()) {
            task.stop();
            error_count.increment();
            throw new RuntimeException("Author isn't found");
        }
        task.stop();

        return createAuthorResponse(authorSearch.get());
    }

    @PostMapping()
    public AuthorResponse createAuthors(@RequestBody CreateAuthorCommand createAuthorCommand) {
        logger.info("Create authors");
        request_count.increment();
        final LongTaskTimer.Sample task = execution_duration.start();
        Author author = this.authorService.create(createAuthorCommand);
        AuthorResponse authorResponse = createAuthorResponse(author);
        this.sendPushNotification(authorResponse);
        task.stop();
        return authorResponse;
    }


    private void sendPushNotification(AuthorResponse authorResponse) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            redisTemplate.convertAndSend(redisTopic, gson.toJson(authorResponse));
        } catch (Exception e) {
            error_count.increment();
            logger.error("Push Notification Error", e);
        }
    }

    private AuthorResponse createAuthorResponse(Author author) {
        AuthorResponse authorResponse = new AuthorResponse();
        authorResponse.setId(author.getId());
        authorResponse.setFirstName(author.getFirstName());
        authorResponse.setLastName(author.getLastName());
        authorResponse.setAddress(author.getAddress());
        authorResponse.setLanguage(author.getLanguage());
        return authorResponse;
    }
}
