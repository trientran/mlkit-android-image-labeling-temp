package dev.troyt.imagelabeling.ui.camera

import android.content.Context
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.linkfirebase.FirebaseModelSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.ui.LOCAL_TFLITE_MODEL_NAME
import dev.troyt.imagelabeling.ui.REMOTE_TFLITE_MODEL_NAME
import dev.troyt.imagelabeling.ui.Recognition
import timber.log.Timber

class ImageAnalyzer(
    private val context: Context,
    private val maxResultsDisplayed: Int = 3,
    private val recognitionListener: (recognition: MutableList<Recognition>) -> Unit,
) : ImageAnalysis.Analyzer {

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val inputImage = imageProxy.image?.let {
            InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
        }
        // set the minimum confidence required:
        val localModel = LocalModel.Builder().setAssetFilePath(LOCAL_TFLITE_MODEL_NAME).build()

        // Specify the name you assigned in the Firebase console.
        val remoteModel = CustomRemoteModel
            .Builder(FirebaseModelSource.Builder(REMOTE_TFLITE_MODEL_NAME).build())
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
                    .setConfidenceThreshold(0.0f)
                    .setMaxResultCount(maxResultsDisplayed)
                    .build()

                val labeler = ImageLabeling.getClient(options)
                inputImage?.let {
                    processImage(labeler, it, imageProxy)
                }
            }
    }

    private fun processImage(
        labeler: ImageLabeler,
        it: InputImage,
        imageProxy: ImageProxy
    ) {
        val recognitionList = mutableListOf<Recognition>()
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
                recognitionListener(recognitionList)
                // Close the image,this tells CameraX to feed the next image to the analyzer
                imageProxy.close()
            }
            .addOnFailureListener {
                Timber.e(it.message ?: "Some error")
            }
    }
}