package com.google.cloud.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.JsonLayoutBase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Google cloud logging v2 json layout
 */
public class GoogleCloudLoggingV2Layout extends JsonLayoutBase<ILoggingEvent> {
    private static final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    private String serviceName;
    private String serviceVersion;

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

    public GoogleCloudLoggingV2Layout() {
        this("default", "default");
    }

    public GoogleCloudLoggingV2Layout(String serviceName, String serviceVersion) {
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
    }

    @Override
    protected Map toJsonMap(ILoggingEvent event) {
        Map<Object, Object> builder = new HashMap<>(1);
        builder.put("log", buildLog(event));
        return builder;
    }


    Map<String, Object> buildLog(ILoggingEvent event) {
        Map<String, Object> log = new HashMap<>();
        log.put("time", getTime(event));
        log.put("severity", getSeverity(event));

        // add the rest of the fields for the json payload
        log.put("serviceContext", getServiceContext());
        log.put("message", getMessage(event));
        Map<String, Object> context = getContext(event);
        if (!context.isEmpty()) {
            log.put("context", context);
        }
        return log;
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

    static String getMessage(ILoggingEvent event) {
        String message = event.getFormattedMessage();

        // add exception if there is one
        String stackTrace = throwableProxyConverter.convert(event);
        if (!isNullOrEmpty(stackTrace)) {
            return message + "\n" + throwableProxyConverter.convert(event);
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
