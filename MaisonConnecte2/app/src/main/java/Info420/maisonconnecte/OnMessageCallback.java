package Info420.maisonconnecte;

import static androidx.core.content.ContextCompat.getSystemService;
import android.content.Context;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class OnMessageCallback implements MqttCallback {
    Date currentTime;

    OnMessageCallback() {

    }

    public void connectionLost(Throwable cause) {
        // After the connection is lost, it usually reconnects here
        //System.out.println("disconnect，you can reconnect");
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println(topic + ": " + new String(message.getPayload()));

        currentTime = Calendar.getInstance().getTime();
        if (Objects.equals(topic, "capteur_ultrason")) {
            MainActivity.CapteurUltrason(currentTime, Integer.parseInt(new String(message.getPayload())));
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("deliveryComplete---------" + token.isComplete());
    }
}