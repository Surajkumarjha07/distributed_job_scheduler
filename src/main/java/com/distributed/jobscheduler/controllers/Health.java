package com.distributed.jobscheduler.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Health {
    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.status(HttpStatus.OK).body("Job scheduler is working properly!");
    }
}
