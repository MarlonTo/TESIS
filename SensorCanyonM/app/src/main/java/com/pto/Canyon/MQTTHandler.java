package com.pto.Canyon;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;

public class MQTTHandler {
    private static final String TAG = "MQTTHandler";
    private static final String BROKER = "tcp://186.4.224.175:1883";
    private static final String CLIENT_ID = "AndroidSensorApp";
    private MqttClient client;
    private final Context context;
    private final List<String> pendingMessages;
    private final String topic;

    public MQTTHandler(Context context, String topic) {
        this.context = context;
        this.topic = topic;
        this.pendingMessages = new ArrayList<>();
        setupMQTTClient();
    }

    private void setupMQTTClient() {
        try {
            client = new MqttClient(BROKER, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            
            if (isNetworkAvailable()) {
                client.connect(options);
                Log.d(TAG, "MQTT Client connected successfully");
                sendPendingMessages();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error setting up MQTT client: " + e.getMessage());
        }
    }

    public void publishMessage(String message) {
        if (!isNetworkAvailable() || !client.isConnected()) {
            pendingMessages.add(message);
            Log.d(TAG, "No connection available. Message stored for later sending.");
            return;
        }

        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic, mqttMessage);
            Log.d(TAG, "Message published successfully");
        } catch (MqttException e) {
            Log.e(TAG, "Error publishing message: " + e.getMessage());
            pendingMessages.add(message);
        }
    }

    private void sendPendingMessages() {
        if (!client.isConnected() || pendingMessages.isEmpty()) {
            return;
        }

        List<String> successfullySent = new ArrayList<>();
        for (String message : pendingMessages) {
            try {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                client.publish(topic, mqttMessage);
                successfullySent.add(message);
                Log.d(TAG, "Pending message sent successfully");
            } catch (MqttException e) {
                Log.e(TAG, "Error sending pending message: " + e.getMessage());
                break;
            }
        }
        pendingMessages.removeAll(successfullySent);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void reconnect() {
        if (!isNetworkAvailable() || client.isConnected()) {
            return;
        }

        try {
            client.connect();
            sendPendingMessages();
        } catch (MqttException e) {
            Log.e(TAG, "Error reconnecting: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error disconnecting: " + e.getMessage());
        }
    }
} 