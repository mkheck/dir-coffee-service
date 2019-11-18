package com.thehecklers.coffeeservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

//@WebFluxTest({CoffeeService.class, CoffeeController.class})
@WebFluxTest({CoffeeService.class, RouteConfig.class})
class ExternalAPITests {
    @Autowired
    private WebTestClient client;

    @MockBean
    private CoffeeRepository repo;

    Coffee coffee1, coffee2;

    @BeforeEach
    void setUp() {
        coffee1 = new Coffee("000-TEST-111", "Tester's Choice");
        coffee2 = new Coffee("000-TEST-222", "Maxfail House");

        Mockito.when(repo.findAll()).thenReturn(Flux.just(coffee1, coffee2));
        Mockito.when(repo.findById(coffee1.getId())).thenReturn(Mono.just(coffee1));
        Mockito.when(repo.findById(coffee2.getId())).thenReturn(Mono.just(coffee2));
    }

    @Test
    void all() {
        StepVerifier.create(client.get()
                .uri("/coffees")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .returnResult(Coffee.class)
                .getResponseBody())
                .expectNext(coffee1)
                .expectNext(coffee2)
                .verifyComplete();
    }

    @Test
    void byId() {
        StepVerifier.create(client.get()
                .uri("/coffees/{id}", coffee2.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .returnResult(Coffee.class)
                .getResponseBody())
                .expectNext(coffee2)
                .verifyComplete();
    }

    @Test
    void orders() {
        StepVerifier.create(client.get()
                .uri("/coffees/{id}/orders", coffee1.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
                .returnResult(CoffeeOrder.class)
                .getResponseBody()
                .take(2))
                .expectNextCount(2)
                .verifyComplete();
    }
}