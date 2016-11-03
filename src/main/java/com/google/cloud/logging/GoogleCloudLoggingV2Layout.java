package com.google.cloud.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.JsonLayoutBase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Usage instructions:
 * <appender name="gcloud-appender" class="ch.qos.logback.core.ConsoleAppender">
 *     <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
 *         <layout class="com.google.cloud.logging.GoogleCloudLoggingV2Layout">
 *             <jsonFormatter class="com.google.cloud.logging.GSONJsonFormatter"/>
 *         </layout>
 *     </encoder>
 *  </appender>
 */
public class GoogleCloudLoggingV2Layout extends JsonLayoutBase<ILoggingEvent> {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = ISODateTimeFormat.dateTime();
    private static final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

    @Override
    protected Map toJsonMap(ILoggingEvent event) {
        String severity = toCloudLoggingLevel(event.getLevel());
        String time = new DateTime(event.getTimeStamp()).toString(TIMESTAMP_FORMATTER);
        Map<Object, Object> builder = new HashMap<>();
        // for sure add caller data
        StackTraceElement callerData = event.getCallerData()[0];

        // stack trace
        String stackTrace = throwableProxyConverter.convert(event);
        Map<Object, Object> payloadBuilder = new HashMap<>();
        payloadBuilder.put("thread", event.getThreadName());
        payloadBuilder.put("logger", event.getLoggerName());
        if (!isNullOrEmpty(stackTrace)) {
            payloadBuilder.put("stackTrace", stackTrace);
        }
        String functionName = callerData.getClassName() + "." + callerData.getMethodName();
        Map<String, String> sourceLocation = new HashMap<>();
        sourceLocation.put("file", callerData.getFileName());
        sourceLocation.put("line", String.valueOf(callerData.getLineNumber()));
        sourceLocation.put("functionName", functionName);

        builder.put("severity",         severity);
        builder.put("time",             time);
        builder.put("message",          event.getFormattedMessage());
        builder.put("sourceLocation",   sourceLocation);
        builder.put("jsonPayload",      payloadBuilder);

        return builder;
    }

    private static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }


    private static String toCloudLoggingLevel(final Level level) {
        if (level == Level.ALL)        return "DEBUG";
        else if (level == Level.TRACE) return "DEBUG";
        else if (level == Level.DEBUG) return "DEBUG";
        else if (level == Level.INFO)  return "INFO";
        else if (level == Level.WARN)  return "WARNING";
        else if (level == Level.ERROR) return "ERROR";
        else return "DEFAULT";
    }
}
