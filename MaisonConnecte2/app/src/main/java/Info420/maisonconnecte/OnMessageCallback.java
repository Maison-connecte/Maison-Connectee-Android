package Info420.maisonconnecte;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

//class utilisé pour le mode sécurité, reçois les messages à MQTT et met à jour les textes sur le MainAcitivity
public class OnMessageCallback implements MqttCallback {

    //date actuel
    Date tempsActuel;

    OnMessageCallback() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException {

    }

    //quand la connexion est perdu, un message est envoyer dans la console
    public void connectionLost(Throwable cause) {
        // After the connection is lost, it usually reconnects here
        System.out.println("déconnecté，vous pouvez vous reconnecté");
    }

    //quand un message est reçus ont affiche dans la console et ont met à jour les texte dans la mainActivity
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //System.out.println(topic + ": " + new String(message.getPayload()));

        tempsActuel = Calendar.getInstance().getTime();
        if (Objects.equals(topic, "capteur_ultrason") && new String(message.getPayload()).equals("1") ) {
            MainActivity.CapteurUltrason(tempsActuel);
        }
    }

    //un message est reçus pour dire que l'envois est complété
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("deliveryComplete---------" + token.isComplete());
    }
}