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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class LedDiv extends AppCompatActivity {
    private SeekBar seekBarR;
    private SeekBar seekBarG;
    private SeekBar seekBarB;
    private TextView valeurR;
    private TextView valeurG;
    private TextView valeurB;
    private ToggleButton power;
    private Button confirm;

    //Variables MQTT
    String pubTopic = "MaisonConnectée_420-67P-SI/Led_div";
    String content;
    int qos = 2;
    String broker = "tcp://test.mosquitto.org:1883"; //Changer pour l'adresse du broker
    String clientId = "emqx_test";
    MemoryPersistence persistence = new MemoryPersistence();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_div);

        //Affiche la répartition de rouge, vert et bleu
        valeurR = (TextView) findViewById(R.id.valeurR);
        valeurR.setText("100");
        valeurG = (TextView) findViewById(R.id.valeurG);
        valeurG.setText("100");
        valeurB = (TextView) findViewById(R.id.valeurB);
        valeurB.setText("100");

        //Aperçu de la couleur
        TextView preview = (TextView) findViewById(R.id.preview);

        //Sliders pour sélectionner la couleur
        seekBarR = (SeekBar) findViewById(R.id.seekBarR);
        seekBarG = (SeekBar) findViewById(R.id.seekBarG);
        seekBarB = (SeekBar) findViewById(R.id.seekBarB);

        //Interrupteur
        power = (ToggleButton) findViewById(R.id.togglePower);

        //Confirme l'envoi de la commande à la LED
        confirm = (Button) findViewById(R.id.btnConfirm);

        seekBarR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valeurR.setText("" + progress);
                preview.setBackgroundColor(Color.rgb(seekBarR.getProgress(), seekBarG.getProgress(), seekBarB.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                EnvoiMQTT();
            }
        });

        seekBarG.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valeurG.setText("" + progress);
                preview.setBackgroundColor(Color.rgb(seekBarR.getProgress(), seekBarG.getProgress(), seekBarB.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                EnvoiMQTT();
            }
        });

        seekBarB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valeurB.setText("" + progress);
                preview.setBackgroundColor(Color.rgb(seekBarR.getProgress(), seekBarG.getProgress(), seekBarB.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                EnvoiMQTT();
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EnvoiMQTT();
            }
        });
    }

    private void EnvoiMQTT(){
        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            //connOpts.setUserName("rw");
            //connOpts.setPassword("readwrite".toCharArray());
            connOpts.setCleanSession(true);

            //MaJ contenu du message
            content = valeurR.getText().toString() + "/" + valeurG.getText().toString() + "/" + valeurB.getText().toString() + "/" + power.isChecked();

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