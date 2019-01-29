package org.taymyr.lagom.javadsl.broker;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.pathCall;

public interface TestTopicPublisher extends Service {

    ServiceCall<NotUsed, NotUsed> publishWithoutKey(String msg);

    ServiceCall<NotUsed, NotUsed> publishWithKey(String msg);

    @Override
    default Descriptor descriptor() {
        return named("test-topic-publisher").withCalls(
            pathCall("/test-topic-publisher/without-key/:msg", this::publishWithoutKey),
            pathCall("/test-topic-publisher/with-key/:key", this::publishWithKey)
        ).withAutoAcl(true);
    }
}