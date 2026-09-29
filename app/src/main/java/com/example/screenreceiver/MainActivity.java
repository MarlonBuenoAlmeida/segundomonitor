package com.example.segundomonitor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends Activity {

    private static final String TAG = "SegundoMonitor";
    private static final int PORT = 9090;

    private ImageView imageView;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Criar a ImageView programaticamente em fullscreen simples
        imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        setContentView(imageView);

        Log.d(TAG, "Iniciando aplicação...");

        // Iniciar thread em segundo plano estrita para a rede
        startSocketServer();
    }

    private void startSocketServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT);
                    Log.d(TAG, "Servidor escutando na porta " + PORT);

                    while (isRunning) {
                        Socket socket = null;
                        try {
                            socket = serverSocket.accept();
                            Log.d(TAG, "Conexão recebida do PC!");

                            InputStream inputStream = socket.getInputStream();
                            DataInputStream dataInputStream = new DataInputStream(inputStream);

                            while (isRunning && !socket.isClosed()) {
                                // Lê o tamanho da imagem enviado pelo PC
                                int length = dataInputStream.readInt();

                                if (length > 0 && length < 5000000) {
                                    byte[] frameData = new byte[length];
                                    dataInputStream.readFully(frameData, 0, length);

                                    // Decodificação rápida sem estourar a memória
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inPreferredConfig = Bitmap.Config.RGB_565;

                                    final Bitmap bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length, options);

                                    if (bitmap != null) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                imageView.setImageBitmap(bitmap);
                                            }
                                        });
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erro na sessão com o PC: " + e.getMessage());
                        } finally {
                            if (socket != null && !socket.isClosed()) {
                                try {
                                    socket.close();
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro fatal ao iniciar ServerSocket: " + e.getMessage());
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }
}