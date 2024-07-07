package org.example.components;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Subscriber {
    private String id;
    private Map<String, Boolean> topics;
    private BlockingQueue<Message> messages;

    private boolean active;

    private final ReadWriteLock lock;

    public String getId() {
        return id;
    }

    public Subscriber(String id) {
        this.id = id;
        this.messages = new LinkedBlockingQueue<>();
        this.topics = new ConcurrentHashMap<>();
        this.active = true;
        this.lock = new ReentrantReadWriteLock();
    }

    private static String generateRandomId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    // Helper method to convert byte array to hexadecimal string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    public Subscriber createNewSubscriber() {
        return new Subscriber(generateRandomId());
    }

    public void addTopic(String topic) {
        lock.writeLock().lock();
        try {
            topics.put(topic, true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeTopic(String topic) {
        lock.writeLock().lock();
        try {
            topics.remove(topic);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, Boolean> getTopics() {
         return topics;
    }

    public boolean isActive() {
        lock.readLock().lock();
        try {
            return active;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void activate() {
        lock.writeLock().unlock();
        try {
            active = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deactivate() {
        lock.writeLock().lock();
        try {
            active = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void signal(Message msg) {
        lock.readLock().lock();
        try {
            if (active) {
                messages.add(msg);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void listen() {
        while (true) {
            try {
                Message msg = messages.take();
                System.out.printf("Subscriber %s, received: %s from topic: %s\n",
                        id, msg.getMessageBody(), msg.getTopic());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

}
