[![Gitter](https://img.shields.io/badge/chat-gitter-purple.svg)](https://gitter.im/taymyr/taymyr)
[![Gitter_RU](https://img.shields.io/badge/chat-russian%20channel-purple.svg)](https://gitter.im/taymyr/taymyr_ru)
[![Build Status](https://app.travis-ci.com/taymyr/lagom-extensions.svg?branch=master)](https://app.travis-ci.com/taymyr/lagom-extensions)
[![Javadocs](https://www.javadoc.io/badge/org.taymyr.lagom/lagom-extensions-java_2.12.svg)](https://www.javadoc.io/doc/org.taymyr.lagom/lagom-extensions-java_2.12)
[![Codecov](https://codecov.io/gh/taymyr/lagom-extensions/branch/master/graph/badge.svg)](https://codecov.io/gh/taymyr/lagom-extensions)
[![Maven Central](https://img.shields.io/maven-central/v/org.taymyr.lagom/lagom-extensions-java_2.12.svg)](https://search.maven.org/search?q=a:lagom-extensions-java_2.12%20AND%20g:org.taymyr.lagom)

# Lagom Java API Extensions

This library is an extension of Lagom Java/Scala DSL.

_Note: We try not to change the API, but before the release of stable version `1.0.0` API may be changed._

## Versions compatibility

| Lagom Extensions | Lagom            | Scala          |
|------------------|------------------|----------------|
| 0.+              | 1.5.+ <br> 1.6.+ | 2.12 <br> 2.13 |

## Features

### Message Protocols (Java &#10003; / Scala &#10007; )

`MessageProtocols` have constants for most used message protocols (`application/json`, `application/json; charset=utf-8`, etc).
See [Javadoc](https://www.javadoc.io/doc/org.taymyr.lagom/lagom-extensions-java_2.12) for more information.

### Response Headers (Java &#10003; / Scala &#10007; )

`ResponseHeaders` have constants of `ResponseHeader` and utilities functions for instantiation `Pair<ResponseHeader, T>`.

Code example:

```java
// Lagom
(headers, request) -> { 
    ...
    return completedFuture(
        new Pair<>(
            ResponseHeader.OK.withProtocol(MessageProtocol.fromContentTypeHeader(Optional.of("application/json"))), 
            result
        )
    );
};

// Lagom Extensions
(headers, request) -> { 
    ...
    return completedFuture(okJson(result));
};
```

### Simple Kafka Producer (Java &#10003; / Scala &#10007; )

At this moment Lagom (1.4.+) doesn't provide any framework-level API to produce records to topics declared in subscriber-only service descriptors. 
In such cases, we need to use the underlying [Alpakka Kafka](https://doc.akka.io/docs/akka-stream-kafka/current/home.html) directly to publish.

1. It is useful to place `TopicDescriptor` in the subscriber-only service descriptor.

```java
public interface FooTopicService extends Service {

    TopicDescriptor<FooTopicRecord> FOO_TOPIC = TopicDescriptor.of("foo-topic", FooTopicRecord.class);
    
    Topic<String> fooTopic();

    @Override
    default Descriptor descriptor() {
        return named("foo-topic-service")
            .withTopics(topic(FOO_TOPIC.getId(), this::fooTopic))
            .withAutoAcl(true);
    }
}
```

At the topic call declaration you may also specify `.withProperties(KafkaProperties.partitionKeyStrategy, ...)` to support topic record key generation (see [Lagom docs](https://www.lagomframework.com/documentation/current/java/MessageBrokerApi.html#Partitioning-topics)).

2. You should inject `SimpleTopicProducersRegistry` and register producers for the declared topics (other details are intentionally omitted)

```java
public class BarServiceImpl implements BarService {

    private SimpleTopicProducersRegistry registry;

    @Inject
    public BarServiceImpl(FooTopicService fooTopicService, SimpleTopicProducersRegistry registry) {
        this.registry = registry.register(fooTopicService);
    }
}
```

3. Now you able to retrieve producer for the desired topic from the registry and to publish record easily.

```java
@Override
public ServiceCall<FooTopicRecord, NotUsed> publishToFoo() {
    return fooTopicRecord ->
            registry.get(FooTopicService.FOO_TOPIC).publish(fooTopicRecord)
                .thenApply( x -> NotUsed.getInstance() );
}
```

4. `SimpleTopicProducer` relies on `akka.kafka.producer` config by default (see [Akka producer](https://doc.akka.io/docs/akka-stream-kafka/current/producer.html#settings), [Akka source](https://doc.akka.io/japi/akka/2.5/akka/stream/javadsl/Source.html#queue(int,akka.stream.OverflowStrategy))).
You also may provide a separate config for each topic producer. In that case, config path should be `<topic-name>.producer` instead of `akka.kafka.producer`.

```HOCON
foo-topic.producer {
  # Tuning parameter of how many sends that can run in parallel.
  parallelism = 100

  # Duration to wait for `KafkaConsumer.close` to finish.
  close-timeout = 60s
  
  # Fully qualified config path which holds the dispatcher configuration
  # to be used by the producer stages. Some blocking may occur.
  # When this value is empty, the dispatcher configured for the stream
  # will be used.
  use-dispatcher = "akka.kafka.default-dispatcher"

  # The time interval to commit a transaction when using the `Transactional.sink` or `Transactional.flow`
  eos-commit-interval = 100ms

  # Size of buffer in element count
  buffer-size = 100

  # Strategy that is used when incoming elements cannot fit inside the buffer.
  # Possible values: "dropHead", "backpressure", "dropBuffer", "dropNew", "dropTail", "fail".
  overflow-strategy = "dropHead"

  # Minimum (initial) duration until the child actor will started again, if it is terminated.
  min-backoff = 3s

  # The exponential back-off is capped to this duration.
  max-backoff = 30s

  # After calculation of the exponential back-off an additional random delay based on this factor is added,
  # e.g. 0.2 adds up to 20% delay. In order to skip this additional delay pass in 0.
  random-factor = 0.2

  # Properties defined by org.apache.kafka.clients.producer.ProducerConfig
  # can be defined in this configuration section.
  kafka-clients {
  }
}
```

5. Also you can use a `serviceName` property for lookup bootstrap servers by `ServiceLocator` of _Lagom_. 
And you can customize the name of topic by property `topic-name` (it can be useful for using naming convensions for difference environments). 

```HOCON
foo-topic {
  serviceName = "kafka_native"
  
  topic-name = "foo-topic-envXY"
}
```

### Logging requests/responses of strict client HTTP calls with `ConfiguredAhcWSClient`
Unfortunately out-of-the-box Lagom doesn't support request/response logging for client strict HTTP calls. 
`ConfiguredAhcWSClient` is a simple custom implementation of the `play.api.libs.ws.WSClient` which Lagom uses to perform the strict client HTTP calls. 
It allows you to enable request/response (including the body) logging. It can be enabled in your `application.conf` as follows:
```hocon
configured-ahc-ws-client.logging.enabled = true
```
Enjoy!

### ServiceCall running on coroutines (Java &#10007; / Scala &#10007; / Kotlin &#10003;)
Using `CoroutineService` you can make requests using coroutines.
Example:
```kotlin
class TestService @Inject constructor(actorSystem: ActorSystem) : Service, CoroutineService {
    
    override val dispatcher: CoroutineDispatcher = actorSystem.dispatcher.asCoroutineDispatcher()
    
    private fun testMethod(): ServiceCall<NotUsed, String> = serviceCall { 
        "Hello, from coroutine!"
    }

    override fun descriptor(): Descriptor {
        return Service.named("test-service")
            .withCalls(
                Service.restCall<NotUsed, String>(Method.GET, "/test", TestService::testMethod.javaMethod)
            )
    }
}
```
You must define the `CoroutineDispatcher` on which the coroutines will run. Basically, you need to use akka default execution context.
`CoroutineSecuredService` allows you to execute authorized requests from `org.pac4j.lagom`. Example:
```kotlin
class TestService @Inject constructor(actorSystem: ActorSystem) : Service, CoroutineSecuredService {

    override fun getSecurityConfig(): Config {
        TODO("Return security config")
    }

    override val dispatcher: CoroutineDispatcher = actorSystem.dispatcher.asCoroutineDispatcher()

    private fun testMethod(): ServiceCall<NotUsed, String> = authenticatedServiceCall { request, profile ->
        "Hello, from coroutine!"
    }

    override fun descriptor(): Descriptor {
        return Service.named("test-service")
            .withCalls(
                Service.restCall<NotUsed, String>(Method.GET, "/test", TestService::testMethod.javaMethod)
            )
    }
}
```
## How to use

All **released** artifacts are available in the [Maven central repository](https://search.maven.org/search?q=a:lagom-extensions-java_2.12%20AND%20g:org.taymyr.lagom).
Just add a `lagom-extensions` to your service dependencies:

* **SBT**

```scala
libraryDependencies += "org.taymyr.lagom" %% "lagom-extensions-java" % "X.Y.Z"
```

* **Maven**

```xml
<dependency>
  <groupId>org.taymyr.lagom</groupId>
  <artifactId>lagom-extensions-java_${scala.binary.version}</artifactId>
  <version>X.Y.Z</version>
</dependency>
```

All **snapshot** artifacts are available in the [Sonatype snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/org/taymyr/lagom).
This repository must be added in your build system. 

* **SBT**

```scala
resolvers ++= Resolver.sonatypeRepo("snapshots")
```

* **Maven**
```xml
<repositories>
  <repository>
    <id>snapshots-repo</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <releases><enabled>false</enabled></releases>
    <snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>
``` 

## Contributions

Contributions are very welcome.

## License

Copyright © 2018-2020 Digital Economy League (https://www.digitalleague.ru/).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
