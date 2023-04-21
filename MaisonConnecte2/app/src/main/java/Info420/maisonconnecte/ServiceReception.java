package Info420.maisonconnecte;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ServiceReception extends Service {
    private static final String TAG = "ServiceReception";
    public Handler mHandler = new Handler();
    boolean estDejaDemarre = false;

    String subTopic = "capteur_ultrason";
    int qos = 2;
    String broker = "tcp://test.mosquitto.org:1883"; //Changer pour l'adresse du broker
    String clientId = "emqx_test";
    MemoryPersistence persistence = new MemoryPersistence();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate(): service créé");
    }

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
        stopRepeatingTask();
        super.onDestroy();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            reception();
            mHandler.postDelayed(this, 2000);
        }
    };

    private void reception() {
        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            //connOpts.setUserName("rw");
            //connOpts.setPassword("readwrite".toCharArray());
            connOpts.setCleanSession(true);

            client.setCallback(new OnMessageCallback());

            // établir connexion
            //System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);

            //System.out.println("Connected");

            // Subscribe
            client.subscribe(subTopic);

            //client.disconnect();
            //System.out.println("Disconnected");
            //client.close();
        } catch(MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    //Binding pas utilisé dans le projet
    public IBinder onBind(Intent intent) {
        return null;
    }
}
