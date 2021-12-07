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

package dev.troyt.imagelabeling.ui.images

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.troyt.imagelabeling.databinding.ImageRecognitionItemBinding

class ImagesAdapter(private val context: Context) :
    ListAdapter<Recognition, ImageRecognitionItemViewHolder>(RecognitionDiffUtil()) {

    /**
     * Inflating the ViewHolder with recognition_item layout and data binding
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageRecognitionItemViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ImageRecognitionItemBinding.inflate(inflater, parent, false)
        return ImageRecognitionItemViewHolder(binding)
    }

    // Binding the data fields to the RecognitionViewHolder
    override fun onBindViewHolder(holderImage: ImageRecognitionItemViewHolder, position: Int) {
        holderImage.bindTo(getItem(position))
    }

    private class RecognitionDiffUtil : DiffUtil.ItemCallback<Recognition>() {
        override fun areItemsTheSame(oldItem: Recognition, newItem: Recognition): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(oldItem: Recognition, newItem: Recognition): Boolean {
            return oldItem.confidence == newItem.confidence
        }
    }
}

class ImageRecognitionItemViewHolder(private val binding: ImageRecognitionItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    // Binding all the fields to the view - to see which UI element is bind to which field, check
    // out layout/recognition_item.xml
    fun bindTo(recognition: Recognition) {
        binding.labelTextView.text = recognition.label
        binding.confidenceTextView.text = recognition.confidencePercentage
    }
}
