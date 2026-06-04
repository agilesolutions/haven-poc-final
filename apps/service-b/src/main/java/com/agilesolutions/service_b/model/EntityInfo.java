package com.agilesolutions.service_b.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntityInfo {
    private String id;
    private String name;
    private String description;
    private String version;
}

