package dev.troyt.imagelabeling.ui.dashboard

import android.content.Intent
import android.net.Uri
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
import dev.troyt.imagelabeling.databinding.FragmentDashboardBinding

private const val TAG = "TFL Classify2" // Name for logging
const val IMAGE_URL_KEY = "abcxyz"

class DashboardFragment : Fragment() {

    private var imageUri: Uri? = null

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val viewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val resultLauncher = activityResultLauncher()

        binding.pickPhotoBtn.setOnClickListener { onPickPhoto(resultLauncher) }

        // Initialising the resultRecyclerView and its linked viewAdaptor
        val viewAdapter = DashboardAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter

        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
        viewModel.recognitionList.observe(viewLifecycleOwner,
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
        outState.putString(IMAGE_URL_KEY, imageUri.toString())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        imageUri = savedInstanceState?.getString(IMAGE_URL_KEY, "null")?.toUri()
        // Load the selected image into a preview
        imageUri?.let { binding.localImageView.setImageURI(it) }

    }

    private fun onPickPhoto(resultLauncher: ActivityResultLauncher<Intent>) {
        // Create intent for picking a photo from the gallery
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        resultLauncher.launch(intent)
    }

    private fun activityResultLauncher(): ActivityResultLauncher<Intent> {
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    // There are no request codes
                    val data: Intent? = result.data
                    imageUri = data?.data
                    imageUri?.let {
                        // Load the image located at photoUri into selectedImage
                        binding.localImageView.setImageURI(it)
                        viewModel.inferImage(requireContext(), it) }
                }
            }
        return resultLauncher
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

