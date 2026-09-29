package com.example.screenreceiver;

import android.view.Surface;

public class NativeBridge {
    static {
        System.loadLibrary("tabletdecoder");
    }

    public native void setSurface(Surface surface);
    public native void startReceiver();
    public native void stopReceiver();
}