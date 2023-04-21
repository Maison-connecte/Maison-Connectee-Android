package Info420.maisonconnecte;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

public class ConnexionMQTT extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ConnexionMQTT";
    Button btnConnect;
    String subTopic = "capteur_ultrason";
    String pubTopic = "MaisonConnectee_420-67P-SI/capteur_ultrason";
    String content = "Hello World";
    int qos = 2;
    String broker = "tcp://test.mosquitto.org:1883"; //Changer pour l'adresse du broker
    String clientId = "emqx_test";
    MemoryPersistence persistence = new MemoryPersistence();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connexion_mqtt);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnect:
                try {
                    connexion();
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    private void connexion() throws MqttException {
        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            //connOpts.setUserName("rw");
            //connOpts.setPassword("readwrite".toCharArray());
            connOpts.setCleanSession(true);

            client.setCallback(new OnMessageCallback());

            // établir connexion
            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);

            System.out.println("Connected");
            System.out.println("Publishing message: " + content);

            // Subscribe
            client.subscribe(subTopic);

            // Required parameters for message publishing
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