package com.google.cloud.logging;

import java.util.Map;

public class CustomAttributes {

    private final Map<String, Object> attributes;

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public CustomAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}
