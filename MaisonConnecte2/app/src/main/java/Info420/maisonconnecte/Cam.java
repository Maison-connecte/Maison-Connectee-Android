package Info420.maisonconnecte;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.ViewGroup;

// Cette classe gère la caméra dans l'application
public class Cam extends AppCompatActivity implements TextureView.SurfaceTextureListener, SocketClient.EcouteurImageRecue {
    private TextureView vueTexture; // Vue de la texture
    private SocketClient clientSocket; // Client de socket

    @Override
    protected void onCreate(Bundle savedInstanceState) { // À la création de l'activité
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialisation de la vue de la texture
        vueTexture = findViewById(R.id.texture_view);
        vueTexture.setSurfaceTextureListener(this);

        // Initialisation du client de socket
        clientSocket = new SocketClient("192.168.0.6", 8010, this);
    }

    @Override
    protected void onResume() { // À la reprise de l'activité
        super.onResume();
        clientSocket.demarrer();
    }

    @Override
    protected void onPause() { // À la mise en pause de l'activité
        super.onPause();
        clientSocket.arreter();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // Gestion des actions de menu
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Définir le rapport d'aspect de la vue de la texture
    private void definirRapportAspect(TextureView vueTexture, int width, int height) {
        ViewGroup.LayoutParams parametresLayout = vueTexture.getLayoutParams();
        int largeurVue = vueTexture.getWidth();
        int hauteurVue = vueTexture.getHeight();
        float rapportAspect = (float) width / height;
        float rapportAspectVue = (float) largeurVue / hauteurVue;

        if (rapportAspect > rapportAspectVue) {
            parametresLayout.width = largeurVue;
            parametresLayout.height = (int) (largeurVue / rapportAspect);
        } else {
            parametresLayout.height = hauteurVue;
            parametresLayout.width = (int) (hauteurVue * rapportAspect);
        }

        vueTexture.setLayoutParams(parametresLayout);
    }

    @Override
    public void onImageReceived(byte[] octetsImage) { // À la réception d'une image
        runOnUiThread(() -> {
            try {
                String chaineBase64 = new String(octetsImage);
                byte[] chaineDecodee = Base64.decode(chaineBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(chaineDecodee, 0, chaineDecodee.length);

                if (bitmap != null && vueTexture.isAvailable()) {
                    int largeurBitmap = bitmap.getWidth();
                    int hauteurBitmap = bitmap.getHeight();
                    // Définir le rapport d'aspect pour le bitmap
                    definirRapportAspect(vueTexture, largeurBitmap, hauteurBitmap);

                    vueTexture.getSurfaceTexture().setDefaultBufferSize(largeurBitmap, hauteurBitmap);
                    Canvas toile = vueTexture.lockCanvas();
                    if (toile != null) {
                        toile.drawBitmap(bitmap, 0, 0, null);
                        vueTexture.unlockCanvasAndPost(toile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}