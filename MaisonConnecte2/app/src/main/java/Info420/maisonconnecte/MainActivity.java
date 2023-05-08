package Info420.maisonconnecte;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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