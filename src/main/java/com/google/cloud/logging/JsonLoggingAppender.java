/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.util.Loader;
import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.Logging.WriteOption;

import java.util.*;

public class JsonLoggingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private volatile Logging logging;
    private List<LoggingEnhancer> loggingEnhancers;
    private WriteOption[] defaultWriteOptions;

    private Level flushLevel;
    private String log;
    private String resourceType;
    private Set<String> enhancerClassNames = new HashSet<>();
    private ThrowableProxyConverter throwableProxyConverter;

    /**
     * Batched logging requests get immediately flushed for logs at or above this level.
     *
     * Defaults to Error if not set.
     *
     * @param flushLevel Logback log level
     */
    public void setFlushLevel(Level flushLevel) {
        this.flushLevel = flushLevel;
    }

    /**
     * Sets the log filename.
     *
     * @param log filename
     */
    public void setLog(String log) {
        this.log = log;
    }

    /**
     * Sets the name of the monitored resource (Optional).
     *
     * Must be a <a href="https://cloud.google.com/logging/docs/api/v2/resource-list">supported</a>
     * resource type. gae_app, gce_instance and container are auto-detected.
     *
     * Defaults to "global"
     *
     * @param resourceType name of the monitored resource
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Add extra labels using classes that implement {@link LoggingEnhancer}.
     */
    public void addEnhancer(String enhancerClassName) {
        this.enhancerClassNames.add(enhancerClassName);
    }

    Level getFlushLevel() {
        return (flushLevel != null) ? flushLevel : Level.ERROR;
    }

    String getLogName() {
        return (log != null) ? log : "java.log";
    }

    MonitoredResource getMonitoredResource(String projectId) {
        return MonitoredResourceUtil.getResource(projectId, resourceType);
    }

    List<LoggingEnhancer> getLoggingEnhancers() {
        List<LoggingEnhancer> loggingEnhancers = new ArrayList<>();
        if (enhancerClassNames != null) {
            for (String enhancerClassName : enhancerClassNames) {
                if (enhancerClassName != null) {
                    LoggingEnhancer enhancer = getEnhancer(enhancerClassName);
                    if (enhancer != null) {
                        loggingEnhancers.add(enhancer);
                    }
                }
            }
        }
        return loggingEnhancers;
    }

    private LoggingEnhancer getEnhancer(String enhancerClassName) {
        try {
            Class<? extends LoggingEnhancer> clz = (Class<? extends LoggingEnhancer>) Loader.loadClass(enhancerClassName.trim());
            return clz.newInstance();
        } catch (Exception ex) {
            // If we cannot create the enhancer we fallback to null
        }
        return null;
    }

    /**
     * Initialize and configure the cloud logging service.
     */
    @Override
    public synchronized void start() {
        if (isStarted()) {
            return;
        }
        MonitoredResource resource = getMonitoredResource(getProjectId());
        defaultWriteOptions = new WriteOption[]{
                WriteOption.logName(getLogName()),
                WriteOption.resource(resource)
        };
        getLogging().setFlushSeverity(severityFor(getFlushLevel()));
        loggingEnhancers = new ArrayList<>();
        loggingEnhancers.addAll(MonitoredResourceUtil.getResourceEnhancers());
        loggingEnhancers.addAll(getLoggingEnhancers());
        throwableProxyConverter = new ThrowableProxyConverter();
        super.start();
    }

    String getProjectId() {
        return LoggingOptions.getDefaultInstance().getProjectId();
    }

    @Override
    protected void append(ILoggingEvent e) {
        LogEntry logEntry = logEntryFor(e);
        getLogging().write(Collections.singleton(logEntry), defaultWriteOptions);
    }

    @Override
    public synchronized void stop() {
        if (logging != null) {
            try {
                logging.close();
            } catch (Exception ex) {
                // ignore
            }
        }
        logging = null;
        super.stop();
    }

    Logging getLogging() {
        if (logging == null) {
            synchronized (this) {
                if (logging == null) {
                    logging = LoggingOptions.getDefaultInstance().getService();
                }
            }
        }
        return logging;
    }

    private LogEntry logEntryFor(ILoggingEvent e) {
        Level level = e.getLevel();
        Payload.JsonPayload payload = toJsonPayload(e);

        LogEntry.Builder builder = LogEntry.newBuilder(payload)
                .setTimestamp(e.getTimeStamp())
                .setSeverity(severityFor(level));

        // labels
        builder.addLabel("thread", e.getThreadName());
        builder.addLabel("logger", e.getLoggerName());

        // mdc is always important (if present)
        if (e.getMDCPropertyMap() != null && !e.getMDCPropertyMap().isEmpty()) {
            e.getMDCPropertyMap().forEach(builder::addLabel);
        }

        if (e.getLoggerContextVO() != null) {
            builder.addLabel("context", e.getLoggerContextVO().getName());
        }

        if (loggingEnhancers != null) {
            for (LoggingEnhancer enhancer : loggingEnhancers) {
                enhancer.enhanceLogEntry(builder);
            }
        }

        return builder.build();
    }

    private Payload.JsonPayload toJsonPayload(ILoggingEvent e) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> sourceLocation = getSourceLocation(e);
        if (!sourceLocation.isEmpty()) {
            map.put("sourceLocation", sourceLocation);
        }

        if (e.getFormattedMessage() != null) {
            map.put("message", e.getFormattedMessage());
        }

        if (e.getThrowableProxy() != null) {
            String ex = throwableProxyConverter.convert(e);
            map.put("exception", Objects.toString(ex));
        }

        return Payload.JsonPayload.of(map);
    }

    private static Map<String, Object> getSourceLocation(ILoggingEvent event) {
        Map<String, Object> reportLocation = new HashMap<>();
        StackTraceElement callerData = event.getCallerData()[0];
        if (callerData != null) {
            reportLocation.put("file", callerData.getFileName());
            reportLocation.put("line", callerData.getLineNumber());
            reportLocation.put("function", callerData.getClassName() + "." + callerData.getMethodName());
        }
        reportLocation.put("thread", event.getThreadName());
        reportLocation.put("logger", event.getLoggerName());
        return reportLocation;
    }

    /**
     * Transforms Logback logging levels to Cloud severity.
     *
     * @param level Logback logging level
     * @return Cloud severity level
     */
    private static Severity severityFor(Level level) {
        switch (level.toInt()) {
            // TRACE
            case 5000:
                return Severity.DEBUG;
            // DEBUG
            case 10000:
                return Severity.DEBUG;
            // INFO
            case 20000:
                return Severity.INFO;
            // WARNING
            case 30000:
                return Severity.WARNING;
            // ERROR
            case 40000:
                return Severity.ERROR;
            default:
                return Severity.DEFAULT;
        }
    }
}
