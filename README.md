# gcloud-logging json log appender for sl4j + logback

[![](https://jitpack.io/v/ankurcha/gcloud-logging-slf4j-logback.svg)](https://jitpack.io/#ankurcha/gcloud-logging-slf4j-logback)

This library provides a simple json based layout that can be used to send structured logs to gcloud logging.

This appender is particularly useful in [Google Container Engine](https://cloud.google.com/container-engine/)
where logs should be emitted to stdout.

## Is this production ready?

Yes.

## Usage

Add dependency to `build.gradle`:

```groovy
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

dependencies {
    // refer to badge for latest version
    compile 'com.github.ankurcha:gcloud-logging-slf4j-logback:LATEST'
}
```

or to maven `pom.xml`:

```xml
<repositories>
    <repository>
	    <id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>

<dependency>
    <groupId>com.github.ankurcha</groupId>
	<artifactId>gcloud-logging-slf4j-logback</artifactId>
	<version>LATEST</version>
</dependency>
```

Add entry to `logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="KUBE_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="com.google.cloud.logging.GoogleCloudLoggingV2Layout">
                <appendLineSeparator>true</appendLineSeparator>
                <serviceName>@service-name@</serviceName>
                <serviceVersion>@git.sha@</serviceVersion>
                <jsonFormatter class="com.google.cloud.logging.GSONJsonFormatter"/>
            </layout>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="KUBE_CONSOLE"/>
    </root>

</configuration>

```

Example log line (prettified here):

```json
{
   "log":{
      "severity":"INFO",
      "context":{
         "reportLocation":{
            "functionName":"org.springframework.web.servlet.handler.AbstractUrlHandlerMapping.registerHandler",
            "filePath":"org/springframework/web/servlet/handler/AbstractUrlHandlerMapping.class",
            "lineNumber":354
         }
      },
      "time":{
         "seconds":1478198449,
         "nanos":186000000
      },
      "serviceContext":{
         "version":"c8a586a45a090516adf5cd79505179a6925da5db",
         "service":"ferric"
      },
      "message":"Mapped URL path [/**] onto handler of type [class org.springframework.web.servlet.resource.ResourceHttpRequestHandler]"
   }
}
```

## TODO

* [x] Update in readme
* [ ] Add some tests for the formatter
