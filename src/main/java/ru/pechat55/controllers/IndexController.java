package ru.pechat55.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
public class IndexController {


    @GetMapping()
    private Mono<String> index() {
        return Mono.just("Image painting backend is working");
    }
}
