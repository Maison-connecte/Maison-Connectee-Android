package Info420.maisonconnecte;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

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


    //Variables couleur
    ColorPickerView colorPickerView;
    BrightnessSlideBar brightnessSlideBar;
    private SeekBar seekBarIntensite;
    private TextView intensite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_div);

        //Interrupteur
        interrupteur = (ToggleButton) findViewById(R.id.interrupteur);
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

        //Roue des couleurs
        colorPickerView = (ColorPickerView) findViewById(R.id.colorPickerView);
        colorPickerView.setColorListener(new ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                EnvoiMQTT(Color.red(color), Color.green(color), Color.blue(color));
            }
        });

        //Barre de luminosité
        brightnessSlideBar = findViewById(R.id.brightnessSlide);
        colorPickerView.attachBrightnessSlider(brightnessSlideBar);
        
        //Barre d'intensité
        intensite = (TextView) findViewById(R.id.intensiteValeur);
        intensite.setText("100");
        seekBarIntensite = (SeekBar) findViewById(R.id.seekBarIntensite);
        seekBarIntensite.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intensite.setText("" + (progress+20));
                if(progress<1)
                {
                    progress=1;
                }
                String intensite=Integer.toString(progress);
                try {
                    MqttClient client = new MqttClient(broker, clientId, persistence);

                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    connOpts.setCleanSession(true);
                    System.out.println("Connecting to broker: " + broker);
                    client.connect(connOpts);

                    System.out.println("Connected");
                    System.out.println("Publishing message: " + progress);

                    //Ajout du contenu du message
                    MqttMessage message = new MqttMessage(intensite.getBytes());
                    message.setQos(qos);
                    client.publish("intensite", message);
                    message=new MqttMessage("1".getBytes());
                    System.out.println("Message published");

                    client.disconnect();
                    System.out.println("Disconnected");
                    client.close();


                } catch (MqttException e) {
                    System.out.println("reason " + e.getReasonCode());
                    System.out.println("msg " + e.getMessage());
                    System.out.println("loc " + e.getLocalizedMessage());
                    System.out.println("cause " + e.getCause());
                    System.out.println("excep " + e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    
    private void EnvoiMQTT(int R, int G, int B){
        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            //MaJ contenu du message
            content = R + "/" + G + "/" + B + "/" + interrupteur.isChecked();

            //Établir connexion
            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);

            System.out.println("Connected");
            System.out.println("Publishing message: " + content);

            //Ajout du contenu du message
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            client.publish(pubTopic, message);
            System.out.println("Message published");

            client.disconnect();
            System.out.println("Disconnected");
            client.close();
        } catch(MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }
}