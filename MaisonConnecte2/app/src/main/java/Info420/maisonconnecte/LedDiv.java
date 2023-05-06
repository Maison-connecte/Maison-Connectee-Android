package Info420.maisonconnecte;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LedDiv extends AppCompatActivity {
    //Variable interrupteur
    private ToggleButton interrupteur;

    //Variables MQTT
    String pubTopic = "colordylan";
    String content;
    int qos = 2;
    String broker = "tcp://test.mosquitto.org:1883";
    String clientId = "emqx_test";
    MemoryPersistence persistence = new MemoryPersistence();

    //création d'un thread pool pour les envois MQTT
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int previousColorSum = 0;
    private final int ColorSumDifference = 20;


    //Variables couleur
    ColorPickerView colorPickerView;
    BrightnessSlideBar brightnessSlideBar;
    private SeekBar seekBarIntensite;
    private TextView intensite;

    //connection MQTT
    private MqttClient client;
    private MqttConnectOptions connOpts;

    //sauvegarde en cache la couleur pour que quand l'activité est détruite et recréer la couleur est préservé.
    private void saveSelectedColorAndBrightness(int color) {
        SharedPreferences sharedPref = getSharedPreferences("LedDivPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("selectedColor", color);
        editor.apply();
    }

    //utilise les préférences et met le color picker à la bonne couleur
    private void loadSelectedColorAndBrightness() {
        SharedPreferences sharedPref = getSharedPreferences("LedDivPrefs", Context.MODE_PRIVATE);
        int color = sharedPref.getInt("selectedColor", Color.BLACK);
        int brightness = sharedPref.getInt("selectedBrightness", 128);
        colorPickerView.setInitialColor(color);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_div);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //Interrupteur
        interrupteur = (ToggleButton) findViewById(R.id.interrupteur);

        //établissement d'une connection au serveur MQTT
        try {
            client = new MqttClient(broker, clientId, persistence);

            connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);

            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connected");

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }


        interrupteur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String contenu;

                try {
                    MqttClient client = new MqttClient(broker, clientId, persistence);

                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    //connOpts.setUserName("rw");
                    //connOpts.setPassword("readwrite".toCharArray());
                    connOpts.setCleanSession(true);
                    if(interrupteur.isChecked()){
                        contenu="1";
                    }
                    else{
                        contenu="0";
                    }

                    //Établir connexion
                    System.out.println("Connecting to broker: " + broker);
                    client.connect(connOpts);

                    System.out.println("Connected");
                    System.out.println("Publishing message: " + contenu);

                    //Ajout du contenu du message
                    MqttMessage message = new MqttMessage(contenu.getBytes());
                    message.setQos(qos);
                    client.publish("enable", message);
                    System.out.println("Message published");

                    client.disconnect();
                    System.out.println("Disconnected");
                    client.close();
                }
                catch(MqttException me) {
                    System.out.println("reason " + me.getReasonCode());
                    System.out.println("msg " + me.getMessage());
                    System.out.println("loc " + me.getLocalizedMessage());
                    System.out.println("cause " + me.getCause());
                    System.out.println("excep " + me);
                    me.printStackTrace();
                }
            }
        });
        colorPickerView = (ColorPickerView) findViewById(R.id.colorPickerView);
        //Barre de luminosité
        brightnessSlideBar = findViewById(R.id.brightnessSlide);

        //Roue des couleurs
        colorPickerView.setColorListener(new ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                final int red = Color.red(color);
                final int green = Color.green(color);
                final int blue = Color.blue(color);
                final int currentColorSum = red + green + blue;

                if (Math.abs(currentColorSum - previousColorSum) >= ColorSumDifference) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            EnvoiMQTT(red, green, blue);
                        }
                    });
                    previousColorSum = currentColorSum;
                }
            }
        });

        colorPickerView.attachBrightnessSlider(brightnessSlideBar);
        loadSelectedColorAndBrightness();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSelectedColorAndBrightness(colorPickerView.getColor());
        executorService.shutdown();
        try {
            if (client != null) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private void EnvoiMQTT(int R, int G, int B){
        try {
            //si le client est déconnecté ont le reconnecte
            if (!client.isConnected()) {
                System.out.println("Reconnecting to broker: " + broker);
                client.connect(connOpts);
                System.out.println("Reconnected");
            }

            //MaJ contenu du message
            content = R + "/" + G + "/" + B + "/" + interrupteur.isChecked();

            //Ajout du contenu du message
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            client.publish(pubTopic, message);
            System.out.println("Publishing message: " + message);

        } catch(MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
        }
}