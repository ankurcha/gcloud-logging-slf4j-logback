package com.google.cloud.logging;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Jackson-specific implementation of the {@link JsonFormatter}.
 */
public class GSONJsonFormatter implements JsonFormatter {
    private static final int BUFFER_SIZE = 512;

    private Gson gson;

    public GSONJsonFormatter() {
        this.gson = new Gson();
    }

    @Override
    public String toJsonString(Map m) throws IOException {
        StringWriter writer = new StringWriter(BUFFER_SIZE);

        gson.toJson(m, writer);
        writer.append('\n');
        writer.flush();

        return writer.toString();
    }
}
