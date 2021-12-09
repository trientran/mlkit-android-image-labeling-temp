package dev.troyt.imagelabeling.ui.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dev.troyt.imagelabeling.databinding.FragmentCameraBinding
import dev.troyt.imagelabeling.ui.CAMERA_PERMISSION
import dev.troyt.imagelabeling.ui.callbackForPermissionResult
import dev.troyt.imagelabeling.ui.checkPermission
import timber.log.Timber
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private val callbackForPermissionResult = callbackForPermissionResult { startCamera() }

    // CameraX variables
    private lateinit var preview: Preview // Preview use case, fast, responsive view of the camera
    private lateinit var imageAnalyzer: ImageAnalysis // Analysis use case, for running ML code
    private lateinit var camera: Camera

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


        // Initialising the resultRecyclerView and its linked viewAdaptor
        val viewAdapter = CameraAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter

        // Disable recycler view animation to reduce flickering, otherwise items can move, fade in
        // and out as the list change
        binding.recyclerView.itemAnimator = null

        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
        cameraViewModel.recognitionList.observe(viewLifecycleOwner, {
            viewAdapter.submitList(it)
        })

        // Request camera permissions
        if (this.checkPermission(CAMERA_PERMISSION)) {
            startCamera()
        } else {
            callbackForPermissionResult.launch(CAMERA_PERMISSION)
        }

        return root
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
            // Select camera, back is the default. If it is not available, choose front camera
            val cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            preview = Preview.Builder().build()
            imageAnalyzer =
                cameraViewModel.analyzeImage(requireContext(), Executors.newSingleThreadExecutor())

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera - try to bind everything at once and CameraX will find
                // the best combination.
                camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

                // Attach the preview to preview view, aka View Finder
                preview.setSurfaceProvider(binding.cameraView.surfaceProvider)
            } catch (e: Exception) {
                Timber.e(e.message ?: "Some error")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}