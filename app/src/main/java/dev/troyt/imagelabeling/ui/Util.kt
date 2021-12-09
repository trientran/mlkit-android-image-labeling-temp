package dev.troyt.imagelabeling.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import timber.log.Timber.DebugTree


fun Uri.toScaledBitmap(context: Context, width: Int = 224, height: Int = 224): Bitmap? {
    val bitmapFromUri: Bitmap? =
        // check version of Android on device
        try {
            if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(context.contentResolver, this)
                ImageDecoder.decodeBitmap(source)
            } else {
                // support older versions of Android by using getBitmap
                MediaStore.Images.Media.getBitmap(context.contentResolver, this)
            }
        } catch (e: Exception) {
            null
        }

    val resizedBitmap = bitmapFromUri?.let { Bitmap.createScaledBitmap(it, width, height, false) }

    return resizedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
}

/**
 * Simple Data object with two fields for the label and probability
 */
data class Recognition(val label: String, val confidence: Float, val imageUri: Uri? = null) {
    // Output probability as a string to enable easy data binding
    val confidencePercentage = String.format("%.1f%%", confidence * 100.0f)
}

class ExampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
    }
}

val requiredPermissions = arrayOf(Manifest.permission.CAMERA) // permission needed
val defaultDispatcher = Dispatchers.Default
val mainDispatcher = Dispatchers.Main
val ioDispatcher = Dispatchers.IO

