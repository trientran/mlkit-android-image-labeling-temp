package dev.troyt.imagelabeling.ui.home

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.dashboard.toScaledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class RecognitionViewModel(context: Context, clipData: ClipData) : ViewModel() {

    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.

    init {
        viewModelScope.launch(Dispatchers.Default) {
            // Trigger the flow and consume its elements using collect
            predictImageFlow(context, clipData)
                .onEach {
                    _list.value.add(it)
                    Log.d("trien model", _list.value.toString())
                }
        }
    }

    private val _recognitionList = MutableLiveData<MutableList<Recognition>>(mutableListOf())
    val recognitionList: LiveData<MutableList<Recognition>> get() = _recognitionList

    // Backing property to avoid state updates from other classes
    private val _list = MutableStateFlow(mutableListOf<Recognition>())

    // The UI collects from this StateFlow to get its state updates
    val uiState: StateFlow<List<Recognition>> = _list

    private fun predictImageFlow(context: Context, clipData: ClipData) = channelFlow {

        println("Trien Current Thread name is ${Thread.currentThread().name}")

        for (i in 0 until clipData.itemCount) {
            val selectedImageUri: Uri = clipData.getItemAt(i).uri
            val bitmap = selectedImageUri.toScaledBitmap(context, 224, 224)
            if (bitmap != null) {
                //predictImage(imageUri, bitmap)
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                // set the minimum confidence required:
                val options = ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.7f)
                    .build()

                val labeler = ImageLabeling.getClient(options)

                var recognition: Recognition

                labeler.process(inputImage)
                    .addOnSuccessListener { results ->
                        recognition = try {
                            Recognition(
                                imageUri = selectedImageUri,
                                label = results[0].text + " " + results[0].index,
                                confidence = results[0].confidence
                            )
                        } catch (e: Exception) {
                            Recognition(
                                imageUri = selectedImageUri,
                                label = context.getString(R.string.no_result),
                                confidence = 0f
                            )
                        }
                        trySend(recognition)
                    }
                    .addOnFailureListener {
                        Log.e("Error", it.localizedMessage ?: "some error")
                    }
            }
        }
        awaitClose()
    }.shareIn(
        viewModelScope,
        replay = 1,
        started = SharingStarted.WhileSubscribed()
    )

    fun updateData(recognitions: MutableList<Recognition>) {
        _recognitionList.value = recognitions
    }

    fun addData(recognition: Recognition) {
        _recognitionList.value?.add(recognition)
    }

    /* fun addData(recognition: Recognition) {
         val newList = mutableListOf<Recognition>()
         _recognitionList.value?.let {
             newList.addAll(it)
             newList.add(recognition)
             updateData(newList)
         }
     }*/

    fun clearAllData() {
        _recognitionList.value?.clear()
    }

    class Factory(context: Context, clipData: ClipData) : ViewModelProvider.Factory {
        override fun <RecognitionViewModel : ViewModel> create(modelClass: Class<RecognitionViewModel>): RecognitionViewModel {
            return RecognitionViewModel(context = context, clipData = clipData)
        }
//        @Suppress("UNCHECKED_CAST")
//        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//            return NewsViewModel(newsService) as T
//        }

    }
}



/**
 * Simple Data object with two fields for the label and probability
 */
data class Recognition(val imageUri: Uri? = null, val label: String, val confidence: Float) {
    // Output probability as a string to enable easy data binding
    val confidencePercentage = String.format("%.1f%%", confidence * 100.0f)

    // For easy logging
    override fun toString(): String {
        return "$label / $confidencePercentage"
    }
}

// Represents different states for the LatestNews screen
/*
sealed class LatestNewsUiState(val label: String, val confidence: Float) {
    data class CameraRecognition(news: List<ArticleHeadline>): LatestNewsUiState()
    data class SingleImageRecognition(exception: Throwable): LatestNewsUiState()
}*/
