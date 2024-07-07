package org.example.components;

public class Message {
    private final String topic;
    private final String messageBody;

    public Message(String topic, String messageBody) {
        this.topic = topic;
        this.messageBody = messageBody;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageBody() {
        return messageBody;
    }

}
