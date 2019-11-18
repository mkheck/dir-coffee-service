package com.thehecklers.coffeeservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(CoffeeService.class)
class InternalAPITests {
    @Autowired
    private CoffeeService service;

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
    void getAllCoffees() {
        StepVerifier.withVirtualTime(()->service.getAllCoffees())
                .expectNext(coffee1)
                .expectNext(coffee2)
                .verifyComplete();
    }

    @Test
    void getCoffeeById() {
        StepVerifier.withVirtualTime(()->service.getCoffeeById(coffee1.getId()))
                .expectNext(coffee1)
                .verifyComplete();
    }

    @Test
    void getOrdersForCoffee() {
        StepVerifier.withVirtualTime(()->service.getOrdersForCoffee(coffee2.getId()).take(10))
                .thenAwait(Duration.ofHours(10))
                .expectNextCount(10)
                .verifyComplete();
    }
}