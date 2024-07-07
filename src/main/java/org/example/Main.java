package org.example;
import org.example.components.Broker;
import org.example.components.Subscriber;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final Map<String, String> availableTopics = new HashMap<>();
    static {
        availableTopics.put("BTC", "BITCOIN");
        availableTopics.put("ETH", "ETHEREUM");
        availableTopics.put("DOT", "POLKADOT");
        availableTopics.put("SOL", "SOLANA");
    }

    private static void pricePublisher(Broker broker) {
        List<String> topicValues = new ArrayList<>(availableTopics.values());

        Random random = new Random();
        while (true) {
            String randomValue = topicValues.get(random.nextInt(topicValues.size()));
            String message = String.format("%f", random.nextDouble());
            broker.publish(randomValue, message);
            System.out.printf("Publishing %s to %s topic\n", message, randomValue);

            try {
                Thread.sleep(5000); // sleep for 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        Subscriber s1 = broker.addSubscriber();
        broker.subscribe(s1, availableTopics.get("BTC"));
        broker.subscribe(s1, availableTopics.get("ETH"));

        Subscriber s2 = broker.addSubscriber();
        broker.subscribe(s2, availableTopics.get("ETH"));
        broker.subscribe(s2, availableTopics.get("SOL"));

        ExecutorService executorService = Executors.newCachedThreadPool();

        executorService.execute(() -> {
            try {
                Thread.sleep(3000); // sleep for 3 seconds
                broker.subscribe(s2, availableTopics.get("DOT"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executorService.execute(() -> {
            try {
                Thread.sleep(5000); // sleep for 5 seconds
                broker.unsubscribe(s2, availableTopics.get("SOL"));
                System.out.printf("Total subscribers for topic ETH is %d\n", broker.getSubscribersCount(availableTopics.get("ETH")));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executorService.execute(() -> {
            try {
                Thread.sleep(10000); // sleep for 10 seconds
                broker.removeSubscriber(s2);
                System.out.printf("Total subscribers for topic ETH is %d\n", broker.getSubscribersCount(availableTopics.get("ETH")));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executorService.execute(() -> pricePublisher(broker));
        executorService.execute(() -> s1.listen());
        executorService.execute(() -> s2.listen());

        try {
            System.in.read(); // wait for a key press to exit
        } catch (IOException e) {
            e.printStackTrace();
        }

        executorService.shutdownNow();
        System.out.println("Done!");
    }
}
