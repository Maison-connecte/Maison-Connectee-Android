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
    private Switch switchSecurite;
    private static TextView TextViewDate;
    private static TextView TextViewHeure;
    private boolean notifDesactivee;
    String[] perms = {"android.permission.POST_NOTIFICATION"};
    int permsRequestCode = 200;
    Button button;
    private TextView ouvertureLumiereTextView;
    private TextView fermetureLumiereTextView;

    // Add this field to your MainActivity class
    private static MqttAndroidClient mqttClient;
    private String serverUri = "tcp://test.mosquitto.org:1883"; // the URI of the MQTT broker
    private String clientId = "AndroidClient"; // a unique identifier for this client
    private static String topic = "enable"; // the topic to publish to
    private void initializeMqttClient() {
        mqttClient = new MqttAndroidClient(this.getApplicationContext(), serverUri, clientId);

        try {
            IMqttToken token = mqttClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Connected successfully
                    Log.d("DEBUG", "Connected to MQTT broker");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Failed to connect
                    Log.d("DEBUG", "Failed to connect to MQTT broker");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public static void publishMessage(String message) {
        byte[] encodedMessage = new byte[0];
        try {
            encodedMessage = message.getBytes("UTF-8");
            MqttMessage mqttMessage = new MqttMessage(encodedMessage);
            System.out.println("Publishing message: " + mqttMessage);
            mqttClient.publish(topic, mqttMessage);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAlarm(String time, String message) {
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        // If the alarm time has passed for today, set it for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1);
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d("DEBUG", "message: " + message);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switchSecurite = (Switch) findViewById(R.id.switchSecurite);
        switchSecurite.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b == true) {
                //Vérifie si les notifications sont activées
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(perms, permsRequestCode);
                    switchSecurite.setChecked(false);
                    Toast.makeText(this, "Activer les notifications pour le mode Sécurité", Toast.LENGTH_LONG).show();
                } else {
                    startService(new Intent(getApplicationContext(), ServiceReception.class));
                }
            } else {
                stopService(new Intent(getApplicationContext(), ServiceReception.class));
            }
        });

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

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
                    createNotificationChannel();

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
                openTimePickerDialog(ouvertureLumiereTextView, "Ouverture");
            }
        });

        boutonChoixFermetureDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTimePickerDialog(fermetureLumiereTextView,"Fermeture");
            }
        });

        loadTimeFromSharedPreferences("Ouverture", ouvertureLumiereTextView, "Ouverture");
        loadTimeFromSharedPreferences("Fermeture", fermetureLumiereTextView, "Fermeture");
        initializeMqttClient();
    }

    //pour remettre les valeurs sauvegarder dans les préférences
    private void loadTimeFromSharedPreferences(String key, TextView targetTextView, String prefix) {
        SharedPreferences sharedPreferences = getSharedPreferences("time_preferences", MODE_PRIVATE);
        int hourOfDay = sharedPreferences.getInt(key + "_hour", -1);
        int minute = sharedPreferences.getInt(key + "_minute", -1);

        if (hourOfDay != -1 && minute != -1) {
            String timePeriod = hourOfDay < 12 ? "AM" : "PM";
            int hourIn12HourFormat = hourOfDay % 12 == 0 ? 12 : hourOfDay % 12;
            targetTextView.setText(String.format("%s des lumières à : %02d:%02d %s", prefix, hourIn12HourFormat, minute, timePeriod));

            String time = String.format("%02d:%02d", hourOfDay, minute);
            String message = prefix.equals("Ouverture") ? "1" : "0";
            setAlarm(time, message);
        }
    }

    //pour sauvegarder les temps ouverture et fermeture dans les préférence.
    private void saveTimeToSharedPreferences(String key, int hourOfDay, int minute) {
        SharedPreferences sharedPreferences = getSharedPreferences("time_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key + "_hour", hourOfDay);
        editor.putInt(key + "_minute", minute);
        editor.apply();
    }


    //Function pour créer le timer dialog
    private void openTimePickerDialog(TextView targetTextView,String prefix) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String timePeriod = hourOfDay < 12 ? "AM" : "PM";
                        int hourIn12HourFormat = hourOfDay % 12 == 0 ? 12 : hourOfDay % 12;
                        // Set the TextView text
                        targetTextView.setText(String.format("%s des lumières à : %02d:%02d %s", prefix, hourIn12HourFormat, minute, timePeriod));
                        // Save the time to SharedPreferences
                        saveTimeToSharedPreferences(prefix, hourOfDay, minute);

                        // setting the alarm
                        String time = String.format("%02d:%02d", hourOfDay, minute);
                        String message = prefix.equals("Ouverture") ? "1" : "0";

                        // Add debugging logs
                        Log.d("DEBUG", "prefix: " + prefix);
                        Log.d("DEBUG", "hourOfDay: " + hourOfDay);
                        Log.d("DEBUG", "minute: " + minute);
                        Log.d("DEBUG", "time: " + time);
                        Log.d("DEBUG", "message: " + message);

                        setAlarm(time, message);
                    }

                },
                12, 0, false); // Set initial time and 12-hour format

        timePickerDialog.show();
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

    public void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "nom";
            String description = "desc";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("123", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(getApplicationContext(),"123")
                    .setSmallIcon(R.mipmap.stat_notify_error) // notification icon
                    .setContentTitle("Alerte Mouvement!") // title for notification
                    .setContentText("Le détecteur de mouvement a été déclenché") // message for notification
                    .setAutoCancel(true); // clear notification after click
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),0,intent,PendingIntent.FLAG_IMMUTABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mBuilder.setContentIntent(pi);
            notificationManager.notify(1, mBuilder.build());
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

    public static void CapteurUltrason(Date timestamp, int distance) {
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(timestamp);   // assigns calendar to given date
        TextViewDate.setText(String.format("%d-%d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)));
        int heure = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int seconde = calendar.get(Calendar.SECOND);
        if (heure < 10) {
            TextViewHeure.setText(String.format("0%d:", heure));
        }
        if (minute < 10) {
            TextViewHeure.setText(String.format("%d:0%d:", heure, minute));
        }
        if (seconde < 10) {
            TextViewHeure.setText(String.format("%d:%d:0%d", heure, minute, seconde));
        }
        TextViewHeure.setText(String.format("%d:%d:%d", heure, minute, seconde));
    }

}