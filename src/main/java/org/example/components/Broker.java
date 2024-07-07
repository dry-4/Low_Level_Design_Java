package org.example.components;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private Map<String, Subscriber> subscribers;
    private Map<String, Map<String, Subscriber>> topics;
    private ReadWriteLock lock;

    public Broker() {
        this.subscribers = new ConcurrentHashMap<>();
        this.topics = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void subscribe(Subscriber subscriber, String topic) {
        lock.readLock().lock();
        try {
            topics.putIfAbsent(topic, new HashMap<>());
            subscriber.addTopic(topic);
            topics.get(topic).put(subscriber.getId(), subscriber);
            System.out.printf("%s Subscribed for topic: %s\n", subscriber.getId(), topic);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void unsubscribe(Subscriber subscriber, String topic) {
        lock.readLock().lock();
        try {
            topics.get(topic).remove(subscriber.getId());
            subscriber.removeTopic(topic);
            System.out.printf("%s Unsubscribed for topic: %s\n", subscriber.getId(), topic);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Subscriber addSubscriber() {
        lock.writeLock().lock();
        try {
            Subscriber subscriber = new Subscriber(generateRandomId());
            subscribers.put(subscriber.getId(), subscriber);
            return subscriber;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeSubscriber(Subscriber subscriber) {
        lock.writeLock().lock();
        try {
            for (String topic : subscriber.getTopics().keySet()) {
                unsubscribe(subscriber, topic);
            }
            subscribers.remove(subscriber.getId());
            subscriber.deactivate();
            System.out.printf("Removed subscriber %s\n", subscriber.getId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getSubscribersCount(String topic) {
        lock.readLock().lock();
        try {
            return topics.getOrDefault(topic, new HashMap<>()).size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void broadcast(String msg, String[] topicList) {
        lock.readLock().lock();
        try {
            for (String topic : topicList) {
                for (Subscriber subscriber : topics.getOrDefault(topic, new HashMap<>()).values()) {
                    if (subscriber.isActive()) {
                        Message message = new Message(topic, msg);
                        new Thread(() -> subscriber.signal(message)).start();
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void publish(String topic, String msg) {
        lock.readLock().lock();
        try {
            for (Subscriber subscriber : topics.getOrDefault(topic, new HashMap<>()).values()) {
                if (subscriber.isActive()) {
                    Message message = new Message(topic, msg);
                    new Thread(() -> subscriber.signal(message)).start();
                    System.out.printf("Published & signaled %s to %s topic\n", msg, topic);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private String generateRandomId() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }
}
