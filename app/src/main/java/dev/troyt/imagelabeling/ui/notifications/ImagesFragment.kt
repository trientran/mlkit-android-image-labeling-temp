package dev.troyt.imagelabeling.ui.notifications

import android.Manifest
import android.content.ClipData
import android.content.Context
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
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.databinding.FragmentNotificationsBinding
import dev.troyt.imagelabeling.ui.RecognitionAdapter
import dev.troyt.imagelabeling.ui.home.MAX_RESULT_DISPLAY
import dev.troyt.imagelabeling.ui.home.Recognition
import dev.troyt.imagelabeling.ui.home.RecognitionListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import java.io.IOException

class ImagesFragment : Fragment() {

    private var photoUri: Uri? = null

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val recogViewModel: RecognitionListViewModel by viewModels()
    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val resultLauncher = activityResultLauncher(requireContext())

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

    // Trigger gallery selection for a photo
    private fun onPickPhoto(resultLauncher: ActivityResultLauncher<Intent>) {
        // Create intent for picking a photo from the gallery
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        resultLauncher.launch(intent)

    }

    private fun activityResultLauncher(context: Context): ActivityResultLauncher<Intent> {
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {

                    val clipData: ClipData? = result.data?.clipData
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            val photoUri: Uri = clipData.getItemAt(i).uri
                            Log.d("trienzzz", photoUri.toString())
                            // Load the image located at photoUri into selectedImage
                            val selectedImage: Bitmap? = loadFromUri(photoUri)
                            selectedImage?.let { predictImage(context, it) }
                        }
                    } else {
                        val data: Intent? = result.data
                        photoUri = data?.data
                        Log.d("trienyyy", photoUri.toString())
                        val selectedImage = loadFromUri(photoUri)
                        if (selectedImage != null) {
                            predictImage(context, selectedImage)
                        }
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

    private fun predictImage(context: Context, selectedImage: Bitmap) {
        val recognitionList = mutableListOf<Recognition>()
        val inputImage: InputImage = InputImage.fromBitmap(selectedImage, 0)

        // set the minimum confidence required:
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

        val labeler = ImageLabeling.getClient(options)
        inputImage.let {
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
                    Log.d("trienoi", recognitionList[0].toString())

                    recogViewModel.addData(recognitionList[0])
                    // Update the recognition result list
                    //todo recogViewModel.updateData
                    /*val nextImageRecognitionResults: Flow<List<Recognition>> = flow {
                        while(true) {
                            emit(recognitionList)
                        }
                    }.flowOn(Dispatchers.IO)*/
                }
                .addOnFailureListener {
                    Log.e("Error", it.localizedMessage ?: "some error")
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}