package Info420.maisonconnecte;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ServiceReception extends Service {
    private static final String TAG = "ServiceReception";
    boolean estDejaDemarre = false;
    String sousTopic = "capteur_ultrason";
    String courtier = "tcp://test.mosquitto.org:1883"; //Changer pour l'adresse du broker
    String identifiantClient = "emqx_test";
    MemoryPersistence persistance = new MemoryPersistence();

    MqttClient client;

    private Handler gestionnaireVerifConnection;
    private final HandlerThread filGestionnaire = new HandlerThread("filGestionnaireMqtt");
    private static final long INTERVALLE_VERIF_CONNECTION = 10000; // 10 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        filGestionnaire.start();
        gestionnaireVerifConnection = new Handler(filGestionnaire.getLooper());
        Log.d(TAG, "onCreate(): service créé");
    }

    // Démarre le service pour le mode sécurité
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!estDejaDemarre) {
            Log.d(TAG, "onStartCommand(): service démarré");
            estDejaDemarre = true;
        }
        gestionnaireVerifConnection.post(verifConnectionRunnable);
        return Service.START_STICKY;
    }

    private final Runnable verifConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (client == null || !client.isConnected()) {
                    client = new MqttClient(courtier, identifiantClient, persistance);

                    MqttConnectOptions optionsConn = new MqttConnectOptions();
                    optionsConn.setCleanSession(true);

                    client.setCallback(new OnMessageCallback());

                    // établir connexion
                    client.connect(optionsConn);

                    // Souscrire au topic une seule fois
                    client.subscribe(sousTopic);
                    Log.d(TAG, "Abonné avec succès au topic: " + sousTopic);
                }
            } catch (MqttException | ClassNotFoundException | IllegalAccessException |
                     InstantiationException | NoSuchMethodException me) {
                // Gérer les exceptions
                Log.e(TAG, "Exception", me);
            }

            gestionnaireVerifConnection.postDelayed(this, INTERVALLE_VERIF_CONNECTION);
        }
    };

    @Override
    public void onDestroy() {
        System.out.println("service arrêté");
        gestionnaireVerifConnection.removeCallbacks(verifConnectionRunnable);
        filGestionnaire.quitSafely();  // Quitter de manière sécurisée le fil du gestionnaire
        if (client != null) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    // Liaison non utilisée dans le projet mais nécessaire au projet
    public IBinder onBind(Intent intent) {
        return null;
    }
}