package dev.troyt.imagelabeling.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.linkfirebase.FirebaseModelSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import dev.troyt.imagelabeling.R
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

    private fun updateData(recognitions: MutableList<Recognition>) {
        _recognitionList.value = recognitions
    }

    fun updateImageUri(imageUri: Uri) {
        _imageUri.value = imageUri
    }

    fun inferImage(
        context: Context,
        selectedImageUri: Uri,
        confidence: Float = 0.5f,
        maxResultsDisplayed: Int = 3,
    ) {
        // Create a new coroutine on the UI thread
        viewModelScope.launch(defaultDispatcher) {

            val bitmap = selectedImageUri.toScaledBitmap(context, 224, 224) ?: return@launch
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // set the minimum confidence required:
            val localModel = LocalModel.Builder().setAssetFilePath("new_model.tflite").build()

            // Specify the name you assigned in the Firebase console.
            val remoteModel = CustomRemoteModel
                .Builder(FirebaseModelSource.Builder("new_model").build())
                .build()

            RemoteModelManager.getInstance().isModelDownloaded(remoteModel)
                .addOnSuccessListener { isDownloaded ->
                    val optionsBuilder =
                        if (isDownloaded) {
                            Timber.d("Remote model being used")
                            CustomImageLabelerOptions.Builder(remoteModel)
                        } else {
                            Timber.d("Local model being used")
                            CustomImageLabelerOptions.Builder(localModel)
                        }
                    val options = optionsBuilder
                        .setConfidenceThreshold(confidence)
                        .setMaxResultCount(maxResultsDisplayed)
                        .build()

                    val labeler = ImageLabeling.getClient(options)
                    processImage(labeler, inputImage, maxResultsDisplayed, context)
                }
        }
    }

    private fun processImage(
        labeler: ImageLabeler,
        inputImage: InputImage,
        maxResultsDisplayed: Int,
        context: Context
    ) {
        labeler.process(inputImage)
            .addOnSuccessListener {
                val recognitionList = mutableListOf<Recognition>()
                for (i in 0 until maxResultsDisplayed) {
                    try {
                        recognitionList.add(
                            Recognition(
                                label = it[i].text + " " + it[i].index,
                                confidence = it[i].confidence
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
                Timber.d(recognitionList.toString())
                updateData(recognitionList)
            }
            .addOnFailureListener {
                Timber.e(it.message ?: "Some error")
            }
    }
}



