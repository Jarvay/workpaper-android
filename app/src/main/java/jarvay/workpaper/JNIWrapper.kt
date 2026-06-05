package jarvay.workpaper

import jarvay.workpaper.rust.BitmapProcessor

class JNIWrapper {
    companion object {
        // Test JNI connection
        @JvmStatic
        fun testConnection(): String {
            return BitmapProcessor.testConnection()
        }

        // Test passthrough - returns input unchanged
        @JvmStatic
        fun passthrough(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
            return BitmapProcessor.passthrough(bitmap)
        }

        // Use the BitmapProcessor from the workpaper-lib module
        @JvmStatic
        fun scaleFixedRatio(
            bitmap: android.graphics.Bitmap,
            targetWidth: Int,
            targetHeight: Int,
            useMin: Boolean
        ): android.graphics.Bitmap {
            return BitmapProcessor.scaleFixedRatio(bitmap, targetWidth, targetHeight, useMin)
        }

        @JvmStatic
        fun centerCrop(
            bitmap: android.graphics.Bitmap,
            targetWidth: Int,
            targetHeight: Int
        ): android.graphics.Bitmap {
            return BitmapProcessor.centerCrop(bitmap, targetWidth, targetHeight)
        }

        @JvmStatic
        fun getInfo(bitmap: android.graphics.Bitmap): String {
            return BitmapProcessor.getInfo(bitmap)
        }

        @JvmStatic
        fun blur(bitmap: android.graphics.Bitmap, radius: Int): android.graphics.Bitmap {
            return BitmapProcessor.blur(bitmap, radius)
        }

        @JvmStatic
        fun noise(bitmap: android.graphics.Bitmap, percent: Int): android.graphics.Bitmap {
            return BitmapProcessor.noise(bitmap, percent)
        }

        @JvmStatic
        fun effect(
            bitmap: android.graphics.Bitmap,
            brightness: Int,
            contrast: Int,
            saturation: Int
        ): android.graphics.Bitmap {
            return BitmapProcessor.effect(bitmap, brightness, contrast, saturation)
        }

        @JvmStatic
        fun setAlpha(bitmap: android.graphics.Bitmap, alpha: Int): android.graphics.Bitmap {
            return BitmapProcessor.setAlpha(bitmap, alpha)
        }
    }
}