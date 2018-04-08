package com.google.cloud.logging;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequestContext {
    private final Map<String, Object> fields;

    public HttpRequestContext() {
        this(new LinkedHashMap<>());
    }

    public HttpRequestContext(Map<String, Object> fields) {
        this.fields = fields;
    }

    public HttpRequestContext(HttpServletRequest request, HttpServletResponse response) {
        this();
        if (request != null) {
            put("requestMethod", request.getMethod());
            put("requestUrl", request.getRequestURL().toString());
            put("requestSize", String.valueOf(request.getContentLengthLong()));
            put("userAgent", request.getHeader("user-agent"));
            put("remoteIp", request.getHeader("X-Forwarded-For"));
            put("referer", request.getHeader("Referer"));
            put("protocol", request.getProtocol());
        }

        if (response != null) {
            put("status", String.valueOf(response.getStatus()));
        }
    }

    public void setLatency(double latencySeconds) {
        put("latency", String.valueOf(latencySeconds) + "s");
    }

    public void setCacheLookup(boolean cacheLookup) {
        put("cacheLookup", cacheLookup);
    }

    public void setCacheHit(boolean cacheHit) {
        put("cacheHit", cacheHit);
    }

    public void setCacheValidatedWithOriginServer(boolean cacheValidatedWithOriginServer) {
        put("cacheValidatedWithOriginServer", cacheValidatedWithOriginServer);
    }

    public void setCacheFillBytes(long cacheFillBytes) {
        put("cacheFillBytes", String.valueOf(cacheFillBytes));
    }

    public Object put(String key, Object value) {
        return fields.put(key, value);
    }

    public Map<String, Object> getFields() {
        return fields;
    }
}
