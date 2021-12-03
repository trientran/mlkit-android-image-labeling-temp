/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.troyt.imagelabeling.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecognitionListViewModel : ViewModel() {

    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.

    private val _recognitionList = MutableLiveData<List<Recognition>>()
    val recognitionList: LiveData<List<Recognition>> get() = _recognitionList

    fun updateData(recognitions: List<Recognition>) {
        _recognitionList.postValue(recognitions)
    }

    fun addData(recognition: Recognition) {
        val currentList = _recognitionList.value as MutableList<Recognition>
        currentList.add(recognition)
        updateData(currentList)
    }
}

/**
 * Simple Data object with two fields for the label and probability
 */
data class Recognition(val label: String, val confidence: Float) {

    // For easy logging
    override fun toString(): String {
        return "$label / $probabilityString"
    }

    // Output probability as a string to enable easy data binding
    val probabilityString = String.format("%.1f%%", confidence * 100.0f)

    // get a vietnamese name for the herb (only display the name, not the herb's key)
    val vietHerb = label.substring(5) // coz the original label name is "0001 cam thảo"
}

fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}