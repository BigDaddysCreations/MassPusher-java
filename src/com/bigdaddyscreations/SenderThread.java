package com.bigdaddyscreations;

import javapns.Push;
import javapns.notification.Payload;
import javapns.notification.PushedNotification;

import java.util.ArrayList;
import java.util.List;

public class SenderThread extends Thread {
    private List<DeviceToken> tokens;
    private Payload payload;
    private Object keystore;
    private String password;
    private boolean production;
    private int threadCount;

    private int sentCount;
    private int batchSize;

    private List<PushedNotification> successfulNotifications;
    private List<PushedNotification> failedNotifications;

    public SenderThread(List<DeviceToken> tokens, Payload payload, Object keystore, String password, boolean production, int threadCount, int sentCount, int batchSize) {
        this.tokens = tokens;
        this.payload = payload;
        this.keystore = keystore;
        this.password = password;
        this.production = production;
        this.threadCount = threadCount;
        this.sentCount = sentCount;
        this.batchSize = batchSize;

        successfulNotifications = new ArrayList<PushedNotification>();
        failedNotifications = new ArrayList<PushedNotification>();
    }

    public SenderThread(List<DeviceToken> tokens, Payload payload, Object keystore, String password, boolean production) {
        this(tokens, payload, keystore, password, production, 50, 0, 100);
    }

    public int getSentCount() { return sentCount; }

    public List<PushedNotification> getSuccessfulNotifications() { return successfulNotifications; }
    public List<PushedNotification> getFailedNotifications() { return failedNotifications; }

    @Override
    public void run() {
        while(sentCount < batchSize && sentCount < tokens.size()) {
            List<DeviceToken> toSend = new ArrayList<DeviceToken>(batchSize);
            int i;
            for (i = sentCount; i < batchSize && i < tokens.size(); ++i)
                toSend.add(tokens.get(i));

            sentCount = i;

            try {
                List<PushedNotification> results = sendNotifications(toSend, payload, keystore, password, production, threadCount);

                successfulNotifications.addAll(PushedNotification.findSuccessfulNotifications(results));
                failedNotifications.addAll(PushedNotification.findFailedNotifications(results));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<PushedNotification> sendNotifications(List<DeviceToken> tokens, Payload payload, Object keystore, String password, boolean production, int threads) throws Exception {
        List<String> devices = new ArrayList<String>();

        for (DeviceToken token : tokens)
            devices.add(token.deviceToken);

        return Push.payload(payload, keystore, password, production, threads, devices);
    }

}
