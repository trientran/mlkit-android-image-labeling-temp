package dev.troyt.imagelabeling.ui.images

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.databinding.FragmentImagesBinding
import dev.troyt.imagelabeling.ui.Recognition
import dev.troyt.imagelabeling.ui.defaultDispatcher
import dev.troyt.imagelabeling.ui.toScaledBitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class ImagesFragment : Fragment() {

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val recogViewModel: ImagesViewModel by viewModels()
    private var _binding: FragmentImagesBinding? = null
    private var imageCount: Int = 0
    private lateinit var viewAdapter: ImagesAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.pickPhotoBtn.setOnClickListener { onPickPhoto() }

        // Initialising the resultRecyclerView and its linked viewAdaptor
        viewAdapter = ImagesAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter
        // initialize an instance of linear layout manager
        val layoutOrientation =
            (binding.recyclerView.layoutManager as LinearLayoutManager).orientation
        val dividerItemDecoration = DividerItemDecoration(requireContext(), layoutOrientation)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
//        recogViewModel.recognitionList.observe(viewLifecycleOwner, {
//            viewAdapter.submitList(null)
//            viewAdapter.submitList(it)
//            Log.d("trien1", it.toString())
//        }
        //       )
        return root
    }

    // Trigger gallery selection for a photo

    private fun onPickPhoto() {
        // Create intent for picking a photo from the gallery
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        resultLauncher.launch(intent)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data?.clipData?.let { clipData ->
                    imageCount = clipData.itemCount
                    val recognitionList = mutableListOf<Recognition>()
                    predictImageFlow(clipData)
                        .flowOn(defaultDispatcher)
                        .onEach {
                            //recogViewModel.addData(it)
                            recognitionList.add(it)
                            Log.d("trien", recognitionList.toString())
                            val adapter = binding.recyclerView.adapter as ImagesAdapter
                            val lastRowIndex = adapter.itemCount
                            adapter.submitList(recognitionList)
                            adapter.notifyItemInserted(lastRowIndex)
                        }.onCompletion { Log.d("trien", "fragment canceled") }
                        .launchIn(this.lifecycleScope)

                    /*lifecycleScope.launch(Dispatchers.Default) {
                        predictImageFlow(clipData).
                        recogViewModel.updateData(recognitionList)
                        Log.d("trien", recognitionList.toString())
                        }
*/
                    /* for (i in 0 until clipData.itemCount) {
                         val imageUri: Uri = clipData.getItemAt(i).uri
                         val bitmap = imageUri.toScaledBitmap(requireContext(), 224, 224)
                         if (bitmap != null) {
                             //predictImage(imageUri, bitmap)

                         }
                     }*/
                }
            }
        }

    fun predictImageFlow(clipData: ClipData) = callbackFlow {
        println("Trien Current Thread name is ${Thread.currentThread().name}")

        // set the minimum confidence required:
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        for (i in 0 until clipData.itemCount) {
            val selectedImageUri: Uri = clipData.getItemAt(i).uri
            val bitmap = selectedImageUri.toScaledBitmap(requireContext(), 224, 224)
            if (bitmap != null) {
                //predictImage(imageUri, bitmap)
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                var recognition: Recognition

                labeler.process(inputImage)
                    .addOnSuccessListener { results ->
                        recognition = try {
                            Recognition(
                                label = results[0].text + " " + results[0].index,
                                confidence = results[0].confidence,
                                imageUri = selectedImageUri
                            )
                        } catch (e: Exception) {
                            Recognition(
                                label = getString(R.string.no_result),
                                confidence = 0f,
                                imageUri = selectedImageUri
                            )
                        }
                        try {
                            trySend(recognition)
                        } catch (e: Throwable) {
                            Log.e(tag, e.localizedMessage ?: "some error")
                        }
                    }
                    .addOnFailureListener {
                        Log.e(tag, it.localizedMessage ?: "some error")
                    }
            }
        }
        awaitClose { labeler.close() }
    }


    private fun predictImage(selectedImageUri: Uri, bitmap: Bitmap) {

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // set the minimum confidence required:
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        var recognition: Recognition? = null

        labeler.process(inputImage)
            .addOnSuccessListener { results ->
                recognition = try {
                    Recognition(
                        imageUri = selectedImageUri,
                        label = results[0].text + " " + results[0].index,
                        confidence = results[0].confidence
                    )
                } catch (e: Exception) {
                    Recognition(
                        imageUri = selectedImageUri,
                        label = getString(R.string.no_result),
                        confidence = 0f
                    )
                }
            }
            .addOnFailureListener {
                Log.e("Error", it.localizedMessage ?: "some error")
                recognition = Recognition(
                    imageUri = selectedImageUri,
                    label = getString(R.string.no_result),
                    confidence = 0f
                )
            }
        recognition?.let { recogViewModel.addData(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}