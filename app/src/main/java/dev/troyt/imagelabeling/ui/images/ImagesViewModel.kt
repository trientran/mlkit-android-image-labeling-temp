package dev.troyt.imagelabeling.ui.images

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.Recognition
import dev.troyt.imagelabeling.ui.home.HomeViewModel
import dev.troyt.imagelabeling.ui.toScaledBitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class ImagesViewModel : ViewModel() {

    private val tag = HomeViewModel::class.simpleName

    // Backing property to avoid state updates from other classes
    private val _recognitionList = MutableStateFlow<MutableList<Recognition>>(mutableListOf())

    // The UI collects from this StateFlow to get its state updates
    val recognitionList: StateFlow<MutableList<Recognition>> get() = this._recognitionList

    fun addData(recognition: Recognition) {
        val currentList = _recognitionList.value.toMutableList()
        currentList.add(recognition)
        _recognitionList.value = currentList
    }

    fun clearAllData() {
        _recognitionList.value = mutableListOf()
    }

    @ExperimentalCoroutinesApi
    fun inferImages(
        context: Context,
        clipData: ClipData,
        confidence: Float = 0.7f,
    ) = callbackFlow {
        // set the minimum confidence required:
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(confidence)
            .build()

        val labeler = ImageLabeling.getClient(options)

        for (i in 0 until clipData.itemCount) {
            val selectedImageUri: Uri = clipData.getItemAt(i).uri
            val bitmap = selectedImageUri.toScaledBitmap(context, 224, 224) ?: return@callbackFlow
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            var recognition: Recognition

            labeler.process(inputImage)
                .addOnSuccessListener { results ->
                    recognition = try {
                        Recognition(
                            label = results[0].text + " " + results[0].index,
                            confidence = results[0].confidence,
                            imageUri = selectedImageUri
                        )
                    } catch (e: IndexOutOfBoundsException) {
                        Recognition(
                            label = context.getString(R.string.no_result),
                            confidence = 0f,
                            imageUri = selectedImageUri
                        )
                    }
                    trySend(recognition)
                }
                .addOnFailureListener {
                    Log.e(tag, it.localizedMessage ?: "some error")
                }
        }
        awaitClose { labeler.close() }
    }
}