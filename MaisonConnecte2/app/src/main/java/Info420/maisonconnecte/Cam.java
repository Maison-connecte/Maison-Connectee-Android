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

public class Cam extends AppCompatActivity implements TextureView.SurfaceTextureListener, SocketClient.ImageReceivedListener {
    private TextureView textureView;
    private SocketClient socketClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textureView = findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);

        socketClient = new SocketClient("192.168.0.6", 8010, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        socketClient.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        socketClient.stop();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void setAspectRatio(TextureView textureView, int width, int height) {
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        float aspectRatio = (float) width / height;
        float viewAspectRatio = (float) viewWidth / viewHeight;

        if (aspectRatio > viewAspectRatio) {
            layoutParams.width = viewWidth;
            layoutParams.height = (int) (viewWidth / aspectRatio);
        } else {
            layoutParams.height = viewHeight;
            layoutParams.width = (int) (viewHeight * aspectRatio);
        }

        textureView.setLayoutParams(layoutParams);
    }
    @Override
    public void onImageReceived(byte[] imageBytes) {
        runOnUiThread(() -> {
            try {
                String base64String = new String(imageBytes);
                byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (bitmap != null && textureView.isAvailable()) {
                    int bitmapWidth = bitmap.getWidth();
                    int bitmapHeight = bitmap.getHeight();
                    setAspectRatio(textureView, bitmapWidth, bitmapHeight);

                    textureView.getSurfaceTexture().setDefaultBufferSize(bitmapWidth, bitmapHeight);
                    Canvas canvas = textureView.lockCanvas();
                    if (canvas != null) {
                        canvas.drawBitmap(bitmap, 0, 0, null);
                        textureView.unlockCanvasAndPost(canvas);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}