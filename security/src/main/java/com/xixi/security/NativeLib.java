package com.xixi.security;

public class NativeLib {

    // Used to load the 'security' library on application startup.
    static {
        System.loadLibrary("security");
    }

    /**
     * A native method that is implemented by the 'security' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native String baseUrl();
}