package com.demo.cloudstreampoc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

import com.azure.spring.integration.core.EventHubHeaders;
import com.azure.spring.integration.core.api.reactor.Checkpointer;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Sinks;

import static com.azure.spring.integration.core.AzureHeaders.CHECKPOINTER;


@SpringBootApplication
public class CloudstreampocApplication {

    public static final Logger LOGGER = LoggerFactory.getLogger(CloudstreampocApplication.class);

    @Autowired
    private StreamBridge streamBridge;


    public static void main(String[] args) {
        SpringApplication.run(CloudstreampocApplication.class, args);
    }

    @Autowired
    private Sinks.Many<Message<String>> many;
    private int i = 0;

    @Bean
    public CommandLineRunner runner() {
        return args -> {
            for (int k = 0; k < 500; k++) {
                String message = "Hello world, " + i++;
                LOGGER.info("Going to add message {} to sendMessage.", message);
                many.emitNext(MessageBuilder.withPayload(message).build(), Sinks.EmitFailureHandler.FAIL_FAST);
                TimeUnit.SECONDS.sleep(1L);
            }
        };
    }

    //Use Consumer if just need to process the data from event hub
    // Use Function if we need to receive from one event hub and after processing needs to store in other event hub
    @Bean
    public Consumer<Message<String>> consume() {
        return message -> {
            Checkpointer checkpointer = (Checkpointer) message.getHeaders().get(CHECKPOINTER);

            LOGGER.info("New message received: '{}', partition key: {}, sequence number: {}, offset: {}, enqueued "
                    + "time: {}",
                message.getPayload(),
                message.getHeaders().get(EventHubHeaders.PARTITION_KEY),
                message.getHeaders().get(EventHubHeaders.SEQUENCE_NUMBER),
                message.getHeaders().get(EventHubHeaders.OFFSET),
                message.getHeaders().get(EventHubHeaders.ENQUEUED_TIME)
            );

            if ((Long) (message.getHeaders().get(EventHubHeaders.SEQUENCE_NUMBER)) % 2 == 0) {
                LOGGER.info("Sending to binding 0 eventhub");
                streamBridge.send("valid-out-0", message);
            } else {
                LOGGER.info("Sending to binding 1 eventhub");
                streamBridge.send("reject-out-0", message);
            }

            checkpointer.success()
                        .doOnSuccess(success -> LOGGER.info("Message '{}' successfully checkpointed",
                            message.getPayload()))
                        .doOnError(error -> LOGGER.error("Exception found", error))
                        .subscribe();
        };
    }

    // Replace destination with spring.cloud.stream.bindings.input.destination
    // Replace group with spring.cloud.stream.bindings.input.group
    @ServiceActivator(inputChannel = "queue1.$Default.errors")
    public void consumerError(ErrorMessage message) {
        System.out.println("Handling comsumer error in error channel" + message);
        System.out.println(message.getPayload().getStackTrace().toString());
        LOGGER.error("Handling customer ERROR: " + message.getOriginalMessage().getPayload());
    }

    @ServiceActivator(inputChannel = "queue1.errors")
    public void supplyError(Message<?> message) {
        LOGGER.error("Handling supply ERROR: {}", message);
    }

    @ServiceActivator(inputChannel = "queue2.errors")
    public void validError(Message<?> message) {
        LOGGER.error("Handling Valid Event Publisher ERROR: {}", message);
    }

    @ServiceActivator(inputChannel = "queue3.errors")
    public void rejectError(Message<?> message) {
        LOGGER.error("Handling Reject Event Publisher ERROR: {}", message);
    }
}
