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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
    //----------------------Sensor del Corazón--------------------------------
    //MAC de la banda anterior
    public static final String MAC_ADDRESS = "E0:13:76:E8:AE:C7";
    //MAC de la banda nueva
    //public static final String MAC_ADDRESS = "E8:3E:7C:69:50:CF";
    //UUID del servicio
    public static final UUID UUID_SERVICE = UUID.fromString("00000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CORAZON = UUID.fromString("000002a37-0000-1000-8000-00805f9b34fb");
    //----------------------------------------------------------
    //UUID del descriptor
    public static final UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //----------------------------------------------------------
    //Sensor de Cadencia & Velocidad
    //LEZYNE
    // public static final String MAC_ADDRESS_CV = "D7:08:28:30:DC:53";'¿
    //----------------------------------------------------------
    //CYCPLUS VELOCIDAD
    public static final String MAC_ADDRESS_CV = "FE:2A:38:59:02:B5";
    //----------------------------------------------------------
    //CYCPLUS CADENCIA
    public static final String MAC_ADDRESS_CA = "D7:08:28:30:DC:53";

    //MAGENE VELOCIDAD
    //public static final String MAC_ADDRESS_CV = "DD:EF:3F:E3:8A:98";
    //----------------------------------------------------------
    //MAGENE CADENCIA
    //public static final String MAC_ADDRESS_CA = "D7:CD:93:0B:0E:75";

    //----------------------------------------------------------
    //UUID del servicio
    public static final UUID UUID_SERVICE_CV = UUID.fromString("000001816-0000-1000-8000-00805f9b34fb");
    //UUID del característica
    public static final UUID UUID_CHARACTERISTIC_CV = UUID.fromString("000002a5b-0000-1000-8000-00805f9b34fb");
    //----------------------------------------------------------
    //UUID del servicio
    public static final UUID UUID_SERVICE_CA = UUID.fromString("000001816-0000-1000-8000-00805f9b34fb");
    //UUID del característica
    public static final UUID UUID_CHARACTERISTIC_CA = UUID.fromString("000002a5b-0000-1000-8000-00805f9b34fb");
    //----------------------------------------------------------
    public static BluetoothManager mBluetoothManager;
    //----------------------------------------------------------
    public static BluetoothAdapter mBluetoothAdapter;
    //----------------------------------------------------------
    public static BluetoothGatt mBluetoothGatt;
    //----------------------------------------------------------
    public static BluetoothGatt mBluetoothGattCV;
    //----------------------------------------------------------
    public static BluetoothGatt mBluetoothGattCA;
    //----------------------------------------------------------
    public String TAG = "PTO", FILE_NAME = "bike";
    //----------------------------------------------------------
    private String deviceMacAddress;
    //----------------------------------------------------------
    private static final int PERMISSION_REQUEST_CODE = 1;
    //----------------------------------------------------------

    //Variables de la interfaz
    TextView textPulsos, textVelocidad, textNegativo,textPositivo,txtDistancia, textZona, textAltitud, txtCadencia, txtPeso;
    TextView txtLongitud,txtLatitud,txtMensaje, txtNombre, txtEdad, txtDiametro, txtUidad;
    EditText etNombre, etEdad, etDiametro,etPeso;
    Button bConexion, bInicio, bParametros;
    //----------------------------------------------------------
    //Variables de la interfaz
    int tiempoactual= 0, tiempoanterior= 0, vueltaactual=0, vueltaanterior=0,vueltasTotal, lecturasAltimetria = 1;
    float tiempoactualCA= 0, tiempoanteriorCA= 0, vueltaactualCA=0, vueltaanteriorCA=0,vueltasTotalCA =0,cadenciaTotal= 0;
    int zonaMaxima, zonaMinima,numeroLecturas = 1;
    //----------------------------------------------------------
    float anguloAcumulado = 0, pendiente = 0.0f;
    //----------------------------------------------------------
    double velocidadTotal, distanciaTotal = 0.0, sumaAltimetrias = 0.0;
    double desnivelPositivo = 0, desnivelNegativo = 0, altitudAnterior = 0;
    double desnivelAnteriorP = 0, desnivelActualP = 0, distanciaAnteriorP = 0,distanciaActualP = 0;
    ProgressiveGauge progressiveGauge;
    ////MQTT
    //----------------------------------------------------------
    private final String TAG1 = "AiotMqtt";

    final String host = "tcp://186.4.224.175:1883";
    private String clientId = "SensorBike" + System.currentTimeMillis();
    private String userName="pavel";
    private String passWord="2008P4P3luch0";
    private String SUB_TOPIC ="mensajeBike";
    //----------------------------------------------------------
    private MqttAndroidClient mqttAndroidClient;
    private MQTTHandler mqttHandler;

    ////Sensor de Giroscopio
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    ////end MQTT
    private boolean mqttEnvioActivo = false;
    //----------------------------------------------------------
    //Función para conectar el sensor del corazón, velocidad y cadencia
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
                    }
                });
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }

        //----------------------------------------------------------
        //Sirve para conectar el sensor del corazón, mediante el servicio y la característica
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
        //----------------------------------------------------------
        //Lee el sensor del corazón
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

        }
        //----------------------------------------------------------
        //Cambia el sensor del corazón
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            readBtnStateCharacteristic(characteristic);
        }
        //----------------------------------------------------------
        //Función para calcular la zona de pulsos
        private void readBtnStateCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (UUID_CHARACTERISTIC_CORAZON.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                int vs = characteristic.getIntValue(characteristic.FORMAT_UINT8, 1);
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
                if (vs > zonaMaxima){
                    subZona = (vs-zonaMaxima)*10/17;
                    textoZona = "5." + subZona + "";
                    colorZona = Color.RED;
                }

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
        //----------------------------------------------------------
        //Función para conectar el sensor de velocidad
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

        //----------------------------------------------------------
        //Sirve para conectar el sensor de velocidad, mediante el servicio y la característica
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
        //----------------------------------------------------------
        //Función para leer el sensor de velocidad
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
                //----------------------------------------------------------
                //Función para calcular la velocidad
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

    private long lastCadenceTime = 0;

    private int lastCadenceCrankRevolutions = 0;
    private float currentCadence = 0;
    private static final long CADENCE_TIMEOUT = 2000; // 2 segundos de timeout

    private BluetoothGattCallback mGattCallbackCA = new BluetoothGattCallback() {

        private final String TAG = "mGattCallbackCA";

        //----------------------------------------------------------
        //Función para conectar el sensor de cadencia
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, status + " " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Sensor de cadencia conectado", Toast.LENGTH_SHORT).show();
                    }
                });
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Sensor de cadencia desconectado", Toast.LENGTH_SHORT).show();
                        txtCadencia.setText("0");
                    }
                });
            }
        }

        //----------------------------------------------------------
        //Sirve para conectar el sensor de cadencia, mediante el servicio y la característica
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
                        BluetoothGattDescriptor descriptorCA = characteristicCA.getDescriptor(UUID_DESCRIPTOR);
                        if (descriptorCA != null) {
                            descriptorCA.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptorCA);
                            Log.i(TAG, "Descriptor enviado CA");
                        }
                    }


                }

            }


        }

        // Variables para el timeout de cadencia
        private static final long MAX_IDLE_TIME_MS = 3000; // 3 segundos de inactividad permitida

        private long lastRealMovementTimestamp = 0;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (UUID_CHARACTERISTIC_CA.equals(characteristic.getUuid())) {
                processCadenceData(characteristic);
            }
        }
        //----------------------------------------------------------
        //Función para procesar los datos de la cadencia

        /*private void processCadenceData(BluetoothGattCharacteristic characteristic) {
            //long currentTime = System.currentTimeMillis();
            int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            int lastRevolutionTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);

            if (lastCadenceTime > 0) {
                long timeDiff = lastRevolutionTime - lastCadenceTime;

                if (timeDiff > 0) {
                    int revolutionDiff = crankRevolutions - lastCadenceCrankRevolutions;

                    if (revolutionDiff > 0) {
                        // Calcular RPM: (revoluciones * 60) / tiempo en segundos
                        currentCadence = (float) (revolutionDiff * 60.0 / (timeDiff / 1000.0));

                        // Limitar la cadencia a un rango razonable (0-200 RPM)
                        currentCadence = Math.min(Math.max(currentCadence, 0), 200);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String cadenceText = String.format(Locale.US, "%d", (int) currentCadence);
                                txtCadencia.setText(cadenceText);

                                // Colorear la cadencia según el rango
                                int cadenceColor;
                                if (currentCadence < 60) {
                                    cadenceColor = Color.RED; // 🔴 Baja, resistencia de fuerza
                                } else if (currentCadence >= 60 && currentCadence < 80) {
                                    cadenceColor = Color.rgb(255, 165, 0); // 🟠 Moderada, subida con esfuerzo
                                } else if (currentCadence >= 80 && currentCadence <= 100) {
                                    cadenceColor = Color.GREEN; // 🟢 Óptima para rendimiento
                                } else {
                                    cadenceColor = Color.BLUE; // 🔵 Alta, sprint o cadencia excesiva
                                }
                                txtCadencia.setTextColor(cadenceColor);
                            }
                        });
                    }
                } else {
                    // Reiniciar cadencia si no hay actualizaciones
                    currentCadence = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtCadencia.setText("0");
                            txtCadencia.setTextColor(Color.GRAY); // Color gris cuando no hay datos
                        }
                    });
                }
            }

            lastCadenceTime = lastRevolutionTime;
            lastCadenceCrankRevolutions = crankRevolutions;
        }

    };*/
        private void processCadenceData(BluetoothGattCharacteristic characteristic) {
            try {
                int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                int lastRevolutionTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);

                if (lastCadenceTime > 0) {
                    long timeDiff = lastRevolutionTime - lastCadenceTime;
                    if (timeDiff < 0) {
                        timeDiff += 65536; // Desbordamiento
                    }

                    int revolutionDiff = crankRevolutions - lastCadenceCrankRevolutions;
                    if (revolutionDiff < 0) {
                        revolutionDiff += 65536; // Desbordamiento
                    }

                    if (revolutionDiff > 0 && timeDiff > 0) {
                        // Hay movimiento real
                        currentCadence = (float) (revolutionDiff * 60000.0 / timeDiff);
                        currentCadence = Math.min(Math.max(currentCadence, 0), 200);

                        lastRealMovementTimestamp = System.currentTimeMillis(); // Actualizamos el último movimiento

                        updateCadenceUI(currentCadence);
                    } else {
                        // No hay nuevo movimiento
                        long now = System.currentTimeMillis();
                        if (now - lastRealMovementTimestamp > MAX_IDLE_TIME_MS) {
                            // Si han pasado más de 3 segundos sin movimiento, ponemos 0
                            currentCadence = 0;
                            updateCadenceUI(currentCadence);
                        }
                        // Si no, seguimos mostrando la última cadencia medida
                    }
                } else {
                    // Primera lectura
                    currentCadence = 0;
                    lastRealMovementTimestamp = System.currentTimeMillis();
                    updateCadenceUI(currentCadence);
                }

                lastCadenceTime = lastRevolutionTime;
                lastCadenceCrankRevolutions = crankRevolutions;

            } catch (Exception e) {
                Log.e(TAG, "Error procesando datos de cadencia: " + e.getMessage());
                runOnUiThread(() -> {
                    txtCadencia.setText("0");
                    txtCadencia.setTextColor(Color.GRAY);
                });
            }
        }

        private void updateCadenceUI(float cadence) {
            runOnUiThread(() -> {
                String cadenceText = String.format(Locale.US, "%d", (int) cadence);
                txtCadencia.setText(cadenceText);

                int cadenceColor;
                if (cadence == 0) {
                    cadenceColor = Color.GRAY;
                } else if (cadence < 60) {
                    cadenceColor = Color.RED;
                } else if (cadence < 80) {
                    cadenceColor = Color.rgb(255, 165, 0);
                } else if (cadence <= 100) {
                    cadenceColor = Color.GREEN;
                } else {
                    cadenceColor = Color.BLUE;
                }
                txtCadencia.setTextColor(cadenceColor);
            });
        }
    };

    //CADENCIA FIN
    //----------------------------------------------------------
    //Función para conectar el sensor de cadencia
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
        //----------------------------------------------------------
        //Variables para el txt
        textVelocidad = (TextView) findViewById(R.id.t_velocidad);

        progressiveGauge= (ProgressiveGauge) findViewById(R.id.progressiveGauge);
        progressiveGauge.setTextColor(Color.TRANSPARENT);

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

        //FIN MQTT
        //----------------------------------------------------------
        bInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bInicio.getText().toString().compareTo("Iniciar") == 0) {
                    bInicio.setText("Finalizar");
                    grabar_nuevo("INICIO");
                    mqttEnvioActivo = true;
                    isRecording = true;
                } else {
                    bInicio.setText("Iniciar");
                    mqttEnvioActivo = false;
                    isRecording = false;
                    if (recordingHandler != null) {
                        recordingHandler.removeCallbacksAndMessages(null);
                    }

                    // Enviar mensaje de finalización MQTT
                    try {
                        String jsonFinalizar = String.format(
                                "{\"tipo\":\"finalizar\",\"mac_dispositivo\":\"%s\",\"fecha_fin\":\"%s\"}",
                                getDeviceMacAddress(),
                                obtenerFechaConFormato("yyyy-MM-dd HH:mm:ss")
                        );
                        mqttHandler.publishMessage(jsonFinalizar);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al enviar mensaje de finalización: " + e.getMessage());
                    }
                }

                ejecutar();
                ejecutarPotencia();
                desnivelNegativo = 0.0;
                desnivelPositivo = 0.0;
                txtDistancia.setText(0.0 + " km");
                textPositivo.setText(0.0 + "");
                textNegativo.setText(0.0 + "");
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

        // Inicializar MQTTHandler
        mqttHandler = new MQTTHandler(this, SUB_TOPIC);
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
        if (mqttHandler != null) {
            mqttHandler.disconnect();
        }
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
        try {
            // Obtener la cadencia en RPM
            float cadencia = 0;
            try {
                cadencia = Float.parseFloat(txtCadencia.getText().toString());
            } catch (NumberFormatException e) {
                cadencia = 0;
            }

            // Obtener la masa total (ciclista + bicicleta)
            double masaTotal = Double.parseDouble(etPeso.getText().toString());

            // Obtener la aceleración del acelerómetro
            float[] aceleracion = new float[3];
            if (gravity != null) {
                aceleracion = gravity.clone();
            } else {
                aceleracion = new float[]{0, 0, 0};
            }

            // Usar la componente Y del acelerómetro como aceleración relevante
            double aceleracionRelevante = Math.abs(aceleracion[1]);

            // Longitud de la biela (metros)
            double longitudBiela = 0.175;

            // Fuerza = masa total * aceleración
            double fuerza = masaTotal * aceleracionRelevante;

            // Torque = fuerza * longitud de la biela
            double torque = fuerza * longitudBiela;

            // Velocidad angular (radianes/segundo)
            double velocidadAngular = (cadencia * 2 * Math.PI) / 60.0;

            // Potencia = torque * velocidad angular
            double potencia = torque * velocidadAngular;
            potencia = Math.max(0, potencia); // No negativa

            // Mostrar resultado
            final String strPower = String.format(Locale.US, "%.2f", potencia);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (txtMensaje != null) {
                        txtMensaje.setText(strPower);
                        txtMensaje.setTextColor(Color.WHITE);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al calcular potencia: " + e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (txtMensaje != null) {
                        txtMensaje.setText("0.00");
                        txtMensaje.setTextColor(Color.WHITE);
                    }
                }
            });
        }
    }

   /* public void calculaPotencia() {
        try {
            // Obtener la cadencia en RPM
            float cadencia = 0;
            try {
                cadencia = Float.parseFloat(txtCadencia.getText().toString());
            } catch (NumberFormatException e) {
                cadencia = 0;
            }

            // Obtener la masa del ciclista
            double masaTotal = Double.parseDouble(etPeso.getText().toString());

            // Calcular la masa efectiva de la pierna (17% del peso total)
            double masaPierna = masaTotal * 0.17;

            // Obtener la aceleración del acelerómetro
            float[] aceleracion = new float[3];
            if (gravity != null) {
                aceleracion = gravity.clone();
            } else {
                aceleracion = new float[]{0, 0, 0};
            }

            // Calcular la aceleración perpendicular al pedaleo
            // Usamos la componente Y del acelerómetro que es perpendicular al pedaleo
            double aceleracionPerpendicular = Math.abs(aceleracion[1]);

            // Longitud de la biela en metros (típicamente 0.17-0.18m)
            double longitudBiela = 0.175;

            // Calcular la fuerza aplicada (F = m * a)
            double fuerza = masaPierna * aceleracionPerpendicular;

            // Calcular el torque (T = F * L)
            double torque = fuerza * longitudBiela;

            // Calcular la velocidad angular (ω = (Cadencia * 2π) / 60)
            double velocidadAngular = (cadencia * 2 * Math.PI) / 60.0;

            // Calcular la potencia (P = T * ω)
            double potencia = torque * velocidadAngular;

            // Asegurarse de que la potencia no sea negativa
            potencia = Math.max(0, potencia);

            // Formatear el valor de potencia y mostrarlo en el TextView
            final String strPower = String.format(Locale.US, "%.2f", potencia);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (txtMensaje != null) {
                        txtMensaje.setText(strPower);
                        txtMensaje.setTextColor(Color.WHITE);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al calcular potencia: " + e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (txtMensaje != null) {
                        txtMensaje.setText("0.00");
                        txtMensaje.setTextColor(Color.WHITE);
                    }
                }
            });
        }
    }*/

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

            // Conectar sensor de cadencia
            BluetoothDevice bluetoothDeviceCA = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS_CA);
            if (mBluetoothGattCA != null) {
                mBluetoothGattCA.close();
            }
            mBluetoothGattCA = bluetoothDeviceCA.connectGatt(this, true, mGattCallbackCA);
            if (mBluetoothGattCA == null) {
                Log.w(TAG, "No se Conecta al GATT client CA");
                Toast.makeText(this, "No se puede conectar a " + MAC_ADDRESS_CA, Toast.LENGTH_SHORT).show();
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

    private boolean isRecording = false;
    private Handler recordingHandler = null;

    private void ejecutar() {
        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }

        isRecording = true;
        recordingHandler = new Handler();

        recordingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    try {
                        grabar_nuevo(""); // llamamos nuestro metodo
                    } catch (Exception e) {
                        Log.e(TAG, "Error al grabar datos: " + e.getMessage());
                    }
                    recordingHandler.postDelayed(this, 1000); // se ejecutara cada 1 segundo
                }
            }
        }, 1000); // empezara a ejecutarse después de 1 segundo
    }

    public void grabar_nuevo(String ls_valor) {
        if (!isRecording && !"INICIO".equals(ls_valor)) {
            return;
        }

        checkExternalStoragePermission();
        File file = new File(getExternalFilesDir(null), FILE_NAME);
        file.setWritable(true);

        if (file.exists()) {
            if ("INICIO".equals(ls_valor)) {
                file.delete();
            }
            grabar(file, ls_valor);
        } else {
            try {
                file.createNewFile();
                Log.e("PTO", "Archivo creado");
            } catch (IOException e) {
                Log.e("PTO", e.toString());
                e.printStackTrace();
            }
        }
    }

    public void grabar(File file, String valor) {
        try {
            String ls_valor = null;
            String ls_zona = null;
            String ls_velocidad = null;

            // Obtener valores de manera segura
            ls_zona = textZona != null ? textZona.getText().toString() : "0";
            ls_velocidad = textVelocidad != null ? textVelocidad.getText().toString() : "0";

            // Obtener referencias a los TextViews de manera segura
            textAltitud = findViewById(R.id.t_altitud);
            txtLongitud = findViewById(R.id.t_longitud);
            txtLatitud = findViewById(R.id.t_latitud);

            // Construir la línea de datos para el archivo txt con valores por defecto
            StringBuilder sb = new StringBuilder();
            sb.append(ls_zona).append(" ");
            sb.append(textPulsos != null ? textPulsos.getText().toString() : "0").append(" ");
            sb.append(ls_velocidad).append(" ");
            sb.append(textAltitud != null ? textAltitud.getText().toString() : "0").append(" ");
            sb.append(txtDistancia != null ? txtDistancia.getText().toString() : "0").append(" ");
            sb.append(txtLatitud != null ? txtLatitud.getText().toString() : "0").append(" ");
            sb.append(txtLongitud != null ? txtLongitud.getText().toString() : "0").append(" ");
            sb.append(txtCadencia != null ? txtCadencia.getText().toString() : "0").append(" ");
            sb.append(etEdad != null ? etEdad.getText().toString() : "0").append(" ");
            sb.append(etNombre != null ? etNombre.getText().toString() : "unknown").append(" ");
            sb.append(etDiametro != null ? etDiametro.getText().toString() : "0").append(" ");
            sb.append(txtMensaje != null ? txtMensaje.getText().toString() : "0").append(" ");
            sb.append(obtenerFechaConFormato("dd/MM/yyyy HH:mm:ss"));
            String desnivelPositivoStr = "0.0";
            String desnivelNegativoStr = "0.0";
            ls_valor = sb.toString();

            // Intentar enviar datos MQTT si está activo y hay conexión
            if (mqttEnvioActivo) {
                try {
                    // Enviar JSON según el tipo de mensaje
                    String jsonMessage;
                    if ("INICIO".equals(valor)) {
                        // Mensaje de inicio de sesión (tabla sesiones)
                        jsonMessage = String.format(
                                "{\"tipo\":\"sesion\",\"mac_dispositivo\":\"%s\",\"nombre_ciclista\":\"%s\"," +
                                        "\"edad_ciclista\":%s,\"diametro_rueda\":%s,\"fecha_inicio\":\"%s\"}",
                                getDeviceMacAddress(),
                                etNombre != null ? etNombre.getText().toString() : "unknown",
                                etEdad != null ? etEdad.getText().toString() : "0",
                                etDiametro != null ? etDiametro.getText().toString() : "0",
                                obtenerFechaConFormato("yyyy-MM-dd HH:mm:ss")
                        );
                    } else if ("FINALIZAR".equals(valor)) {
                        // Mensaje de finalización de sesión
                        jsonMessage = String.format(
                                "{\"tipo\":\"finalizar\",\"mac_dispositivo\":\"%s\",\"fecha_fin\":\"%s\"}",
                                getDeviceMacAddress(),
                                obtenerFechaConFormato("yyyy-MM-dd HH:mm:ss")
                        );
                    } else {
                        // Obtener el valor de cadencia de manera segura
                        String cadencia = "0";
                        if (txtCadencia != null && !txtCadencia.getText().toString().isEmpty()) {
                            try {
                                cadencia = txtCadencia.getText().toString();
                            } catch (Exception e) {
                                cadencia = "0";
                            }
                        }
                        try {
                            desnivelPositivoStr = textPositivo != null ?
                                    textPositivo.getText().toString().replaceAll("[^\\d.]", "") : "0.0"; // Eliminar caracteres no numéricos
                            desnivelNegativoStr = textNegativo != null ?
                                    textNegativo.getText().toString().replaceAll("[^\\d.]", "") : "0.0";
                        } catch (Exception e) {
                            Log.e(TAG, "Error al parsear desniveles: " + e.getMessage());
                        }

                        // Mensaje de registro normal (tabla registros)
                        jsonMessage = String.format(
                                "{\"tipo\":\"registro\",\"mac_dispositivo\":\"%s\",\"zona_esfuerzo\":\"%s\"," +
                                        "\"pulsaciones\":%s,\"velocidad\":%s,\"altitud\":%s,\"distancia\":%s," +
                                        "\"latitud\":%s,\"longitud\":%s,\"cadencia\":%s,\"edad\":%s," +
                                        "\"nombre\":\"%s\",\"diametro\":%s,\"potencia\":%s," +
                                        "\"desnivel_positivo\":%s,\"desnivel_negativo\":%s," +
                                        "\"fecha_hora\":\"%s\"}",
                                getDeviceMacAddress(),
                                ls_zona,
                                textPulsos != null ? textPulsos.getText().toString() : "0",
                                ls_velocidad,
                                textAltitud != null ? textAltitud.getText().toString() : "0",
                                txtDistancia != null ? txtDistancia.getText().toString().replace(" km", "") : "0",
                                txtLatitud != null ? txtLatitud.getText().toString() : "0",
                                txtLongitud != null ? txtLongitud.getText().toString() : "0",
                                cadencia,
                                etEdad != null ? etEdad.getText().toString() : "0",
                                etNombre != null ? etNombre.getText().toString() : "unknown",
                                etDiametro != null ? etDiametro.getText().toString() : "0",
                                txtMensaje != null ? txtMensaje.getText().toString() : "0",
                                desnivelPositivoStr, // Nuevo valor
                                desnivelNegativoStr, // Nuevo valor
                                obtenerFechaConFormato("yyyy-MM-dd HH:mm:ss")

                        );
                    }

                    // Enviar el mensaje MQTT
                    mqttHandler.publishMessage(jsonMessage);

                } catch (Exception e) {
                    Log.e(TAG, "Error al enviar datos MQTT: " + e.getMessage());
                }
            }

            // Escribir en el archivo (siempre se intenta, independientemente de la conexión)
            try (OutputStreamWriter archivo = new OutputStreamWriter(new FileOutputStream(file, true))) {
                archivo.write(ls_valor + "\n");
                archivo.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error al escribir en el archivo: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error general en grabar(): " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            CLocation myLocation = new CLocation(location, this.useMetricUnits());
            actualizaAltimetria(myLocation);
        }
    }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onGpsStatusChanged(int event) { }

    private boolean useMetricUnits() {
        return true;
    }

    private float[] lastGravity = null;
    private float[] lastGeomagnetic = null;
    private static final int FILTER_SIZE = 10;
    private float[] pitchBuffer = new float[FILTER_SIZE];
    private int pitchIndex = 0;

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

                // Obtener el pitch (inclinación frontal/trasera) en grados
                float pitch = (float) Math.toDegrees(orientation[1]);

                // Aplicar un filtro de media móvil
                pitchBuffer[pitchIndex] = pitch;
                pitchIndex = (pitchIndex + 1) % FILTER_SIZE;

                // Calcular el promedio
                float sumPitch = 0;
                for (float p : pitchBuffer) {
                    sumPitch += p;
                }
                float filteredPitch = sumPitch / FILTER_SIZE;

                // Convertir a radianes y ajustar el signo
                pendiente = (float) Math.toRadians(filteredPitch);

                // Calibrar si es necesario (ajustar según el dispositivo)
                if (lastGravity != null && lastGeomagnetic != null) {
                    // Detectar si el dispositivo está en una superficie plana
                    float[] flatGravity = new float[3];
                    float[] flatGeomagnetic = new float[3];
                    SensorManager.getRotationMatrix(R, I, lastGravity, lastGeomagnetic);
                    SensorManager.getOrientation(R, orientation);

                    // Si el dispositivo está en una superficie plana, usar ese valor como referencia
                    if (Math.abs(filteredPitch) < 1.0) {
                        // Calibrar el offset
                        float calibrationOffset = filteredPitch;
                        pendiente = (float) Math.toRadians(filteredPitch - calibrationOffset);
                    }
                }

                lastGravity = gravity.clone();
                lastGeomagnetic = geomagnetic.clone();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se necesita implementar
    }

    private String getDeviceMacAddress() {
        if (deviceMacAddress == null) {
            try {
                deviceMacAddress = mBluetoothAdapter.getAddress();
                if (deviceMacAddress.equals("02:00:00:00:00:00")) {
                    // En versiones más recientes de Android, la MAC real está oculta
                    deviceMacAddress = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting MAC address: " + e.getMessage());
                deviceMacAddress = "unknown";
            }
        }
        return deviceMacAddress;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void enviarDatosMQTT(String datos) {
        if (mqttHandler != null && mqttEnvioActivo && isNetworkAvailable()) {
            try {
                // Asegurarse de que el valor de potencia sea numérico
                String potenciaStr = txtMensaje.getText().toString();
                double potencia = 0.0;
                try {
                    potencia = Double.parseDouble(potenciaStr);
                } catch (NumberFormatException e) {
                    potencia = 0.0;
                }

                // Reemplazar el valor de potencia en el JSON si existe
                if (datos.contains("\"potencia\"")) {
                    datos = datos.replaceAll("\"potencia\":\"[^\"]*\"", String.format("\"potencia\":%.2f", potencia));
                }

                mqttHandler.publishMessage(datos);
            } catch (Exception e) {
                Log.e(TAG, "Error al enviar datos MQTT: " + e.getMessage());
                // No mostrar error al usuario, solo loggear
            }
        }
    }

    private void ejecutarPotencia() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                calculaPotencia(); // llamamos nuestro metodo
                handler.postDelayed(this, 3000); // se ejecutara cada 3 segundos
            }
        }, 1000); // empezara a ejecutarse después de 1 segundo
    }

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

    private void checkExternalStoragePermission() {
        int permissionCheck = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionCheck = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.i("Mensaje", "No se tiene permiso para leer.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 225);
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static String obtenerFechaConFormato(String formato) {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat(formato);
        return sdf.format(date);
    }

    private void actualizaAltimetria(CLocation location) {
        double naltitud = 0;
        double longitud = 0;
        double latitud = 0;

        TextView txtAltitud = (TextView) this.findViewById(R.id.t_altitud);
        TextView txtLongitud = (TextView) this.findViewById(R.id.t_longitud);
        TextView txtLatitud = (TextView) this.findViewById(R.id.t_latitud);

        if(location != null) {
            location.setUseMetricunits(this.useMetricUnits());
            naltitud = location.getAltitude();
            longitud = location.getLongitude();
            latitud = location.getLatitude();
        }

        Formatter fma = new Formatter(new StringBuilder());
        fma.format(Locale.US, "%6.0f", naltitud);
        String strnaltitud = fma.toString();
        txtAltitud.setText(strnaltitud);
        txtLongitud.setText(Double.toString(longitud));
        txtLatitud.setText(Double.toString(latitud));

        if (lecturasAltimetria <= 2 && location != null){
            sumaAltimetrias += naltitud;
            lecturasAltimetria++;
        } else if (location != null) {
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
            } else {
                if (abs(naltitud - altitudAnterior) <= 15)
                    desnivelNegativo = desnivelNegativo + (altitudAnterior - naltitud);
                Formatter fmn = new Formatter(new StringBuilder());
                fmn.format(Locale.US, "%6.0f", desnivelNegativo);
                String strNegativo = fmn.toString();
                textNegativo.setText(strNegativo);
            }
            altitudAnterior = naltitud;
            sumaAltimetrias = 0.0;
        }
    }
}