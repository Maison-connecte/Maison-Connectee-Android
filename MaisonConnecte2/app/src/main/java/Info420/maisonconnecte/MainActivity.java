package Info420.maisonconnecte;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button boutonService;
    private Button boutonArret;
    private static TextView TextViewDate;
    private static TextView TextViewHeure;
    private static TextView TextViewIntensite;
    static NotificationManager NM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boutonService = (Button) findViewById(R.id.btnService);
        boutonService.setOnClickListener(this);
        boutonArret = (Button) findViewById(R.id.btnArret);
        boutonArret.setOnClickListener(this);

        //Capteur ultrason
        TextViewDate = (TextView) findViewById(R.id.DateContenu);
        TextViewHeure = (TextView) findViewById(R.id.HeureContenu);
        TextViewIntensite = (TextView) findViewById(R.id.IntensiteContenu);

        //Notifications
        NM=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

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
            case R.id.itemCon:
                startActivity(new Intent(MainActivity.this, ConnexionMQTT.class));
                break;
        }
        return true;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnService:
                startService(new Intent(this, ServiceReception.class));
                break;
            case R.id.btnArret:
                stopService(new Intent(this, ServiceReception.class));
                break;
        }
    }

    public static void CapteurUltrason(Date timestamp, int volume) {
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(timestamp);   // assigns calendar to given date
        if (volume > 60)
        {
            TextViewDate.setText(String.format("%d-%d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)));
            TextViewHeure.setText(String.format("%d:%d:%d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
            TextViewIntensite.setText(Integer.toString(volume));

            Notification notify = new Notification(android.R.drawable.stat_notify_more,"Alerte mouvement",System.currentTimeMillis());
            PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(),0);
            //notify.setLatestEventInfo(getApplicationContext(), subject, body,pending);
            NM.notify(0, notify);
        }
    }

}