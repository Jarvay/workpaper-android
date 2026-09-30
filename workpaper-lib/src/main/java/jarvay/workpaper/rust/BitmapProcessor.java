package jarvay.workpaper.rust;

import android.graphics.Bitmap;

public class BitmapProcessor {
    static {
        System.loadLibrary("workpaper_lib");
    }

    // Test JNI connection
    public static native String testConnection();

    // Test passthrough - returns input unchanged
    public static native Bitmap passthrough(Bitmap bitmap);

    // Native methods that correspond to our Rust functions
    public static native Bitmap scaleFixedRatio(Bitmap bitmap, int targetWidth, int targetHeight, boolean useMin);

    public static native Bitmap centerCrop(Bitmap bitmap, int targetWidth, int targetHeight);

    public static native String getInfo(Bitmap bitmap);

    public static native Bitmap blur(Bitmap bitmap, int radius);

    public static native Bitmap noise(Bitmap bitmap, int percent);

    public static native Bitmap effect(Bitmap bitmap, int brightness, int contrast, int saturation);

    public static native Bitmap setAlpha(Bitmap bitmap, int alpha);
}