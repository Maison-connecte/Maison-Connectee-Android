package Info420.maisonconnecte;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/*----------------------------------------------------------------
 *  Auteur: Maxime Paulin
 *
 *  Date de création: 10 Mai 2023
 *
 *  Dernière date de modification: [2023-05-10]
 *
 *  Description: Gère les préférences pour le mode sécurité
 *----------------------------------------------------------------*/

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Récupère le message de l'intent
        String message = intent.getStringExtra("message");
        Log.d("DEBUG", "message: " + message);
        // Publie le message à travers la MainActivity
        MainActivity.publierMessage(message);

        // Après avoir géré l'alarme, envoie un broadcast
        SharedPreferences sharedPreferences = context.getSharedPreferences("preferences_application", MODE_PRIVATE);
        SharedPreferences.Editor editeur = sharedPreferences.edit();
        if (message.equals("1")){
            // Si le message est "1", met l'état du bouton à bascule à vrai
            editeur.putBoolean("etatBascule", true);
        }
        else {
            // Si le message n'est pas "1", met l'état du bouton à bascule à faux
            editeur.putBoolean("etatBascule", false);
        }
        editeur.apply();
    }
}
