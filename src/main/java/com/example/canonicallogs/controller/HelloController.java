package com.example.canonicallogs.controller;

import com.example.canonicallogs.logging.CanonicalLogContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final CanonicalLogContext logCtx;

    public HelloController(CanonicalLogContext logCtx) {
        this.logCtx = logCtx;
    }

    @GetMapping("/")
    public String hello() {
        logCtx.put("endpoint", "hello");
        return "Hello World!";
    }
}
