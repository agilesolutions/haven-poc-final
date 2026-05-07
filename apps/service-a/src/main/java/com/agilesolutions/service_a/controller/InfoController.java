package com.agilesolutions.service_a.controller;

import com.agilesolutions.service_a.model.EntityInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class InfoController {

    private final RestTemplate restTemplate;

    @Value("${service.b.base-url:http://localhost:8081}")
    private String serviceBBaseUrl;

    public InfoController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<EntityInfo> getInfo(@PathVariable String id) {
        String url = serviceBBaseUrl + "/api/internal/info/" + id;
        EntityInfo info = restTemplate.getForObject(url, EntityInfo.class);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }
}

