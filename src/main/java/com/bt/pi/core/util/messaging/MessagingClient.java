package com.bt.pi.core.util.messaging;

public interface MessagingClient {
    void send(String[] recipients, String subject, String message);
}
