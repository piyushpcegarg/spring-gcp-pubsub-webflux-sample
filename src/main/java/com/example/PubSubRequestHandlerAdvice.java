package com.example;

import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class PubSubRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

  private final MessagingTemplate messagingTemplate = new MessagingTemplate();

  @Override
  protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {

    Object result = callback.execute();

    Object evalResult = message.getPayload();
    MessageChannel successChannel = null;
    Object replyChannelHeader = message.getHeaders().getReplyChannel();
    if (replyChannelHeader instanceof MessageChannel) {
      successChannel = (MessageChannel) replyChannelHeader;
    }

    if (evalResult != null && successChannel != null) {
      AdviceMessage<?> resultMessage = new AdviceMessage<>(evalResult, message);
      this.messagingTemplate.send(successChannel, resultMessage);
    }
    return result;
  }
}
