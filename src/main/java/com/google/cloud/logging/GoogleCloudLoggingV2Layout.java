/**
 * Copyright (C) 2016 - Ankur Chauhan <ankur@malloc64.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.JsonLayoutBase;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Google cloud logging v2 json layout
 */
public class GoogleCloudLoggingV2Layout extends JsonLayoutBase<ILoggingEvent> {
    private static final String TRACE_ID_FIELD_KEY        = "logging.googleapis.com/trace";
    private static final String SPAN_ID_FIELD_KEY         = "logging.googleapis.com/spanId";
    private static final String SOURCE_LOCATION_FIELD_KEY = "logging.googleapis.com/sourceLocation";

    private final ThrowableProxyConverter tpc;
    private String serviceName;
    private String serviceVersion;
    private boolean addTraceFields;
    private boolean addHttpRequestFields;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public boolean getAddTraceFields() {
        return addTraceFields;
    }

    public void setAddTraceFields(boolean addTraceFields) {
        this.addTraceFields = addTraceFields;
    }

    public boolean getAddHttpRequestFields() {
        return addHttpRequestFields;
    }

    public void setAddHttpRequestFields(boolean addHttpRequestFields) {
        this.addHttpRequestFields = addHttpRequestFields;
    }

    public GoogleCloudLoggingV2Layout() {
        this("default", "default", true, true);
    }

    public GoogleCloudLoggingV2Layout(String serviceName, String serviceVersion, boolean addTraceFields, boolean addHttpRequestFields) {
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        this.addTraceFields = addTraceFields;
        this.addHttpRequestFields = addHttpRequestFields;
        tpc = new ThrowableProxyConverter();
        tpc.setOptionList(Collections.singletonList("full"));
    }

    @Override
    public void start() {
        tpc.start();
        super.start();
    }

    @Override
    public void stop() {
        tpc.stop();
        super.stop();
    }

    @Override
    protected Map toJsonMap(ILoggingEvent event) {
        Map<Object, Object> builder = new HashMap<>(2);
        builder.put("severity", getSeverity(event));
        builder.put("timestamp", getTime(event));

        // message fields
        builder.put("serviceContext", getServiceContext());
        builder.put("message", getMessage(event));

        // add trace fields if it is present as one of the arguments
        if (addTraceFields || addHttpRequestFields) {
            TraceContext traceCtx = null;
            HttpRequestContext reqCtx = null;

            for (Object arg : event.getArgumentArray()) {
                if (arg instanceof TraceContext) {
                    traceCtx = (TraceContext) arg;
                }

                if (arg instanceof HttpRequestContext) {
                    reqCtx = (HttpRequestContext) arg;
                }
            }

            // add trace context if present
            if (traceCtx != null) {
                if (!isNullOrEmpty(traceCtx.getTraceId())) {
                    builder.put(TRACE_ID_FIELD_KEY, traceCtx.getTraceId());
                }

                if (!isNullOrEmpty(traceCtx.getSpanId())) {
                    builder.put(SPAN_ID_FIELD_KEY, traceCtx.getSpanId());
                }
            }

            // add http request if present
            if (reqCtx != null) {
                builder.put("httpRequest", reqCtx.getFields());
            }
        }

        if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            builder.put("details", event.getMDCPropertyMap());
        }

        Map<String, Object> sourceLocation = getSourceLocation(event);
        if (sourceLocation != null && !sourceLocation.isEmpty()) {
            builder.put(SOURCE_LOCATION_FIELD_KEY, sourceLocation);
        }

        builder.put("thread", event.getThreadName());
        builder.put("logger", event.getLoggerName());

        return builder;
    }

    private Map<String, String> _serviceContext;

    Map<String, String> getServiceContext() {
        if (_serviceContext == null) {
            Map<String, String> serviceContext = new HashMap<>(2);
            serviceContext.put("service", serviceName);
            serviceContext.put("version", serviceVersion);
            _serviceContext = serviceContext;
        }
        return this._serviceContext;
    }

    String getMessage(ILoggingEvent event) {
        String message = event.getFormattedMessage();

        // add exception if there is one
        String stackTrace = tpc.convert(event);
        if (!isNullOrEmpty(stackTrace)) {
            return message + "\n" + stackTrace;
        }
        return message;
    }

    static Map<String, Object> getSourceLocation(ILoggingEvent event) {
        StackTraceElement[] cda = event.getCallerData();
        Map<String, Object> sourceLocation = new HashMap<>();
        if (cda != null && cda.length > 0) {
            StackTraceElement ste = cda[0];

            sourceLocation.put("function", ste.getClassName() + "." + ste.getMethodName() + (ste.isNativeMethod() ? "(Native Method)" : ""));
            if (ste.getFileName() != null) {
                String pkg = ste.getClassName().replaceAll("\\.", "/");
                pkg = pkg.substring(0, pkg.lastIndexOf("/") + 1);
                sourceLocation.put("file", pkg + ste.getFileName());
            }
            sourceLocation.put("line", ste.getLineNumber());
        } else {
            sourceLocation.put("file", CallerData.NA);
            sourceLocation.put("line", CallerData.LINE_NA);
            sourceLocation.put("function", CallerData.NA);
        }
        return sourceLocation;
    }

    static Map<String, Object> getTime(ILoggingEvent event) {
        Map<String, Object> time = new HashMap<>();
        Instant ts = Instant.ofEpochMilli(event.getTimeStamp());
        time.put("seconds", ts.getEpochSecond());
        time.put("nanos", ts.getNano());
        return time;
    }

    private static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }


    static String getSeverity(final ILoggingEvent event) {
        Level level = event.getLevel();
        if (level == Level.ALL) return "DEBUG";
        else if (level == Level.TRACE) return "DEBUG";
        else if (level == Level.DEBUG) return "DEBUG";
        else if (level == Level.INFO) return "INFO";
        else if (level == Level.WARN) return "WARNING";
        else if (level == Level.ERROR) return "ERROR";
        else return "DEFAULT";
    }
}
