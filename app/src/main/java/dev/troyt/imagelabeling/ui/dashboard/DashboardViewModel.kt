package dev.troyt.imagelabeling.ui.dashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.home.MAX_RESULT_DISPLAY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class DashboardViewModel : ViewModel() {

    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.

    private val _recognitionList = MutableLiveData<MutableList<Recognition>>(mutableListOf())
    val recognitionList: LiveData<MutableList<Recognition>> get() = _recognitionList

    private fun updateData(recognitions: MutableList<Recognition>) {
        _recognitionList.value = recognitions
    }

    fun inferImage(context: Context, selectedImageUri: Uri) {

        println("Trien1 Current Thread name is ${Thread.currentThread().name}")

        // Create a new coroutine on the UI thread
        viewModelScope.launch {

            val bitmap = selectedImageUri.toScaledBitmap(context, 224, 224) ?: return@launch

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognitionList = mutableListOf<Recognition>()

            // set the minimum confidence required:
            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()

            val labeler: ImageLabeler = ImageLabeling.getClient(options)
            labeler.process(inputImage)
                .addOnSuccessListener { results ->
                    println("Trien2 Current Thread name is ${Thread.currentThread().name}")
                    for (i in 0 until MAX_RESULT_DISPLAY) {
                        try {
                            recognitionList.add(
                                Recognition(
                                    label = results[i].text + " " + results[i].index,
                                    confidence = results[i].confidence
                                )
                            )
                        } catch (e: IndexOutOfBoundsException) {
                            recognitionList.add(
                                Recognition(
                                    label = context.getString(R.string.no_result),
                                    confidence = 0f
                                )
                            )
                        }
                    }
                    // Update the recognition result list
                    updateData(recognitionList)
                }
                .addOnFailureListener {
                    Log.e("Error", it.localizedMessage ?: "some error")
                }
                .result // TODO: 6/12/21  

        }
    }
}

/**
 * Simple Data object with two fields for the label and probability
 */
data class Recognition(val label: String, val confidence: Float) {
    // Output probability as a string to enable easy data binding
    val confidencePercentage = String.format("%.1f%%", confidence * 100.0f)

    // For easy logging
    override fun toString(): String {
        return "$label / $confidencePercentage"
    }
}

suspend fun Uri.toScaledBitmap(context: Context, width: Int, height: Int): Bitmap? {

    return withContext(Dispatchers.Default) {
        Log.d("Trien3 Current Thread name is ", Thread.currentThread().name)
        val bitmapFromUri: Bitmap? = try {
            // check version of Android on device
            if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(context.contentResolver, this@toScaledBitmap)
                ImageDecoder.decodeBitmap(source)
            } else {
                // support older versions of Android by using getBitmap
                MediaStore.Images.Media.getBitmap(context.contentResolver, this@toScaledBitmap)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

        val resizedBitmap =
            bitmapFromUri?.let {
                Bitmap.createScaledBitmap(
                    it,
                    width,
                    height,
                    false
                )
            }
        resizedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
    }
}