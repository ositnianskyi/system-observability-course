package com.course.bff.websockets.controllers;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class PushController implements MessageListener {
    private SimpMessagingTemplate template;

    public PushController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        this.template.convertAndSend("/topic/messages", String.format("Echo: %s", message));
    }
}
