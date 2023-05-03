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

public class SocketClient implements Runnable {

    private final String ipAddress;
    private final int port;
    private final ImageReceivedListener imageReceivedListener;
    private final ExecutorService executorService;

    private Socket socket;
    private InputStream inputStream;

    public SocketClient(String ipAddress, int port, ImageReceivedListener imageReceivedListener) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.imageReceivedListener = imageReceivedListener;
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void run() {
        try {
            socket = new Socket(ipAddress, port);
            inputStream = socket.getInputStream();

            while (true) {
                byte[] imageBytes = readImage(inputStream);
                if (imageBytes != null) {
                    imageReceivedListener.onImageReceived(imageBytes);
                }
            }

        } catch (IOException e) {
            Log.e("SocketClient", "Error connecting to server", e);
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
    public void start() {
        executorService.execute(() -> {
            try {
                socket = new Socket(ipAddress, port);
                inputStream = socket.getInputStream();

                while (true) {
                    byte[] imageBytes = readImage(inputStream);
                    if (imageBytes != null) {
                        imageReceivedListener.onImageReceived(imageBytes);
                    }
                }

            } catch (IOException e) {
                Log.e("SocketClient", "Error connecting to server", e);
            }
        });
    }
    public void stop() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e("SocketClient", "Error closing resources", e);
        }
    }
    private static final String FRAME_DELIMITER = "---END_OF_FRAME---";
    private static final byte[] FRAME_DELIMITER_BYTES = FRAME_DELIMITER.getBytes(StandardCharsets.UTF_8);

    private byte[] readImage(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1000000];
        int bytesRead;
        int delimiterIndex = 0;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);

            // Check if the delimiter is found in the received data
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == FRAME_DELIMITER_BYTES[delimiterIndex]) {
                    delimiterIndex++;
                    if (delimiterIndex == FRAME_DELIMITER_BYTES.length) {
                        byte[] imageBytes = Arrays.copyOfRange(baos.toByteArray(), 0, baos.size() - FRAME_DELIMITER_BYTES.length);
                        baos.reset();
                        return imageBytes;
                    }
                } else {
                    delimiterIndex = 0;
                }
            }
        }

        return null;
    }
    public interface ImageReceivedListener {
        void onImageReceived(byte[] imageBytes);
    }
}
