package dev.troyt.imagelabeling.ui.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.Recognition

class ImageAnalyzer(
    private val context: Context,
    private val maxResultsDisplayed: Int = 3,
    private val listener: RecognitionListener,
) : ImageAnalysis.Analyzer {

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        val recognitionList = mutableListOf<Recognition>()
        val inputImage = imageProxy.image?.let {
            InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
        }

        // set the minimum confidence required:
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

        val labeler = ImageLabeling.getClient(options)
        inputImage?.let {
            labeler.process(it)
                .addOnSuccessListener { results ->
                    for (i in 0 until maxResultsDisplayed) {
                        try {
                            recognitionList.add(
                                Recognition(
                                    label = results[i].text + " " + results[i].index,
                                    confidence = results[i].confidence
                                )
                            )
                        } catch (e: Exception) {
                            recognitionList.add(
                                Recognition(
                                    label = context.getString(R.string.no_result),
                                    confidence = 0f
                                )
                            )
                        }
                    }
                    // Return the result
                    listener(recognitionList)
                    // Close the image,this tells CameraX to feed the next image to the analyzer
                    imageProxy.close()
                }
                .addOnFailureListener {
                    Log.e("Error", it.localizedMessage ?: "some error")
                }
        }
    }
}