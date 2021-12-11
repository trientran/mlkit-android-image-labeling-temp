package dev.troyt.imagelabeling.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.ui.Recognition
import dev.troyt.imagelabeling.ui.defaultDispatcher
import dev.troyt.imagelabeling.ui.toScaledBitmap
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel : ViewModel() {
    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.

    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri> get() = _imageUri
    private val _recognitionList = MutableLiveData<MutableList<Recognition>>(mutableListOf())
    val recognitionList: LiveData<MutableList<Recognition>> get() = _recognitionList

    private val _modelUri = MutableLiveData<Uri>()
    val modelUri: LiveData<Uri> get() = _modelUri

    lateinit var localModel: LocalModel

    private fun updateData(recognitions: MutableList<Recognition>) {
        _recognitionList.value = recognitions
    }

    fun updateImageUri(imageUri: Uri) {
        _imageUri.value = imageUri
    }

    fun createTFLiteModel(modelUri: Uri) {
        localModel = LocalModel.Builder()
            .setUri(modelUri)
            .build()
    }

    fun inferImage(
        context: Context,
        selectedImageUri: Uri,
        confidence: Float = 0.7f,
        maxResultsDisplayed: Int = 3,
    ) {
        // Create a new coroutine on the UI thread
        viewModelScope.launch(defaultDispatcher) {

            val bitmap = selectedImageUri.toScaledBitmap(context, 224, 224) ?: return@launch
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // set the minimum confidence required:
            val options = ImageLabelerOptions.Builder().setConfidenceThreshold(confidence).build()

            val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
                .setConfidenceThreshold(0.5f)
                .setMaxResultCount(3)
                .build()

            val labeler = ImageLabeling.getClient(customImageLabelerOptions)
            labeler.process(inputImage)
                .addOnSuccessListener {
                    val recognitionList = mutableListOf<Recognition>()
                    for (i in 0 until maxResultsDisplayed) {
                        recognitionList.add(
                            Recognition(
                                label = it[i].text + " " + it[i].index,
                                confidence = it[i].confidence
                            )
                        )
                    }
                    Timber.d(recognitionList.toString())
                    updateData(recognitionList)
                }
                .addOnFailureListener {
                    Timber.e(it.message ?: "Some error")
                }
        }
    }
}



