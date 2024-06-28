package com.xixi.security;

public class Monitor {

    // Used to load the 'security' library on application startup.
    static {
        System.loadLibrary("security");
    }

    public native String baseUrl();

    public native String mqttHost();

    public native int mqttPort();

    public native String mqttUsername();

    public native String mqttPassword();

    public native String getUserId();
}