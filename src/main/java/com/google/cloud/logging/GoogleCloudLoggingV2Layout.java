/**
 * Copyright (C) 2016 - Ankur Chauhan <ankur@malloc64.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
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
    private static final String TRACE_ID_FIELD_KEY = "logging.googleapis.com/trace";
    private static final String SPAN_ID_FIELD_KEY = "logging.googleapis.com/spanId";

    private final ThrowableProxyConverter tpc;
    private String serviceName;
    private String serviceVersion;
    private boolean addTraceFields;

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

    public boolean isAddTraceFields() {
        return addTraceFields;
    }

    public void setAddTraceFields(boolean addTraceFields) {
        this.addTraceFields = addTraceFields;
    }

    public GoogleCloudLoggingV2Layout() {
        this("default", "default", true);
    }

    public GoogleCloudLoggingV2Layout(String serviceName, String serviceVersion, boolean addTraceFields) {
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        this.addTraceFields = addTraceFields;
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
        if (addTraceFields) {
            TraceContext tCtx = null;
            for (Object arg : event.getArgumentArray()) {
                if (arg instanceof TraceContext) {
                    tCtx = (TraceContext) arg;
                    break;
                }
            }

            if (tCtx != null) {
                // add trace fields
                if (!isNullOrEmpty(tCtx.getTraceId())) {
                    builder.put(TRACE_ID_FIELD_KEY, tCtx.getTraceId());
                }

                if (!isNullOrEmpty(tCtx.getSpanId())) {
                    builder.put(SPAN_ID_FIELD_KEY, tCtx.getSpanId());
                }
            }
        }

        Map<String, Object> context = getContext(event);
        if (!context.isEmpty()) {
            builder.put("context", context);
        }
        return builder;
    }

    private Map<String, String> _serviceContext;
    Map<String, String> getServiceContext() {
        if(_serviceContext == null) {
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

    static Map<String, Object> getContext(ILoggingEvent event) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> reportLocation = getReportLocation(event);
        if (!reportLocation.isEmpty()) {
            context.put("reportLocation", reportLocation);
        }
        return context;
    }

    static Map<String, Object> getReportLocation(ILoggingEvent event) {
        Map<String, Object> reportLocation = new HashMap<>();
        StackTraceElement callerData = event.getCallerData()[0];
        if (callerData != null) {
            reportLocation.put("filePath", callerData.getClassName().replace('.', '/') + ".class");
            reportLocation.put("lineNumber", callerData.getLineNumber());
            reportLocation.put("functionName", callerData.getClassName() + "." + callerData.getMethodName());
        }
        reportLocation.put("thread", event.getThreadName());
        reportLocation.put("logger", event.getLoggerName());
        return reportLocation;
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
        if (level == Level.ALL)        return "DEBUG";
        else if (level == Level.TRACE) return "DEBUG";
        else if (level == Level.DEBUG) return "DEBUG";
        else if (level == Level.INFO)  return "INFO";
        else if (level == Level.WARN)  return "WARNING";
        else if (level == Level.ERROR) return "ERROR";
        else return "DEFAULT";
    }
}
