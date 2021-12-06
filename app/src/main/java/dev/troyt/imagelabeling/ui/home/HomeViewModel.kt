package dev.troyt.imagelabeling.ui.home

import android.content.ClipData
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.dashboard.toScaledBitmap
import dev.troyt.imagelabeling.ui.notifications.Recognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.IndexOutOfBoundsException

class RecognitionViewModel : ViewModel() {

    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.

    private val _recognitionList = MutableLiveData<MutableList<Recognition>>(mutableListOf())
    val recognitionList: LiveData<MutableList<Recognition>> get() = _recognitionList

    fun updateData(recognitions: MutableList<Recognition>) {
        _recognitionList.value = recognitions
    }

    fun addData(recognition: Recognition) {
        _recognitionList.value?.add(recognition)
    }

}