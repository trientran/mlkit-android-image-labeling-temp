package dev.troyt.imagelabeling.ui.notifications

import android.content.Intent
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.troyt.imagelabeling.R
import dev.troyt.imagelabeling.databinding.FragmentNotificationsBinding
import dev.troyt.imagelabeling.ui.RecognitionAdapter
import dev.troyt.imagelabeling.ui.home.Recognition
import dev.troyt.imagelabeling.ui.home.RecognitionViewModel
import java.io.IOException


class ImagesFragment : Fragment() {

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val recogViewModel: RecognitionViewModel by viewModels()
    private var _binding: FragmentNotificationsBinding? = null
    private var imageCount: Int = 0

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

        binding.pickPhotoBtn.setOnClickListener { onPickPhoto() }

        // Initialising the resultRecyclerView and its linked viewAdaptor
        val viewAdapter = RecognitionAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter
        // initialize an instance of linear layout manager
        val layoutOrientation =
            (binding.recyclerView.layoutManager as LinearLayoutManager).orientation
        val dividerItemDecoration = DividerItemDecoration(requireContext(), layoutOrientation)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
        recogViewModel.recognitionList.observe(viewLifecycleOwner, {
            viewAdapter.submitList(null)
            viewAdapter.submitList(it)
            Log.d("trien1", it.toString())
        }
        )
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
                result.data?.clipData?.let {
                    imageCount = it.itemCount
                    for (i in 0 until it.itemCount) {
                        val imageUri: Uri = it.getItemAt(i).uri
                        predictImage(imageUri)
                    }
                }
            }
        }

    private fun predictImage(selectedImageUri: Uri) {
        val recognitionList = mutableListOf<Recognition>()
        val inputImage: InputImage? =
            try {
                InputImage.fromFilePath(requireContext(), selectedImageUri)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }

        // set the minimum confidence required:
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        inputImage?.let {
            labeler.process(it)
                .addOnSuccessListener { results ->
                    try {
                        recognitionList.add(
                            Recognition(
                                imageUri = selectedImageUri,
                                label = results[0].text + " " + results[0].index,
                                confidence = results[0].confidence
                            )
                        )
                    } catch (e: Exception) {
                        recognitionList.add(
                            Recognition(
                                imageUri = selectedImageUri,
                                label = getString(R.string.no_result),
                                confidence = 0f
                            )
                        )
                    }
                    recogViewModel.addData(recognitionList[0])
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