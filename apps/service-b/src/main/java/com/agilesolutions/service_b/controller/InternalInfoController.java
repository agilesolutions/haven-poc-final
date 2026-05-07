package com.agilesolutions.service_b.controller;

import com.agilesolutions.service_b.model.EntityInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
public class InternalInfoController {

    @GetMapping("/info/{id}")
    public ResponseEntity<EntityInfo> getInternalInfo(@PathVariable String id) {
        EntityInfo e = new EntityInfo();
        e.setId(id);
        e.setName("Entity " + id);
        e.setDescription("This is a placeholder entity for id " + id);
        e.setVersion("1.0.0");
        return ResponseEntity.ok(e);
    }
}

