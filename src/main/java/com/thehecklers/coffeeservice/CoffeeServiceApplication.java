package com.thehecklers.coffeeservice;

import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;

@SpringBootApplication
public class CoffeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeServiceApplication.class, args);
    }

}

@Component
@AllArgsConstructor
class DataLoader {
    private final CoffeeRepository repo;

    @PostConstruct
    void loadData() {
        repo.deleteAll().thenMany(
                Flux.just("Café Cereza", "Don Pablo", "Espresso Roast", "Juan Valdez", "Kaldi's Coffee", "Kona")
                        .map(Coffee::new)
                        .flatMap(repo::save))
                .thenMany(repo.findAll())
                .subscribe(System.out::println);
    }
}

@Controller
@AllArgsConstructor
class RSController {
    private final CoffeeService service;

    @MessageMapping("coffees")
    Flux<Coffee> all() {
        return service.getAllCoffees();
    }

    @MessageMapping("orders.{coffeename}")
    Flux<CoffeeOrder> orders(@DestinationVariable String coffeename) {
        return service.getCoffeeByName(coffeename)
                .flatMapMany(coffee -> service.getOrdersForCoffee(coffee.getId()));
    }
}

@Configuration
@AllArgsConstructor
class RouteConfig {
    private final CoffeeService service;

    @Bean
    RouterFunction<ServerResponse> routerFunction() {
        return route(GET("/coffees"), this::all)
                .andRoute(GET("/coffees/{id}"), this::byId)
                .andRoute(GET("/coffees/{id}/orders"), this::orders);
    }

    public Mono<ServerResponse> all(ServerRequest req) {
        return ServerResponse.ok()
                .body(service.getAllCoffees(), Coffee.class);
    }

    public Mono<ServerResponse> byId(ServerRequest req) {
        return ServerResponse.ok()
                .body(service.getCoffeeById(req.pathVariable("id")), Coffee.class);
    }

    public Mono<ServerResponse> orders(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.getOrdersForCoffee(req.pathVariable("id")), CoffeeOrder.class);
    }
}

/*@RestController
@RequestMapping("/coffees")
@AllArgsConstructor
class CoffeeController {
    private final CoffeeService service;

    @GetMapping
    Flux<Coffee> all() {
        return service.getAllCoffees();
    }

    @GetMapping("/{id}")
    Mono<Coffee> byId(@PathVariable String id) {
        return service.getCoffeeById(id);
    }

    @GetMapping(value = "/{id}/orders", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<CoffeeOrder> orders(@PathVariable String id) {
        return service.getOrdersForCoffee(id);
    }
}*/

@Service
@AllArgsConstructor
class CoffeeService {
    private final CoffeeRepository repo;

    Flux<Coffee> getAllCoffees() {
        return repo.findAll();
    }

    Mono<Coffee> getCoffeeById(String id) {
        return repo.findById(id);
    }

    Mono<Coffee> getCoffeeByName(String name) {
        return repo.findCoffeeByName(name);
                //.defaultIfEmpty(new Coffee("12345", "My favorite coffee"));
    }

    Flux<CoffeeOrder> getOrdersForCoffee(String coffeeId) {
        return Flux.interval(Duration.ofSeconds(1))
                .onBackpressureDrop()
                .map(l -> new CoffeeOrder(coffeeId, Instant.now()));
    }
}

interface CoffeeRepository extends ReactiveCrudRepository<Coffee, String> {
    Mono<Coffee> findCoffeeByName(String name);
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CoffeeOrder {
    private String coffeeId;
    private Instant whenOrdered;
}

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
class Coffee {
    @Id
    private String id;
    @NonNull
    private String name;
}