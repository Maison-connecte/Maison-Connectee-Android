package Info420.maisonconnecte;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*----------------------------------------------------------------
 *  Auteur: Maxime Paulin.
 *
 *  Date de création: 6 avril 2023
 *
 *  Dernière date de modification: [2023-04-06]
 *
 *  Description: Permet de connecté le client au serveur où la caméra est branché pour recevoir le flux vidéo
 *----------------------------------------------------------------*/

/*----------------------------------------------------------------
 * Sources:
 *  - doc android : https://developer.android.com/reference/java/net/Socket
 *  - information supplémentaire : https://stackoverflow.com/questions/7384678/how-to-create-socket-connection-in-android
 *  - information supplémentaire : https://perihanmirkelam.medium.com/socket-programming-on-android-tcp-server-example-e4552a957c08
 *  - information supplémentaire : https://www.tutorialspoint.com/sending-and-receiving-data-with-sockets-in-android
 *  - Chat GPT v4
 *----------------------------------------------------------------*/

// Cette classe gère la connexion au serveur via un socket et la réception d'images
public class SocketClient implements Runnable {

    private final String adresseIP;
    private final int port;
    private final EcouteurImageRecue ecouteurImageRecue;
    private final ExecutorService serviceExecutor;

    private Socket socket;
    private InputStream fluxEntree;

    public SocketClient(String adresseIP, int port, EcouteurImageRecue ecouteurImageRecue) {
        this.adresseIP = adresseIP;
        this.port = port;
        this.ecouteurImageRecue = ecouteurImageRecue;
        serviceExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void run() {
        try {
            socket = new Socket(adresseIP, port);
            fluxEntree = socket.getInputStream();

            while (true) {
                byte[] octetsImage = lireImage(fluxEntree);
                if (octetsImage != null) {
                    ecouteurImageRecue.onImageReceived(octetsImage);
                }
            }

        } catch (IOException e) {
            Log.e("ClientSocket", "Erreur de connexion au serveur", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // Démarrer la connexion au serveur
    public void demarrer() {
        serviceExecutor.execute(() -> {
            try {
                socket = new Socket(adresseIP, port);
                fluxEntree = socket.getInputStream();

                while (true) {
                    byte[] octetsImage = lireImage(fluxEntree);
                    if (octetsImage != null) {
                        ecouteurImageRecue.onImageReceived(octetsImage);
                    }
                }

            } catch (IOException e) {
                Log.e("ClientSocket", "Erreur de connexion au serveur", e);
            }
        });
    }
    // Arrêter la connexion au serveur
    public void arreter() {
        try {
            if (fluxEntree != null) {
                fluxEntree.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e("ClientSocket", "Erreur à la fermeture des ressources", e);
        }
    }
    private static final String DELIMITEUR_CADRE = "---END_OF_FRAME---";
    private static final byte[] OCTETS_DELIMITEUR_CADRE = DELIMITEUR_CADRE.getBytes(StandardCharsets.UTF_8);

    // Lire l'image depuis le flux d'entrée
    private byte[] lireImage(InputStream fluxEntree) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] tampon = new byte[1000000];
        int octetsLus;
        int indiceDelimiteur = 0;

        while ((octetsLus = fluxEntree.read(tampon)) != -1) {
            baos.write(tampon, 0, octetsLus);

            // Vérifie si le délimiteur est trouvé dans les données reçues
            for (int i = 0; i < octetsLus; i++) {
                if (tampon[i] == OCTETS_DELIMITEUR_CADRE[indiceDelimiteur]) {
                    indiceDelimiteur++;
                    if (indiceDelimiteur == OCTETS_DELIMITEUR_CADRE.length) {
                        byte[] octetsImage = Arrays.copyOfRange(baos.toByteArray(), 0, baos.size() - OCTETS_DELIMITEUR_CADRE.length);
                        baos.reset();
                        return octetsImage;
                    }
                } else {
                    indiceDelimiteur = 0;
                }
            }
        }

        return null;
    }

    // Interface pour écouter lorsque une image est reçue
    public interface EcouteurImageRecue {
        void onImageReceived(byte[] octetsImage);
    }
}