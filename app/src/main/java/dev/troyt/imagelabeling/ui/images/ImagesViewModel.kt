package dev.troyt.imagelabeling.ui.images

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.Recognition
import dev.troyt.imagelabeling.ui.home.HomeViewModel
import dev.troyt.imagelabeling.ui.toScaledBitmap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

class ImagesViewModel : ViewModel() {

    private val tag = HomeViewModel::class.simpleName
    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.

    // Backing property to avoid state updates from other classes
    private val _uiState = MutableStateFlow<MutableList<Recognition>>(mutableListOf())

    // The UI collects from this StateFlow to get its state updates
    val uiState: StateFlow<MutableList<Recognition>> get() = _uiState

    private val _recognitionList = MutableLiveData<MutableList<Recognition>>(mutableListOf())
    val recognitionList: LiveData<MutableList<Recognition>> get() = _recognitionList

    fun updateFlowData(recognitions: MutableList<Recognition>) {
        _uiState.value = recognitions
        Log.d("trienz", uiState.value.toString())
    }

    fun updateData(recognitions: MutableList<Recognition>) {
        _recognitionList.value = recognitions
    }

    fun addFlowData(recognition: Recognition) {
        _recognitionList.value?.add(recognition)
    }

    fun addData(recognition: Recognition) {
        _recognitionList.value?.add(recognition)
    }

    fun clearAllData() {
        _recognitionList.value?.clear()
    }

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
        this.awaitClose()
    }
}