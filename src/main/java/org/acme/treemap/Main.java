package org.acme.treemap;

import org.acme.treemap.model.Message;
import org.acme.treemap.model.MessageType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("Starting the application");
        Message message = new Message(MessageType.INFO, "Hello, World!");
        System.out.println(message);
    }
}
