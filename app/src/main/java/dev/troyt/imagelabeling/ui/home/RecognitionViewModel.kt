package dev.troyt.imagelabeling.ui.home

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecognitionViewModel : ViewModel() {

    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.

    private val _recognitionList = MutableLiveData<MutableList<Recognition>>(mutableListOf())
    val recognitionList: LiveData<MutableList<Recognition>> get() = _recognitionList

    // Backing property to avoid state updates from other classes
    private val _list = MutableStateFlow(mutableListOf<Recognition>())

    // The UI collects from this StateFlow to get its state updates
    val uiState: StateFlow<List<Recognition>> = _list


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