package Info420.maisonconnecte;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*----------------------------------------------------------------
 *  Auteur: Maxime Paulin, Nathael Blais, Dylan lévesque.
 *
 *  Date de création: 22 Mars 2023
 *
 *  Dernière date de modification: [2023-05-16]
 *
 *  Description: Gère l'activité de la bande DEL divertissement
 *----------------------------------------------------------------*/

// Classe principale pour contrôler les lumières LED
public class LedDiv extends AppCompatActivity {

    // Variable pour l'interrupteur
    private ToggleButton interrupteur;

    // Variables pour MQTT
    String sujetPublication = "couleur_led_divertissement";

    String getSujetPublicationFermeLumiere = "allumer_led_divertissement";
    String contenu;
    int qos = 2;
    String broker = "tcp://test.mosquitto.org:1883";
    String identifiantClient = "emqx_test";
    MemoryPersistence persistence = new MemoryPersistence();

    // Création d'un pool de threads pour les envois MQTT
    private final ExecutorService serviceExecutor = Executors.newSingleThreadExecutor();
    private int sommeCouleurPrecedente = 0;
    private final int differenceSommeCouleur = 20;

    // Variables pour la couleur
    ColorPickerView selecteurCouleur;
    BrightnessSlideBar barreLuminosite;

    // Connexion MQTT
    private MqttClient client;
    private MqttConnectOptions optionsConnexion;

    //sert à regarder si le color picket est initialisé.
    //Cela sert à regarder si les préférence sont bien initialisé
    //Permet d'éviter d'envoyer blanc 255/255/255 au topic
    private boolean premiereInitialisationCouleur = true;



    // Sauvegarde en cache la couleur pour que quand l'activité est détruite et recréer la couleur est préservé.
    private void sauvegarderCouleurEtLuminositeSelectionnees(int color) {
        SharedPreferences preferencesPartagees = getSharedPreferences("LedDivPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editeur = preferencesPartagees.edit();
        editeur.putInt("couleurSelectionnee", color);
        editeur.apply();
    }

    // Utilise les préférences et met le sélecteur de couleur à la bonne couleur
    private void chargerCouleurEtLuminositeSelectionnees() {
        SharedPreferences preferencesPartagees = getSharedPreferences("LedDivPrefs", Context.MODE_PRIVATE);
        int couleur = preferencesPartagees.getInt("couleurSelectionnee", Color.BLACK);
        selecteurCouleur.setInitialColor(couleur);
    }

    private void initialisationMQTT(){
        //connexion à mqtt
        serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                // Établissement d'une connexion au serveur MQTT
                try {
                    client = new MqttClient(broker, identifiantClient, persistence);

                    optionsConnexion = new MqttConnectOptions();
                    optionsConnexion.setCleanSession(true);
                    optionsConnexion.setAutomaticReconnect(true);

                    System.out.println("Connexion au broker: " + broker);
                    client.connect(optionsConnexion);
                    System.out.println("Connecté");

                } catch (MqttException me) {
                    System.out.println("raison " + me.getReasonCode());
                    System.out.println("msg " + me.getMessage());
                    System.out.println("loc " + me.getLocalizedMessage());
                    System.out.println("cause " + me.getCause());
                    System.out.println("excep " + me);
                    me.printStackTrace();
                }
            }
        });
    }

    // Méthode appelée lors de la création de l'activité
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_div);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        // Interrupteur
        interrupteur = (ToggleButton) findViewById(R.id.interrupteur);

        // Barre de luminosité et roue des couleurs
        selecteurCouleur = (ColorPickerView) findViewById(R.id.colorPickerView);
        barreLuminosite = findViewById(R.id.brightnessSlide);
        selecteurCouleur.attachBrightnessSlider(barreLuminosite);
        //charge la couleur dans les préférences
        chargerCouleurEtLuminositeSelectionnees();
        initialisationMQTT();

        // Charger le statut du bouton à bascule à partir des SharedPreferences
        SharedPreferences preferencesPartagees = getSharedPreferences("preferences_application", Context.MODE_PRIVATE);
        boolean etatInterrupteur = preferencesPartagees.getBoolean("etatBascule", false);
        interrupteur.setChecked(etatInterrupteur);


        interrupteur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serviceExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!client.isConnected()){
                                client = new MqttClient(broker, identifiantClient, persistence);
                                optionsConnexion = new MqttConnectOptions();
                                optionsConnexion.setCleanSession(true);
                                client.connect(optionsConnexion);
                                Log.d("debug","Connexion au broker: " + broker);
                                Log.d("debug","Connecté");
                            }

                            if(interrupteur.isChecked()){
                                contenu = "1";
                                // prend la couleur sélectionnée dans la roue des couleurs
                                int color = selecteurCouleur.getColor();
                                final int rouge = Color.red(color);
                                final int vert = Color.green(color);
                                final int bleu = Color.blue(color);
                                // envois la couleur
                                envoiMQTT(rouge, vert, bleu);
                            } else {
                                contenu = "0";
                                envoiMQTT(Integer.parseInt(contenu));
                            }
                        } catch(MqttException me) {
                            Log.d("debug","raison " + me.getReasonCode());
                            Log.d("debug","msg " + me.getMessage());
                            Log.d("debug","loc " + me.getLocalizedMessage());
                            Log.d("debug","cause " + me.getCause());
                            Log.d("debug","excep " + me);
                            me.printStackTrace();
                        }
                    }
            });

                // Sauvegarde de l'état du bouton à bascule dans les SharedPreferences
                SharedPreferences preferencesPartagees = getSharedPreferences("preferences_application", Context.MODE_PRIVATE);
                SharedPreferences.Editor editeur = preferencesPartagees.edit();
                editeur.putBoolean("etatBascule", interrupteur.isChecked());
                editeur.apply();
            }
        });

        // Roue des couleurs
        setColorListener();

    }

    private void setColorListener() {
        // Roue des couleurs
        selecteurCouleur.setColorListener(new ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                final int rouge = Color.red(color);
                final int vert = Color.green(color);
                final int bleu = Color.blue(color);

                if (premiereInitialisationCouleur) {
                    premiereInitialisationCouleur = false;
                    return;
                }

                final int sommeCouleurActuelle = rouge + vert + bleu;

                if (Math.abs(sommeCouleurActuelle - sommeCouleurPrecedente) >= differenceSommeCouleur) {
                    if (interrupteur.isChecked()) { // check if light strip is on and preferences have been loaded
                        serviceExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                envoiMQTT(rouge, vert, bleu);
                            }
                        });
                    }
                    sommeCouleurPrecedente = sommeCouleurActuelle;
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Charger l'état à partir des SharedPreferences
        SharedPreferences preferencesPartagees = getSharedPreferences("preferences_application", MODE_PRIVATE);
        boolean etatInterrupteur = preferencesPartagees.getBoolean("etatBascule", false); // par défaut à false

        interrupteur.setChecked(etatInterrupteur);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sauvegarderCouleurEtLuminositeSelectionnees(selecteurCouleur.getColor());
        // MQTT disconnection
        serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client != null) {
                        client.disconnect();
                        client.close();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
        serviceExecutor.shutdown();
    }

    private void envoiMQTT(int R, int G, int B){
        try {
            //si le client est déconnecté ont le reconnecte
            if (!client.isConnected()) {
                System.out.println("Reconnexion au broker: " + broker);
                client.connect(optionsConnexion);
                System.out.println("Reconnecté");
            }

            // MaJ contenu du message
            contenu = R + "/" + G + "/" + B;

            // Ajout du contenu du message
            MqttMessage message = new MqttMessage(contenu.getBytes());
            message.setQos(qos);
            client.publish(sujetPublication, message);
            System.out.println("Publication du message: " + message);

        } catch(MqttException me) {
            System.out.println("raison " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    private void envoiMQTT(int ferme){
        try {
            //si le client est déconnecté ont le reconnecte
            if (!client.isConnected()) {
                System.out.println("Reconnexion au broker: " + broker);
                client.connect(optionsConnexion);
                System.out.println("Reconnecté");
            }

            // MaJ contenu du message
            contenu = String.valueOf(ferme);

            // Ajout du contenu du message
            MqttMessage message = new MqttMessage(contenu.getBytes());
            message.setQos(qos);
            client.publish(getSujetPublicationFermeLumiere, message);
            System.out.println("Publication du message: " + message);

        } catch(MqttException me) {
            System.out.println("raison " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    //flèche en haut à gauche pour retourner au menu principal
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
