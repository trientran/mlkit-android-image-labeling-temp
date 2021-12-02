package dev.troyt.imagelabeling.ui.dashboard

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.databinding.FragmentDashboardBinding
import dev.troyt.imagelabeling.ui.RecognitionAdapter
import dev.troyt.imagelabeling.ui.home.MAX_RESULT_DISPLAY
import dev.troyt.imagelabeling.ui.home.Recognition
import dev.troyt.imagelabeling.ui.home.RecognitionListViewModel
import java.io.IOException

private const val TAG = "TFL Classify2" // Name for logging
const val IMAGE_URL_KEY = "abcxyz"

class DashboardFragment : Fragment() {

    private var photoUri: Uri? = null

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val recogViewModel: RecognitionListViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val resultLauncher = activityResultLauncher()

        binding.loadImageBtn.setOnClickListener { onPickPhoto(resultLauncher) }

        // Initialising the resultRecyclerView and its linked viewAdaptor
        val viewAdapter = RecognitionAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter

        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
        recogViewModel.recognitionList.observe(viewLifecycleOwner,
            {
                Log.d("trien1", it.toString())
                viewAdapter.submitList(it)
            }
        )
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // Here You have to save count value
        Log.i("MyTag", "onSaveInstanceState")
        outState.putString(IMAGE_URL_KEY, photoUri.toString())
    }

    //todo
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        photoUri = savedInstanceState?.getString(IMAGE_URL_KEY, "null")?.toUri()
        // Load the image located at photoUri into selectedImage

        // Load the selected image into a preview
        if (photoUri.toString() != "null") {
            val selectedImage = loadFromUri(photoUri)
            binding.localImageView.setImageBitmap(selectedImage)
        }
    }

    // Trigger gallery selection for a photo
    private fun onPickPhoto(resultLauncher: ActivityResultLauncher<Intent>) {
        // Create intent for picking a photo from the gallery
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

//        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
//        // So as long as the result is not null, it's safe to use the intent.
//        if (intent.resolveActivity(packageManager) != null) {
//            // Bring up gallery to select a photo
//            startActivityForResult(intent, PICK_PHOTO_CODE)
//        }

        resultLauncher.launch(intent)

    }

    private fun activityResultLauncher(): ActivityResultLauncher<Intent> {
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    // There are no request codes
                    val data: Intent? = result.data
                    photoUri = data?.data

                    // Load the image located at photoUri into selectedImage
                    val selectedImage = loadFromUri(photoUri)

                    // Load the selected image into a preview
                    binding.localImageView.setImageBitmap(selectedImage)

                    if (selectedImage != null) {
                        predictImage(selectedImage)
                    }
                }
            }
        return resultLauncher
    }

    private fun loadFromUri(photoUri: Uri?): Bitmap? {
        var image: Bitmap? = null
        try {
            // check version of Android on device
            image = if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(requireContext().contentResolver, photoUri!!)
                ImageDecoder.decodeBitmap(source)
            } else {
                // support older versions of Android by using getBitmap
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return image
    }

    private fun predictImage(selectedImage: Bitmap) {

        val items = mutableListOf<Recognition>()

        // TODO 2: Resize and Convert Image to Bitmap then to TensorImage
        //      val resizedBitmap: Bitmap = BitmapScaler.scaleToFitWidth(rawTakenImage, SOME_WIDTH)
        /*Bitmap.createScaledBitmap(
            selectedImage,
            224,
            224,
            false
        )*/
        val inputImage: InputImage = InputImage.fromBitmap(selectedImage, 0)

        // set the minimum confidence required:
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
        val labeler = ImageLabeling.getClient(options)

        labeler.process(inputImage)
            .addOnSuccessListener { mutableList ->
                val maxResultDisplayed = when {
                    mutableList.size >= MAX_RESULT_DISPLAY -> MAX_RESULT_DISPLAY
                    else -> mutableList.size
                }
                for (i in 0 until maxResultDisplayed) {
                    items.add(
                        Recognition(
                            label = mutableList[i].text + " " + mutableList[i].index,
                            confidence = mutableList[i].confidence
                        )
                    )
                }
                // Return the result
                Log.v("trien3", items.toList().toString())
                recogViewModel.updateData(items)
            }
            .addOnFailureListener {
                Log.e("Error", it.localizedMessage ?: "some error")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}