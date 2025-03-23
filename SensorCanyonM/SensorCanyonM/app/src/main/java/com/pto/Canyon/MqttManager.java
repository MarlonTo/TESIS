package com.pto.Canyon;

import android.content.Context;
import android.util.Log;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class MqttManager {
    private static final String TAG = "MqttManager";
    private static final String HOST = "tcp://161.97.90.158:1883";
    private static final String CLIENT_ID = "SensorBike1";
    private static final String USERNAME = "pavel";
    private static final String PASSWORD = "2008P4P3lucho";
    private static final String TOPIC = "mensajeBike";

    private MqttAndroidClient mqttClient;
    private Context context;
    private boolean isConnected = false;

    public MqttManager(Context context) {
        this.context = context;
        initializeMqttClient();
    }

    private void initializeMqttClient() {
        mqttClient = new MqttAndroidClient(context, HOST, CLIENT_ID);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection lost: " + cause.getMessage());
                isConnected = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d(TAG, "Message received: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message delivered");
            }
        });
    }

    public void connect() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(USERNAME);
        mqttConnectOptions.setPassword(PASSWORD.toCharArray());
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setKeepAliveInterval(60);

        try {
            mqttClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Connected successfully");
                    isConnected = true;
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connection failed: " + exception.getMessage());
                    isConnected = false;
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error connecting: " + e.getMessage());
        }
    }

    private void subscribeToTopic() {
        try {
            mqttClient.subscribe(TOPIC, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to topic: " + TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to subscribe: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error subscribing: " + e.getMessage());
        }
    }

    public void publishData(String zona, String pulsos, String velocidad, String altitud, 
                          String distancia, String latitud, String longitud, String cadencia,
                          String edad, String nombre, String diametro, String potencia) {
        if (!isConnected) {
            Log.e(TAG, "MQTT client not connected");
            return;
        }

        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("zona", zona);
            jsonData.put("pulsos", pulsos);
            jsonData.put("velocidad", velocidad);
            jsonData.put("altitud", altitud);
            jsonData.put("distancia", distancia);
            jsonData.put("latitud", latitud);
            jsonData.put("longitud", longitud);
            jsonData.put("cadencia", cadencia);
            jsonData.put("edad", edad);
            jsonData.put("nombre", nombre);
            jsonData.put("diametro", diametro);
            jsonData.put("potencia", potencia);
            jsonData.put("timestamp", System.currentTimeMillis());

            MqttMessage message = new MqttMessage(jsonData.toString().getBytes());
            message.setQos(1);
            mqttClient.publish(TOPIC, message);
        } catch (Exception e) {
            Log.e(TAG, "Error publishing data: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            mqttClient.disconnect();
            isConnected = false;
        } catch (MqttException e) {
            Log.e(TAG, "Error disconnecting: " + e.getMessage());
        }
    }
} 