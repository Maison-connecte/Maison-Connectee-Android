package Info420.maisonconnecte;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity {
    private Switch interrupteurSecurite; // switchSecurite
    private static TextView TextViewDate; // TextViewDate
    private static TextView TextViewHeure; // TextViewHeure
    private boolean notifDesactivee; // notifDesactivee
    String[] perms = {"android.permission.POST_NOTIFICATION"};
    int permsRequestCode = 200;
    private TextView ouvertureLumiereTextView;
    private TextView fermetureLumiereTextView;
    private static MqttAndroidClient clientMqtt; // mqttClient
    private String serverUri = "tcp://test.mosquitto.org:1883"; // L'URI du broker MQTT
    private String clientId = "ClientAndroid"; // Un identifiant unique pour ce client
    private static String sujet = "allumer_led_divertissement"; // Le sujet à publier
    private void initialiserClientMqtt() { // initialisation du client mqtt
        clientMqtt = new MqttAndroidClient(this.getApplicationContext(), serverUri, clientId);

        try {
            IMqttToken token = clientMqtt.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Connecté avec succès
                    Log.d("DEBUG", "Connecté au broker MQTT");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Échec de la connexion
                    Log.d("DEBUG", "Échec de la connexion au broker MQTT");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public static void publierMessage(String message) {
        byte[] messageEncode = new byte[0];
        try {
            messageEncode = message.getBytes("UTF-8");
            MqttMessage mqttMessage = new MqttMessage(messageEncode);
            System.out.println("Publication du message : " + mqttMessage);
            clientMqtt.publish(sujet, mqttMessage);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    private void configurerAlarme(String temps, String message) {
        String[] partiesTemps = temps.split(":");
        int heure = Integer.parseInt(partiesTemps[0]);
        int minute = Integer.parseInt(partiesTemps[1]);

        Intent intention = new Intent(MainActivity.this, AlarmReceiver.class);
        intention.putExtra("message", message);
        PendingIntent intentionPendante = PendingIntent.getBroadcast(MainActivity.this, 0, intention, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager gestionnaireAlarme = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendrier = Calendar.getInstance();

        calendrier.setTimeInMillis(System.currentTimeMillis());
        calendrier.set(Calendar.HOUR_OF_DAY, heure);
        calendrier.set(Calendar.MINUTE, minute);

        // Si l'heure de l'alarme est déjà passée pour aujourd'hui, la régler pour demain
        if (calendrier.getTimeInMillis() <= System.currentTimeMillis()) {
            calendrier.add(Calendar.DATE, 1);
        }

        gestionnaireAlarme.setExact(AlarmManager.RTC_WAKEUP, calendrier.getTimeInMillis(), intentionPendante);
        Log.d("DEBUG", "message: " + message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        interrupteurSecurite = (Switch) findViewById(R.id.switchSecurite);
        interrupteurSecurite.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b == true) {
                //Vérifie si les notifications sont activées
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(perms, permsRequestCode);
                    interrupteurSecurite.setChecked(false);
                    Toast.makeText(this, "Activer les notifications pour le mode Sécurité", Toast.LENGTH_LONG).show();
                } else {
                    startService(new Intent(getApplicationContext(), ServiceReception.class));
                }
            } else {
                stopService(new Intent(getApplicationContext(), ServiceReception.class));
            }
        });

        Intent intention = new Intent(getApplicationContext(), MainActivity.class);

        //Capteur ultrason
        TextViewDate = (TextView) findViewById(R.id.DateContenu);
        TextViewHeure = (TextView) findViewById(R.id.HeureContenu);
        TextViewHeure.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("MissingPermission")
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(!notifDesactivee)
                {
                    creerCanalNotification();

                    notifDesactivee = true;
                }
            }
        });

        Button boutonChoixOuvertureDialog = findViewById(R.id.boutonChoixOuvertureDialog);
        Button boutonChoixFermetureDialog = findViewById(R.id.boutonChoixFermetureDialog);
        ouvertureLumiereTextView = findViewById(R.id.ouvertureLumiereTextView);
        fermetureLumiereTextView = findViewById(R.id.fermetureLumiereTextView);
        boutonChoixOuvertureDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ouvrirDialogChoixHeure(ouvertureLumiereTextView, "Ouverture");
            }
        });

        boutonChoixFermetureDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ouvrirDialogChoixHeure(fermetureLumiereTextView,"Fermeture");
            }
        });

        chargerHeureDepuisPreferences("Ouverture", ouvertureLumiereTextView, "Ouverture");
        chargerHeureDepuisPreferences("Fermeture", fermetureLumiereTextView, "Fermeture");
        initialiserClientMqtt();
    }

    // pour remettre les valeurs sauvegardées dans les préférences
    private void chargerHeureDepuisPreferences(String cle, TextView vueTexteCible, String prefixe) {
        SharedPreferences preferencesPartagees = getSharedPreferences("preferences_heure", MODE_PRIVATE);
        int heureDuJour = preferencesPartagees.getInt(cle + "_heure", -1);
        int minute = preferencesPartagees.getInt(cle + "_minute", -1);

        if (heureDuJour != -1 && minute != -1) {
            String periodeTemps = heureDuJour < 12 ? "AM" : "PM";
            int heureEnFormat12Heures = heureDuJour % 12 == 0 ? 12 : heureDuJour % 12;
            vueTexteCible.setText(String.format("%s des lumières à : %02d:%02d %s", prefixe, heureEnFormat12Heures, minute, periodeTemps));

            String temps = String.format("%02d:%02d", heureDuJour, minute);
            String message = prefixe.equals("Ouverture") ? "1" : "0";
            configurerAlarme(temps, message);
        }
    }

    // pour sauvegarder les temps d'ouverture et de fermeture dans les préférences.
    private void sauvegarderHeureDansPreferences(String cle, int heureDuJour, int minute) {
        SharedPreferences preferencesPartagees = getSharedPreferences("preferences_heure", MODE_PRIVATE);
        SharedPreferences.Editor editeur = preferencesPartagees.edit();
        editeur.putInt(cle + "_heure", heureDuJour);
        editeur.putInt(cle + "_minute", minute);
        editeur.apply();
    }

    // Fonction pour créer le dialog de choix d'heure
    private void ouvrirDialogChoixHeure(TextView vueTexteCible,String prefixe) {
        TimePickerDialog dialogChoixHeure = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int heureDuJour, int minute) {
                        String periodeTemps = heureDuJour < 12 ? "AM" : "PM";
                        int heureEnFormat12Heures = heureDuJour % 12 == 0 ? 12 : heureDuJour % 12;
                        // Définir le texte de la vue TextView
                        vueTexteCible.setText(String.format("%s des lumières à : %02d:%02d %s", prefixe, heureEnFormat12Heures, minute, periodeTemps));
                        // Sauvegarder l'heure dans les préférences partagées
                        sauvegarderHeureDansPreferences(prefixe, heureDuJour, minute);

                        // configuration de l'alarme
                        String temps = String.format("%02d:%02d", heureDuJour, minute);
                        String message = prefixe.equals("Ouverture") ? "1" : "0";

                        // Ajout des logs de debug
                        Log.d("DEBUG", "prefixe: " + prefixe);
                        Log.d("DEBUG", "heureDuJour: " + heureDuJour);
                        Log.d("DEBUG", "minute: " + minute);
                        Log.d("DEBUG", "temps: " + temps);
                        Log.d("DEBUG", "message: " + message);

                        configurerAlarme(temps, message);
                    }

                },
                12, 0, false); // Définir l'heure initiale et le format 12 heures

        dialogChoixHeure.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifDesactivee = true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        notifDesactivee = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // Fonction pour créer le canal de notification
    public void creerCanalNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nom = "nom";
            String description = "desc";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel canal = new NotificationChannel("123", nom, importance);
            canal.setDescription(description);
            NotificationManager gestionnaireNotification = getSystemService(NotificationManager.class);
            gestionnaireNotification.createNotificationChannel(canal);

            NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(getApplicationContext(),"123")
                    .setSmallIcon(R.mipmap.stat_notify_error) // icône de notification
                    .setContentTitle("Alerte Mouvement!") // titre pour la notification
                    .setContentText("Le détecteur de mouvement a été déclenché") // message pour la notification
                    .setAutoCancel(true); // efface la notification après le clic
            Intent intention = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),0,intention,PendingIntent.FLAG_IMMUTABLE);
            intention.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mBuilder.setContentIntent(pi);
            gestionnaireNotification.notify(1, mBuilder.build());
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemLedDiv:
                startActivity(new Intent(MainActivity.this, LedDiv.class));
                break;
            case R.id.itemCam:
                startActivity(new Intent(MainActivity.this, Cam.class));
                break;
        }
        return true;
    }

    // Gère l'évènement lorsqu'un mouvement est détecté par le capteur à ultrasons
    public static void CapteurUltrason(Date horodatage, int distance) {
        Calendar calendrier = GregorianCalendar.getInstance(); // crée une nouvelle instance de calendrier
        calendrier.setTime(horodatage);   // attribue le calendrier à la date donnée
        TextViewDate.setText(String.format("%d-%d-%d", calendrier.get(Calendar.YEAR), calendrier.get(Calendar.MONTH) + 1, calendrier.get(Calendar.DAY_OF_MONTH)));
        int heure = calendrier.get(Calendar.HOUR_OF_DAY);
        int minute = calendrier.get(Calendar.MINUTE);
        int seconde = calendrier.get(Calendar.SECOND);
        String heureTxt = String.valueOf(heure);
        String minuteTxt = String.valueOf(minute);
        String secondeTxt = String.valueOf(seconde);
        if (heure < 10) {
            heureTxt = "0" + String.valueOf(heure);
        }
        if (minute < 10) {
            minuteTxt = "0" + String.valueOf(minute);
        }
        if (seconde < 10) {
            secondeTxt = "0" + String.valueOf(seconde);
        }
        TextViewHeure.setText(String.format("%d:%d:%d", heure, minute, seconde));
    }
}