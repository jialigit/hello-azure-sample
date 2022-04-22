package com.demo.cloudstreampoc;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Service;

import com.azure.spring.integration.core.api.reactor.Checkpointer;

//@Slf4j
//@Service
//@GlobalChannelInterceptor(patterns = "consume-in-*")
public class ConsumerInputInterceptor implements ChannelInterceptor {

	public static final Logger LOGGER = LoggerFactory.getLogger(ConsumerInputInterceptor.class);
	private static final Object CHECKPOINTER = "";
	
  @Override
  public void afterSendCompletion(
      @NotNull Message<?> message,
      @NotNull MessageChannel channel,
      boolean sent,
      Exception ex) {
	  LOGGER.info("afterSendCompletion '{}' to '{}', sent={}, ex='{}'",
        message, channel, sent, ex != null ? ex.getMessage() : "-");
    if (sent) {
    	LOGGER.info("Message sent successfully, will be checkpointed: '{}'", message.getPayload());
			/*
			 * Optional.ofNullable((Checkpointer) message.getHeaders().get(CHECKPOINTER))
			 * .map(c -> c.success() .doOnSuccess(s ->
			 * LOG.info("Message successfully checkpointed: '{}'", message.getPayload()))
			 * .doOnError(error -> LOG.error("Checkpoint failed", error)) .subscribe());
			 */
    } else {
    	LOGGER.info("Message was not sent, will not be checkpointed: '{}'", message.getPayload());
    }
  }

}