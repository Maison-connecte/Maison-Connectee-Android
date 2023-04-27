package Info420.maisonconnecte;

import static java.lang.System.currentTimeMillis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.time.temporal.ChronoUnit;

public class MainActivity extends AppCompatActivity {
    private Switch switchSecurite;
    private static TextView TextViewDate;
    private static TextView TextViewHeure;
    private boolean notifDesactivee;
    String[] perms = {"android.permission.POST_NOTIFICATION"};
    int permsRequestCode = 200;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                createNotificationChannel();
            }
        });
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

    private void createNotificationChannel() {

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
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),0,intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        TextViewHeure.setText(String.format("%d:%d:%d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
    }

}