package Info420.maisonconnecte;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class OnMessageCallback implements MqttCallback {

    Date currentTime;
    private NotificationManager NM;

    OnMessageCallback() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException {

    }

    public void connectionLost(Throwable cause) {
        // After the connection is lost, it usually reconnects here
        //System.out.println("disconnect，you can reconnect");
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println(topic + ": " + new String(message.getPayload()));

        currentTime = Calendar.getInstance().getTime();
        if (Objects.equals(topic, "capteur_ultrason") && new String(message.getPayload()).equals("1") ) {
            MainActivity.CapteurUltrason(currentTime, Integer.parseInt(new String(message.getPayload())));



        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("deliveryComplete---------" + token.isComplete());
    }
}