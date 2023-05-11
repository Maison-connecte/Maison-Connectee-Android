package Info420.maisonconnecte;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ServiceReception extends Service {
    private static final String TAG = "ServiceReception";
    public Handler mHandler = new Handler();
    boolean estDejaDemarre = false;
    String subTopic = "capteur_ultrason";
    String broker = "tcp://test.mosquitto.org:1883"; //Changer pour l'adresse du broker
    String clientId = "emqx_test";
    MemoryPersistence persistence = new MemoryPersistence();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate(): service créé");
    }

    //démare le service pour le mode sécurité
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!estDejaDemarre)
        {
            Log.d(TAG, "onStartCommand(): service démarré");
            estDejaDemarre = true;
            startRepeatingTask();
        }
        else
        {
            Log.d(TAG, "onStartCommand(): service déjà démarré");
        }
        return Service.START_STICKY;
    }

   @Override
    public void onDestroy() {
        System.out.println("service arrêté");
        super.onDestroy();
    }


    //permet de maintenir la souscription au topic
    Runnable repetitionExecution = new Runnable() {
        @Override
        public void run() {
            reception();
            mHandler.postDelayed(this, 2000);
        }
    };

    //reçois les messages MQTT
    private void reception() {
        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            client.setCallback(new OnMessageCallback());

            // établir connexion
            client.connect(connOpts);

            // Subscribe
            client.subscribe(subTopic);

        } catch(MqttException me) {
            System.out.println("raison " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }catch (IllegalAccessException | ClassNotFoundException | InstantiationException |
                NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    void startRepeatingTask() {
        repetitionExecution.run();
    }

    //Binding pas utilisé dans le projet mais nécessaire au projet
    public IBinder onBind(Intent intent) {
        return null;
    }
}
