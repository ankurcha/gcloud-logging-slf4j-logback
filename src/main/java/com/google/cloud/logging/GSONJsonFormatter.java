package com.google.cloud.logging;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson-specific implementation of the {@link JsonFormatter}.
 */
public class GSONJsonFormatter implements JsonFormatter {
    private Gson gson;

    public GSONJsonFormatter() {
        this.gson = new Gson();
    }

    @Override
    public String toJsonString(Map m) throws IOException {
        return gson.toJson(m);
    }
}
