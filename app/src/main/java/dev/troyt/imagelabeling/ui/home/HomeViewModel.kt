package dev.troyt.imagelabeling.ui.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.Recognition
import dev.troyt.imagelabeling.ui.defaultDispatcher
import dev.troyt.imagelabeling.ui.toScaledBitmap
import kotlinx.coroutines.launch


class HomeViewModel : ViewModel() {
    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.
    private val tag = HomeViewModel::class.simpleName

    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri> get() = _imageUri
    private val _recognitionList = MutableLiveData<MutableList<Recognition>>(mutableListOf())
    val recognitionList: LiveData<MutableList<Recognition>> get() = _recognitionList

    private fun updateData(recognitions: MutableList<Recognition>) {
        _recognitionList.value = recognitions
    }

    fun setImageUri(imageUri: Uri) {
        _imageUri.value = imageUri
    }

    fun inferImage(
        context: Context,
        selectedImageUri: Uri,
        confidence: Float = 0.7f,
        maxResultsDisplayed: Int = 3
    ) {
        // Create a new coroutine on the UI thread
        viewModelScope.launch(defaultDispatcher) {

            val bitmap = selectedImageUri.toScaledBitmap(context, 224, 224)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // set the minimum confidence required:
            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(confidence)
                .build()

            val labeler = ImageLabeling.getClient(options)
            labeler.process(inputImage)
                .addOnSuccessListener { results ->
                    val recognitionList = mutableListOf<Recognition>()
                    for (i in 0 until maxResultsDisplayed) {
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
                    Log.d(tag, recognitionList.toString())
                    updateData(recognitionList)
                }
        }
    }
}



