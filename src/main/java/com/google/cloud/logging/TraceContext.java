package com.google.cloud.logging;

public class TraceContext {
    private final String traceId;
    private final String spanId;

    public TraceContext(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }
}
