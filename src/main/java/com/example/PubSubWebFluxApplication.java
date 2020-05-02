package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Entry point into the sample application.
 *
 * @author Piyush Garg
 */
@SpringBootApplication
public class PubSubWebFluxApplication {

	private static final Log LOGGER = LogFactory.getLog(PubSubWebFluxApplication.class);

	private static final String TOPIC_NAME = "person";

	public static void main(String[] args) {
		SpringApplication.run(PubSubWebFluxApplication.class, args);
	}

	/**
	 * bean to deserialize request payload.
	 */
	@Bean
	public JacksonPubSubMessageConverter jacksonPubSubMessageConverter(ObjectMapper objectMapper) {
		return new JacksonPubSubMessageConverter(objectMapper);
	}

	@Bean
	public MessageChannel pubSubOutputChannel() {
		return MessageChannels.flux().get();
	}

	/**
	 * Message handler which will consume messages from message channel.
	 * Then it will send google cloud pubsub topic.
	 */
	@Bean
	@ServiceActivator(inputChannel = "pubSubOutputChannel")
	public MessageHandler messageSender(PubSubTemplate pubSubTemplate) {
		PubSubMessageHandler handler = new PubSubMessageHandler(pubSubTemplate, TOPIC_NAME);
		handler.setSync(true);
		handler.setPublishCallback(new ListenableFutureCallback<>() {
			@Override
			public void onFailure(Throwable ex) {
				LOGGER.info("There was an error sending the message.");
			}

			@Override
			public void onSuccess(String result) {
				LOGGER.info("Message was sent successfully.");
			}
		});

		return handler;
	}

	/**
	 * Webflux endpoint to consume http request.
	 */
	@Bean
	public WebFluxInboundEndpoint webFluxInboundEndpoint() {

		WebFluxInboundEndpoint endpoint = new WebFluxInboundEndpoint();

		RequestMapping requestMapping = new RequestMapping();
		requestMapping.setMethods(HttpMethod.POST);
		requestMapping.setConsumes(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		requestMapping.setPathPatterns("/createPerson");
		endpoint.setRequestMapping(requestMapping);

		endpoint.setRequestChannel(pubSubOutputChannel());

		return endpoint;
	}
}
