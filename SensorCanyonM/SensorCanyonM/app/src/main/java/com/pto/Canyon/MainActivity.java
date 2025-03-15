package com.pto.Canyon;

import static java.lang.Math.abs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.anastr.speedviewlib.ProgressiveGauge;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.UUID;
public class MainActivity extends AppCompatActivity implements IBaseGpsListener, SensorEventListener {
    //Sensor del Corazón
    //banda anterior
    public static final String MAC_ADDRESS = "E0:13:76:E8:AE:C7";
    //banda nueva
   // public static final String MAC_ADDRESS = "E8:3E:7C:69:50:CF";
    public static final UUID UUID_SERVICE = UUID.fromString("00000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CORAZON = UUID.fromString("000002a37-0000-1000-8000-00805f9b34fb");
    //Sensor de Cadencia & Velocidad
    //LEZYNE
   // public static final String MAC_ADDRESS_CV = "CD:C0:8D:E9:C8:12";

    //CYCPLUS VELOCIDAD CANYON
    public static final String MAC_ADDRESS_CV = "FE:2A:38:59:02:B5";

    //CYCPLUS VELOCIDAD BULLS
    public static final String MAC_ADDRESS_CA = "FE:2A:38:59:02:B5";

    public static final UUID UUID_SERVICE_CV = UUID.fromString("000001816-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CV = UUID.fromString("000002a5b-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_CA = UUID.fromString("000001816-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CA = UUID.fromString("000002a5b-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DESCRIPTOR_CA = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static BluetoothManager mBluetoothManager;
     public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothGatt mBluetoothGatt;
    public static BluetoothGatt mBluetoothGattCV;
    public static BluetoothGatt mBluetoothGattCA;
    public String TAG = "PTO", FILE_NAME = "bike";
    TextView textPulsos, textVelocidad, textNegativo,textPositivo,txtDistancia, textZona, textAltitud, txtCadencia, txtPeso;
    TextView txtLongitud,txtLatitud,txtMensaje, txtNombre, txtEdad, txtDiametro, txtUidad;
    EditText etNombre, etEdad, etDiametro,etPeso;
    Button bConexion, bInicio, bParametros;
    int tiempoactual= 0, tiempoanterior= 0, vueltaactual=0, vueltaanterior=0,vueltasTotal, lecturasAltimetria = 1;
    float tiempoactualCA= 0, tiempoanteriorCA= 0, vueltaactualCA=0, vueltaanteriorCA=0,vueltasTotalCA =0,cadenciaTotal= 0;
    int zonaMaxima, zonaMinima,numeroLecturas = 1;
    float anguloAcumulado = 0, pendiente = 0.0f;
    double velocidadTotal, distanciaTotal = 0.0, sumaAltimetrias = 0.0;
    double desnivelPositivo = 0, desnivelNegativo = 0, altitudAnterior = 0;
    double desnivelAnteriorP = 0, desnivelActualP = 0, distanciaAnteriorP = 0,distanciaActualP = 0;
    ProgressiveGauge progressiveGauge;
    ////MQTT
    private final String TAG1 = "AiotMqtt";
    final String host = "tcp://161.97.90.158:1883";
    private String clientId="SensorBike1";
    private String userName="pavel";
    private String passWord="2008P4P3lucho";
    private String SUB_TOPIC ="mensajeBike";

    MqttAndroidClient mqttAndroidClient;

    ////
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    ////end MQTT
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        private final String TAG = "mGattCallback";
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, status + " " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //connectBtn.setEnabled(false);
                        //disconectBtn.setEnabled(true);
                    }
                });
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // connectBtn.setEnabled(true);
                        //disconectBtn.setEnabled(false);
                    }
                });
            }
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == gatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID_SERVICE);
                if (service != null) {
                    Log.i(TAG, "Service connected");
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_CORAZON);
                    if (characteristic != null) {
                        Log.i(TAG, "Characteristica conectada");
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR);
                        if (descriptor != null) {
                            // Los descriptors son muy importantes
                            // TODO: Continue studying about descriptors in BLE
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            Log.i(TAG, "Descriptor enviado");
                        }
                    }

                }


            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            readBtnStateCharacteristic(characteristic);
        }

        private void readBtnStateCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (UUID_CHARACTERISTIC_CORAZON.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                int vs = characteristic.getIntValue(characteristic.FORMAT_UINT8, 1);
                //int state = Ints.fromByteArray(data);

               // Log.i("PULSOS:", data[1] + "");
               // Log.i("PULSOS 1:", vs + "");
                int subZona = 0;
                int colorZona = 0;
                String textoZona="Z1.0";

                if ( vs >= zonaMaxima*.22 && vs < zonaMaxima*.72  ){
                    subZona = (int) ((vs-zonaMaxima*.22)*10/8.8);
                    textoZona = "1."+ subZona + "";
                    colorZona = Color.WHITE;
                }
                if (vs >= zonaMaxima*.72 && vs < zonaMaxima*.82 ){
                    subZona = (int) ((vs-zonaMaxima*.72)*10/17);
                    textoZona = "2." + subZona + "";
                    colorZona = Color.WHITE;
                }
                if (vs >= zonaMaxima*.82 && vs < zonaMaxima*.92 ){
                    subZona = (int) ((vs - zonaMaxima*.82)*10/17);
                    textoZona = "3." + subZona + "";
                    colorZona = Color.YELLOW;
                }
                if (vs >= zonaMaxima*.92  && vs <=zonaMaxima ){
                    subZona = (int) ((vs- zonaMaxima*.92)*10/17);
                    textoZona = "4." + subZona + "";
                    colorZona = Color.RED;
                }
                //if (vs > 170  ){
                if (vs > zonaMaxima){
                    subZona = (vs-zonaMaxima)*10/17;
                    textoZona = "5." + subZona + "";
                    colorZona = Color.RED;
                }
                //Log.i("ZONAS:", textoZona);
                String finalTextoZona = textoZona;
                int finalColorZona = colorZona;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textPulsos.setText(Integer.toString(vs));
                        textZona.setText(finalTextoZona);
                        textZona.setTextColor(finalColorZona);
                        progressiveGauge.setSpeedometerColor(finalColorZona);
                        progressiveGauge.speedTo(vs);

                    }
                });

            }


        }
    };
/////CV inicio /////
     private BluetoothGattCallback mGattCallbackCV = new BluetoothGattCallback() {

    private final String TAG = "mGattCallbackCV";

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        super.onConnectionStateChange(gatt, status, newState);
        Log.i(TAG, status + " " + newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //connectBtn.setEnabled(false);
                    //disconectBtn.setEnabled(true);
                }
            });
            gatt.discoverServices();

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // connectBtn.setEnabled(true);
                    //disconectBtn.setEnabled(false);
                }
            });
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == gatt.GATT_SUCCESS) {
                BluetoothGattService serviceCV = gatt.getService(UUID_SERVICE_CV);
                if (serviceCV != null) {
                    Log.i(TAG, "Service connected CV");
                    BluetoothGattCharacteristic characteristicCV = serviceCV.getCharacteristic(UUID_CHARACTERISTIC_CV);
                    if (characteristicCV != null) {
                        Log.i(TAG, "Characteristica conectada CV");
                        gatt.setCharacteristicNotification(characteristicCV, true);
                        BluetoothGattDescriptor descriptorCV = characteristicCV.getDescriptor(UUID_DESCRIPTOR);
                        if (descriptorCV != null) {
                            // Los descriptors son muy importantes
                            // TODO: Continue studying about descriptors in BLE
                            descriptorCV.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptorCV);
                            Log.i(TAG, "Descriptor enviado CV");
                        }
                    }


                }

            }



    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        readBtnStateCharacteristic(characteristic);
    }

    private void readBtnStateCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (UUID_CHARACTERISTIC_CV.equals(characteristic.getUuid())) {
            byte[] data = characteristic.getValue();
            int vs = characteristic.getIntValue(characteristic.FORMAT_UINT8, 1);
            //Sensor CYCPLUS --Bike CANYON
            int vs5 = characteristic.getIntValue(characteristic.FORMAT_UINT16, 5);
            //Sensor LEZYNE --Bike BULLS
            //int vs5 = characteristic.getIntValue(characteristic.FORMAT_UINT32, 5);
           // Log.i("VELOCIDAD",""+ vs5 );

            //int state = Ints.fromByteArray(data);

            vueltaactual = vs;
            tiempoactual = vs5;
            if (vueltaanterior == vueltaactual) {
                velocidadTotal = 0.0;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double tiempoTotal = 0;
                    double distancia = 0.0;

                    if (vueltaanterior < vueltaactual) {
                        vueltasTotal = vueltaactual - vueltaanterior;
                        distancia = 0.0023*vueltasTotal;
                        //distanciaAnteriorP = distanciaTotal;
                        distanciaTotal = distanciaTotal + distancia;
                        distanciaActualP = distanciaTotal;
                        tiempoTotal = ((double) tiempoactual  - (double) tiempoanterior)/3600000;
                        velocidadTotal = distancia/tiempoTotal;

                        tiempoanterior = tiempoactual;
                        vueltaanterior = vueltaactual;

                    }else {

                        vueltaanterior = vueltaactual;
                    }

                       // DecimalFormat df = new DecimalFormat("#.#");
                        Formatter fmt = new Formatter(new StringBuilder());
                       fmt.format(Locale.US, "%5.1f", velocidadTotal);
                        String strCurrentSpeed = fmt.toString();
                        // imageSpeedometer.speedTo((float) velocidadTotal);
                    if (strCurrentSpeed.compareTo("0.0") != 0)
                        textVelocidad.setText(strCurrentSpeed);


                    Formatter fmtd = new Formatter(new StringBuilder());
                    fmtd.format(Locale.US, "%5.2f", distanciaTotal);
                    String strdistanciaTotal = fmtd.toString();
                    txtDistancia.setText(strdistanciaTotal + " km");
                    //grabar_nuevo(strCurrentSpeed);

                }
            });


        }


    }
};
///CV fin////

//CADENCIA INICIO
private BluetoothGattCallback mGattCallbackCA = new BluetoothGattCallback() {

    private final String TAG = "mGattCallbackCA";

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        super.onConnectionStateChange(gatt, status, newState);
        Log.i(TAG, status + " " + newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //connectBtn.setEnabled(false);
                    //disconectBtn.setEnabled(true);
                }
            });
            gatt.discoverServices();

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // connectBtn.setEnabled(true);
                    //disconectBtn.setEnabled(false);
                }
            });
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == gatt.GATT_SUCCESS) {
            BluetoothGattService serviceCA = gatt.getService(UUID_SERVICE_CA);
            if (serviceCA != null) {
                Log.i(TAG, "Service connected CA");
                BluetoothGattCharacteristic characteristicCA = serviceCA.getCharacteristic(UUID_CHARACTERISTIC_CA);
                if (characteristicCA != null) {
                    Log.i(TAG, "Characteristica conectada CA");
                    gatt.setCharacteristicNotification(characteristicCA, true);
                    BluetoothGattDescriptor descriptorCA = characteristicCA.getDescriptor(UUID_DESCRIPTOR_CA);
                    if (descriptorCA != null) {
                        // Los descriptors son muy importantes
                        // TODO: Continue studying about descriptors in BLE
                        descriptorCA.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptorCA);
                        Log.i(TAG, "Descriptor enviado CA");
                    }
                }


            }

        }



    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        readBtnStateCharacteristic(characteristic);
    }

    private void readBtnStateCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (UUID_CHARACTERISTIC_CV.equals(characteristic.getUuid())) {
            byte[] data = characteristic.getValue();

            processCadenceData(characteristic);

        }

    }
    public  double truncate(double number, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.floor(number * factor) / factor;
    }
    private void processCadenceData(BluetoothGattCharacteristic characteristic) {
        // El formato de los datos depende del perfil CSC y del dispositivo específico
        // A continuación se muestra un ejemplo básico de cómo interpretar los datos

        // Ejemplo de estructura de datos:
        // - Flags (1 byte)
        // - Cumulative Crank Revolutions (2 bytes)
        // - Last Crank Event Time (2 bytes)
        float vs = characteristic.getIntValue(characteristic.FORMAT_UINT16, 3);
        float vs5 = characteristic.getIntValue(characteristic.FORMAT_UINT8, 4);

        float tiempoactualCA = vs;
        float vueltaactualCA = vs5;
        //Log.i("vs",""+ vs);
        //Log.i("vs5",""+ vs5);

        vueltasTotalCA = vueltaactualCA - vueltaanteriorCA;

        if (vueltasTotalCA > 0 ){
            Log.i("diferencia tiempo",""+ ((tiempoactualCA - tiempoanteriorCA)/1000));
            Log.i("diferencia vueltas",""+ vueltasTotalCA);
            cadenciaTotal = 60/((tiempoactualCA - tiempoanteriorCA)/1000);
        }



            txtCadencia.setText(""+truncate( cadenciaTotal,0));
        vueltaanteriorCA = vueltaactualCA;
        tiempoanteriorCA = tiempoactualCA;

    }

};


//CADENCIA FIN



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Inicializa los sensores
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (accelerometer != null && magnetometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                // El dispositivo no tiene uno de los sensores necesarios
            }
        }
        textVelocidad = (TextView) findViewById(R.id.t_velocidad);
       // imageSpeedometer = (ImageSpeedometer) findViewById(R.id.imageSpeedometer);
        //imageSpeedometer.setImageSpeedometer(R.drawable.image_speedometer);
        progressiveGauge= (ProgressiveGauge) findViewById(R.id.progressiveGauge);
        progressiveGauge.setTextColor(Color.TRANSPARENT);
        //imageSpeedometer.setTextColor(Color.RED);
        //imageSpeedometer.setTextSize(5);
        progressiveGauge.setMinMaxSpeed(20,190);
        textPulsos = (TextView) findViewById(R.id.t_pulsacion);
        textNegativo = (TextView) findViewById(R.id.t_negativo);
        textPositivo = (TextView) findViewById(R.id.t_positivo);
        txtDistancia = (TextView) findViewById(R.id.t_distancia);
        txtCadencia = (TextView) findViewById(R.id.t_cadencia);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        distanciaTotal = 0.0;
        bConexion = (Button) findViewById(R.id.b_conexion);
        bInicio = (Button) findViewById(R.id.b_inicio);
        bParametros = (Button) findViewById(R.id.bt_configuracion);
        textZona = (TextView) findViewById(R.id.t_zona);
        textVelocidad = (TextView) findViewById(R.id.t_velocidad);
        txtMensaje = (TextView) findViewById(R.id.t_mensaje);

        txtNombre = (TextView)  findViewById(R.id.t_nombre);
        txtEdad =  (TextView)  findViewById(R.id.t_edad);
        txtDiametro = (TextView)  findViewById(R.id.t_diametro);
        txtUidad = (TextView)  findViewById(R.id.t_unidad);
        etNombre = (EditText) findViewById(R.id.et_nombre);
        etEdad = (EditText) findViewById(R.id.et_edad);
        etDiametro = (EditText) findViewById(R.id.et_diametro);
        etPeso = (EditText) findViewById(R.id.et_peso);
        txtPeso =  (TextView)  findViewById(R.id.t_peso);

        zonaMaxima = 220 - Integer.parseInt(etEdad.getText().toString());
        ////MQTTT
       /* MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(passWord.toCharArray());

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, clientId);

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG1, "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG1, "topic: " + topic + ", msg: " + new String(message.getPayload()));
                txtMensaje.setText(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG1, "msg delivered");
            }
        });

        try {
            mqttAndroidClient.connect(mqttConnectOptions,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succeed");
                    subscribeTopic(SUB_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
*/
        //FIN MQTT

        bInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bInicio.getText().toString().compareTo("Iniciar") == 0) {
                    bInicio.setText("Finalizar");
                    grabar_nuevo("INICIO");
                //    subscribeTopic("mensajeBike");
                } else
                    bInicio.setText("Iniciar");

                ejecutar();
                ejecutarPotencia();
                desnivelNegativo = 0.0;
                desnivelPositivo = 0.0;
                txtDistancia.setText(0.0 + " km");
                textPositivo.setText(0.0 + "");
                textNegativo.setText(0.0 + "");

               /* try {
                    mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.i(TAG, "connect succeed");
                            subscribeTopic(SUB_TOPIC);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.i(TAG, "connect failed");
                        }
                    });

                } catch (MqttException e) {
                    e.printStackTrace();
                }
                */

            }
        });
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            checkAndRequestBluetoothPermissions();
        }
        FILE_NAME = FILE_NAME + obtenerFechaConFormato("dd_MM_yyyy")+".txt";
        startClient();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 7000, 0,  this);

        this.actualizaAltimetria(null);
        bConexion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startClient();
               /* try {
                    mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.i(TAG, "connect succeed");
                            subscribeTopic(SUB_TOPIC);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.i(TAG, "connect failed");
                        }
                    });

                } catch (MqttException e) {
                    e.printStackTrace();
                }*/
            }
        });
        bParametros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "CLIC");
                zonaMaxima = 220 - Integer.parseInt(etEdad.getText().toString());
                if (bInicio.getVisibility() == View.VISIBLE){
                    bInicio.setVisibility(View.INVISIBLE);
                    bConexion.setVisibility(View.INVISIBLE);
                    textVelocidad.setVisibility(View.INVISIBLE);
                    progressiveGauge.setVisibility(View.INVISIBLE);
                    textPulsos.setVisibility(View.INVISIBLE);
                    textNegativo.setVisibility(View.INVISIBLE);
                    textPositivo.setVisibility(View.INVISIBLE);
                    txtDistancia.setVisibility(View.INVISIBLE);
                    textZona.setVisibility(View.INVISIBLE);
                    textVelocidad.setVisibility(View.INVISIBLE);
                    txtMensaje.setVisibility(View.INVISIBLE);
                    txtUidad.setVisibility(View.INVISIBLE);
                    txtCadencia.setVisibility(View.INVISIBLE);

                    txtNombre.setVisibility(View.VISIBLE);
                    txtEdad.setVisibility(View.VISIBLE);
                    txtDiametro.setVisibility(View.VISIBLE);
                    etNombre.setVisibility(View.VISIBLE);
                    etEdad.setVisibility(View.VISIBLE);
                    etDiametro.setVisibility(View.VISIBLE);
                    bParametros.setBackgroundColor(Color.RED);
                    etPeso.setVisibility(View.VISIBLE);
                    txtPeso.setVisibility(View.VISIBLE);

                    //textAltitud.setBackgroundColor(Color.BLACK);
                    //textAltitud.setVisibility(View.INVISIBLE);
                    // txtLongitud.setVisibility(View.INVISIBLE);
                    // txtLatitud.setVisibility(View.INVISIBLE);
                } else {
                    bInicio.setVisibility(View.VISIBLE);
                    bConexion.setVisibility(View.VISIBLE);
                    textVelocidad.setVisibility(View.VISIBLE);
                    progressiveGauge.setVisibility(View.VISIBLE);
                    textPulsos.setVisibility(View.VISIBLE);
                    textNegativo.setVisibility(View.VISIBLE);
                    textPositivo.setVisibility(View.VISIBLE);
                    txtDistancia.setVisibility(View.VISIBLE);
                    textZona.setVisibility(View.VISIBLE);
                    textVelocidad.setVisibility(View.VISIBLE);
                    txtMensaje.setVisibility(View.VISIBLE);
                    txtUidad.setVisibility(View.VISIBLE);
                    txtCadencia.setVisibility(View.VISIBLE);
                    bParametros.setBackgroundColor(Color.parseColor("#FF018786"));
                    //textAltitud.setBackgroundColor(Color.WHITE);
                    // txtLongitud.setVisibility(View.INVISIBLE);
                    // txtLatitud.setVisibility(View.INVISIBLE);

                    txtNombre.setVisibility(View.INVISIBLE);
                    txtEdad.setVisibility(View.INVISIBLE);
                    txtDiametro.setVisibility(View.INVISIBLE);
                    etNombre.setVisibility(View.INVISIBLE);
                    etEdad.setVisibility(View.INVISIBLE);
                    etDiametro.setVisibility(View.INVISIBLE);
                    etPeso.setVisibility(View.INVISIBLE);
                    txtPeso.setVisibility(View.INVISIBLE);
                }


            }
        });

    }
    public void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG1, "subscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG1, "subscribed failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Desregistrar el listener del giroscopio para ahorrar batería
        sensorManager.unregisterListener(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        // Verificar si mBluetoothGatt está inicializado antes de usarlo
        if (mBluetoothGatt != null) {
            mBluetoothGatt.discoverServices();
        } else {
            // Si no está conectado, intentar reconectar
            startClient();
        }
        // Volver a registrar los listeners de los sensores
        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClient();

    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        // just UI topics
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Log.w(TAG, "Bluetooth enabled");
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();

            }
            else {
                // Si no se pudo conectar

                Toast.makeText(this, "Bluetooth not enabled, closing app...", Toast.LENGTH_SHORT).show();
                // TODO: Catch exceptions if bluetooth is not available on device.
            }
        }
    }
    public void calculaPotencia() {

        double angleInDegrees = 5.0; // Ángulo de la pendiente en grados
        double angleInRadians = pendiente;

        double mass =  Float.parseFloat(etPeso.getText().toString()); // Masa total en kg (bicicleta + ciclista)
        double g = 9.81; // Aceleración debido a la gravedad en m/s^2
        double force = mass * g * Math.sin(angleInRadians);

        double speedInKmh = velocidadTotal; // Velocidad en km/h
        double speedInMs = speedInKmh / 3.6; // Convertir a m/s

        double power = force * speedInMs; // Potencia en watts

        Formatter fmtd = new Formatter(new StringBuilder());
        fmtd.format(Locale.US, "%5.2f", power);
        String strdistanciaTotal = fmtd.toString();
        txtMensaje.setText(strdistanciaTotal);
        distanciaAnteriorP = distanciaActualP;
        desnivelAnteriorP = desnivelActualP ;


    }

    @SuppressLint("MissingPermission")
    public void startClient() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                checkAndRequestBluetoothPermissions();
                return;
            }
        }

        try {
            // Conectar sensor de ritmo cardíaco
            BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
            }
            mBluetoothGatt = bluetoothDevice.connectGatt(this, true, mGattCallback);
            if (mBluetoothGatt == null) {
                Log.w(TAG, "No se conecta al GATT Corazon");
                Toast.makeText(this, "No se puede conectar a " + MAC_ADDRESS, Toast.LENGTH_SHORT).show();
                return;
            }

            // Conectar sensor de velocidad
            BluetoothDevice bluetoothDeviceCV = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS_CV);
            if (mBluetoothGattCV != null) {
                mBluetoothGattCV.close();
            }
            mBluetoothGattCV = bluetoothDeviceCV.connectGatt(this, true, mGattCallbackCV);
            if (mBluetoothGattCV == null) {
                Log.w(TAG, "No se Conecta al GATT client CV");
                Toast.makeText(this, "No se puede conectar a " + MAC_ADDRESS_CV, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Conectando dispositivos...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al conectar: " + e.getMessage());
            Toast.makeText(this, "Error al conectar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @SuppressLint("MissingPermission")
    public void stopClient() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = null;
        }
        if (mBluetoothGattCV != null) {
            mBluetoothGattCV.close();
            mBluetoothGattCV = null;
        }
        if (mBluetoothGattCA != null) {
            mBluetoothGattCA.close();
            mBluetoothGattCA = null;
        }

    }
//////Archivos

    public void grabar_nuevo( String ls_valor) {
    checkExternalStoragePermission();
       // Log.e("PTO",FILE_NAME);
        File file = new File(getExternalFilesDir(null),FILE_NAME);
        file.setWritable(true);

        if(file.exists()){
            //Existe archivo

            if (ls_valor.compareTo("INICIO") == 0)
                file.delete();


          //  Log.e("PTO","Archivo Nuevo");
            grabar(file,ls_valor);
        }else{
            try {
                //Crea archivo
                file.createNewFile();
                Log.e("PTO","Archivo creado");
            } catch (IOException e) {
                Log.e("PTO",e.toString());
                e.printStackTrace();
            }
        }

}
    private void checkExternalStoragePermission() {
        int permissionCheck = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionCheck = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.i("Mensaje", "No se tiene permiso para leer.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 225);
        } else {
          //  Log.i("Mensaje", "Se tiene permiso para leer!");
        }
    }
    public void grabar(File file,String valor) {
        String ls_valor = null;
        String ls_zona = null;
        String ls_velocidad = null;
        ls_zona = (String) textZona.getText();
        ls_velocidad = (String) textVelocidad.getText();

        if ((ls_zona.compareTo("1.0") == 0) && (ls_velocidad.trim().compareTo("0.0") == 0))
            return;
        textAltitud = (TextView) this.findViewById(R.id.t_altitud);
        txtLongitud = (TextView) this.findViewById(R.id.t_longitud);
        txtLatitud = (TextView) this.findViewById(R.id.t_latitud);

        ls_valor = (String) textZona.getText() + " "+ textPulsos.getText()+ " "+textVelocidad.getText();
        ls_valor += textAltitud.getText() +" "+ txtDistancia.getText();
        ls_valor += " " +txtLatitud.getText() + " " +txtLongitud.getText();
        ls_valor += " " + txtCadencia.getText() + " " + etEdad.getText() + " " +etNombre.getText() + " "+ etDiametro.getText();
        ls_valor += " " + txtMensaje.getText();
    try {
        OutputStreamWriter archivo = new OutputStreamWriter(new FileOutputStream(file,true));
        archivo.write(ls_valor +" "+ obtenerFechaConFormato("dd/MM/yyyy HH:mm:ss")+"\n");
        archivo.flush();
        archivo.close();
    } catch (IOException e) {

    }

}
    private void ejecutar(){
        final Handler handler= new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                grabar_nuevo("");//llamamos nuestro metodo
                handler.postDelayed(this,2000);//se ejecutara cada 1 segundos
            }
        },1000);//empezara a ejecutarse después de 5 milisegundos
    }
    private void ejecutarPotencia(){
        final Handler handler= new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                calculaPotencia();//llamamos nuestro metodo

                handler.postDelayed(this,5000);//se ejecutara cada 5 segundos
            }
        },1000);//empezara a ejecutarse después de 5 milisegundos
    }
    @SuppressLint("SimpleDateFormat")
    public static String obtenerFechaConFormato(String formato) {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat(formato);

        return sdf.format(date);
    }
    /////////////VELOCIDAD
    public void finish() {
        super.finish();
        System.exit(0);
    }
    //////Inicio-Altimetria/////
    private void actualizaAltimetria(CLocation location) {
         double naltitud =0;
         double longitud = 0;
        double latitud = 0;

        if(location != null)
        { location.setUseMetricunits(this.useMetricUnits());
            naltitud = location.getAltitude();
            longitud = location.getLongitude();
            latitud = location.getLatitude();

        }
        TextView txtAltitud = (TextView) this.findViewById(R.id.t_altitud);
        String strUnits = "meters/second";
        if(this.useMetricUnits()) { strUnits = "meters/second"; }
        Formatter fma = new Formatter(new StringBuilder());
        fma.format(Locale.US, "%6.0f", naltitud);
        String strnaltitud = fma.toString();
        txtAltitud.setText(strnaltitud);

        if (lecturasAltimetria <= 2 ){
            sumaAltimetrias += naltitud;
            lecturasAltimetria++;
        } else{
            lecturasAltimetria = 1;
            sumaAltimetrias = sumaAltimetrias/2;

            naltitud = sumaAltimetrias;
            if (naltitud > altitudAnterior) {
                if (altitudAnterior == 0){
                    desnivelPositivo = 0;
                }
                else{
                    if (abs(naltitud - altitudAnterior) <= 15) {
                        desnivelAnteriorP = desnivelPositivo;
                        desnivelPositivo = desnivelPositivo + (naltitud - altitudAnterior);
                        desnivelActualP = desnivelPositivo;
                    }
                }
                Formatter fmp = new Formatter(new StringBuilder());
                fmp.format(Locale.US, "%6.0f", desnivelPositivo);
                String strPositivo = fmp.toString();

                textPositivo.setText(strPositivo);

            } else{
                if (abs(naltitud - altitudAnterior) <= 15)
                    desnivelNegativo = desnivelNegativo +  ( altitudAnterior - naltitud);
                Formatter fmn = new Formatter(new StringBuilder());
                fmn.format(Locale.US, "%6.0f", desnivelNegativo);
                String strNegativo = fmn.toString();
                textNegativo.setText(strNegativo);

            }
            altitudAnterior=  naltitud;
            sumaAltimetrias = 0.0;

        }


        TextView txtLongitud = (TextView) this.findViewById(R.id.t_longitud);
        TextView txtLatitud = (TextView) this.findViewById(R.id.t_latitud);
        txtLongitud.setText(Double.toString(longitud));
        txtLatitud.setText(Double.toString(latitud));




    }
    //////Fin-Altimetria////


    private boolean useMetricUnits() {
        //CheckBox chkUseMetricUnits = (CheckBox) this.findViewById(R.id.chkMetricUnits);
        return true;//chkUseMetricUnits.isChecked();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            CLocation myLocation = new CLocation(location, true);

            this.actualizaAltimetria(myLocation);
        }
    }
    @Override
    public void onProviderDisabled(String provider) { }
    @Override
    public void onProviderEnabled(String provider) { }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){  }
    @Override
    public void onGpsStatusChanged(int event) { }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                // Los ángulos están en radianes, conviértelos a grados
                float azimuth = (float) Math.toDegrees(orientation[0]); // Rotación alrededor del eje Z
                float pitch = (float) Math.toDegrees(orientation[1]); // Inclinación hacia adelante o atrás (eje X)
                float roll = (float) Math.toDegrees(orientation[2]); // Inclinación hacia los lados (eje Y)


                if (numeroLecturas == 50 ) {
                    pendiente = (-1)*(anguloAcumulado/50 -7);
                    pendiente = (float) Math.toRadians(pendiente);

                    txtCadencia.setText("" + (int) ((-1)*anguloAcumulado/50 -7) );
                    numeroLecturas = 1;
                    anguloAcumulado = 0;
                } else {
                    anguloAcumulado += roll;
                    numeroLecturas++;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private static final int PERMISSION_REQUEST_CODE = 123;

    private void checkAndRequestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                }, PERMISSION_REQUEST_CODE);
            } else {
                startClient();
            }
        } else {
            startClient();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startClient();
            } else {
                Toast.makeText(this, "Se requieren permisos de Bluetooth para usar la aplicación", Toast.LENGTH_LONG).show();
            }
        }
    }
}