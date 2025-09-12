package com.sanddollar.controller;

import com.sanddollar.service.SeedDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dev")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
@Profile({"dev", "local"}) // Only available in development
public class DevController {

    @Autowired
    private SeedDataService seedDataService;

    @PostMapping("/seed")
    public ResponseEntity<?> createSeedData() {
        try {
            seedDataService.createSeedData();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Seed data created successfully",
                "demoEmail", "demo@sanddollar.app",
                "demoPassword", "Demo123!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to create seed data: " + e.getMessage()));
        }
    }
}