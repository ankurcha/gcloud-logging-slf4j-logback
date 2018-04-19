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
            put("requestUrl", getRequestUrl(request));
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

    /**
     * <p>A faster replacement for {@link HttpServletRequest#getRequestURL()}
     * 	(returns a {@link String} instead of a {@link StringBuffer} - and internally uses a {@link StringBuilder})
     * 	that also includes the {@linkplain HttpServletRequest#getQueryString() query string}.</p>
     * <p><a href="https://gist.github.com/ziesemer/700376d8da8c60585438">https://gist.github.com/ziesemer/700376d8da8c60585438</a></p>
     * @author Mark A. Ziesemer
     * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
     */
    private static String getRequestUrl(final HttpServletRequest req) {
        final String scheme = req.getScheme();
        final int port = req.getServerPort();
        final StringBuilder url = new StringBuilder(256);
        url.append(scheme);
        url.append("://");
        url.append(req.getServerName());
        if (!(("http".equals(scheme) && (port == 0 || port == 80)) || ("https".equals(scheme) && port == 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(req.getRequestURI());
        final String qs = req.getQueryString();
        if (qs != null) {
            url.append('?');
            url.append(qs);
        }
        return url.toString();
    }
}
