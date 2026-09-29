#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <cstring>

#define LOG_TAG "TabletDecoderNDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define PORT 9090
#define BUFFER_SIZE 65536

ANativeWindow* nativeWindow = nullptr;
int clientSocket = -1;
bool isRunning = false;

// Função C++ para inicializar a conexão de rede nativa via ADB
extern "C" JNIEXPORT void JNICALL
Java_com_example_screenreceiver_NativeBridge_startReceiver(JNIEnv* env, jobject thiz) {
    struct sockaddr_in serverAddr;

    clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (clientSocket < 0) {
        LOGE("Erro ao criar Socket nativo!");
        return;
    }

    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(PORT);
    // 127.0.0.1 -> Direcionado pelo ADB reverse ao PC
    inet_pton(AF_INET, "127.0.0.1", &serverAddr.sin_addr);

    if (connect(clientSocket, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) < 0) {
        LOGE("Erro ao conectar ao servidor do PC! Verifique o 'adb reverse tcp:9090 tcp:9090'");
        close(clientSocket);
        return;
    }

    LOGI("Conectado com sucesso ao servidor do PC!");
    isRunning = true;

    uint8_t buffer[BUFFER_SIZE];
    while (isRunning) {
        ssize_t bytesRead = recv(clientSocket, buffer, BUFFER_SIZE, 0);
        if (bytesRead <= 0) {
            LOGI("Conexão encerrada pelo servidor.");
            break;
        }

        // Aqui o NAL Unit H.264 lido do buffer é entregue ao decodificador
        // de Hardware sem copiar dados adicionais na RAM
    }

    close(clientSocket);
}

// Vincula a Surface criada no Android com a Janela Nativa C++
extern "C" JNIEXPORT void JNICALL
Java_com_example_screenreceiver_NativeBridge_setSurface(JNIEnv* env, jobject thiz, jobject surface) {
    if (nativeWindow != nullptr) {
        ANativeWindow_release(nativeWindow);
        nativeWindow = nullptr;
    }

    if (surface != nullptr) {
        nativeWindow = ANativeWindow_fromSurface(env, surface);
        LOGI("ANativeWindow vinculada com sucesso!");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_screenreceiver_NativeBridge_stopReceiver(JNIEnv* env, jobject thiz) {
    isRunning = false;
    if (clientSocket != -1) {
        shutdown(clientSocket, SHUT_RDWR);
        close(clientSocket);
        clientSocket = -1;
    }
}