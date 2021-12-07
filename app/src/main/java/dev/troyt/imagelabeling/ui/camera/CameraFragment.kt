package dev.troyt.imagelabeling.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.databinding.FragmentCameraBinding
import dev.troyt.imagelabeling.ui.Recognition
import java.util.concurrent.Executors

// Constants
const val MAX_RESULT_DISPLAY = 3 // Maximum number of results displayed
private const val TAG = "TFL Classify" // Name for logging
private const val REQUEST_CODE_PERMISSIONS = 999 // Return code after asking for permission
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA) // permission needed

// Listener for the result of the ImageAnalyzer
typealias RecognitionListener = (recognition: MutableList<Recognition>) -> Unit

class CameraFragment : Fragment() {

    // CameraX variables
    private lateinit var preview: Preview // Preview use case, fast, responsive view of the camera
    private lateinit var imageAnalyzer: ImageAnalysis // Analysis use case, for running ML code
    private lateinit var camera: Camera
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val cameraViewModel: CameraViewModel by viewModels()

    private var _binding: FragmentCameraBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.camera_permission_required),
                Toast.LENGTH_SHORT
            ).show()
            permissionRequestLauncher.launch(REQUIRED_PERMISSIONS)
        }

        // Initialising the resultRecyclerView and its linked viewAdaptor
        val viewAdapter = CameraAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter

        // Disable recycler view animation to reduce flickering, otherwise items can move, fade in
        // and out as the list change
        binding.recyclerView.itemAnimator = null

        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
        cameraViewModel.recognitionList.observe(viewLifecycleOwner,
            {
                viewAdapter.submitList(it)
            }
        )
        return root
    }

    /**
     * Check all permissions are granted - use for Camera permission in this example.
     */
    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }
            if (granted) {
                startCamera()
            } else {
                // Exit the app if permission is not granted
                // Best practice is to explain and offer a chance to re-request but this is out of
                // scope in this sample. More details:
                // https://developer.android.com/training/permissions/usage-notes
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_deny_text),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Start the Camera which involves:
     *
     * 1. Initialising the preview use case
     * 2. Initialising the image analyser use case
     * 3. Attach both to the lifecycle of this activity
     * 4. Pipe the output of the preview object to the PreviewView on the screen
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                // This sets the ideal size for the image to be analyse, CameraX will choose the
                // the most suitable resolution which may not be exactly the same or hold the same
                // aspect ratio
                .setTargetResolution(Size(224, 224))
                // How the Image Analyser should pipe in input, 1. every frame but drop no frame, or
                // 2. go to the latest frame and may drop some frame. The default is 2.
                // STRATEGY_KEEP_ONLY_LATEST. The following line is optional, kept here for clarity
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase: ImageAnalysis ->
                    analysisUseCase.setAnalyzer(
                        cameraExecutor,
                        ImageAnalyzer(requireContext()) { items ->
                            // updating the list of recognised objects
                            cameraViewModel.updateData(items)
                        })
                }

            // Select camera, back is the default. If it is not available, choose front camera
            val cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera - try to bind everything at once and CameraX will find
                // the best combination.
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                // Attach the preview to preview view, aka View Finder
                preview.setSurfaceProvider(binding.cameraView.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private class ImageAnalyzer(
        private val context: Context,
        private val listener: RecognitionListener
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
                        for (i in 0 until MAX_RESULT_DISPLAY) {
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
}