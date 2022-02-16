package dev.trien.uri.lee.herb.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import dev.trien.uri.lee.herb.databinding.FragmentHomeBinding
import dev.trien.uri.lee.herb.ui.READ_EXTERNAL_STORAGE_PERMISSION
import dev.trien.uri.lee.herb.ui.callbackForPermissionResult
import dev.trien.uri.lee.herb.ui.checkPermission

class HomeFragment : Fragment() {

    private val callbackForPermissionResult = callbackForPermissionResult { pickImage() }

    //private val tag = HomeViewModel::class.simpleName
    private var imageUri: Uri? = null

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.pickImageBtn.setOnClickListener { onPickImage() }

        // Initialising the resultRecyclerView and its linked viewAdaptor
        val viewAdapter = HomeAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter

        // Attach an observer on the LiveData field of recognitionList
        // This will notify the recycler view to update every time when a new list is set on the
        // LiveData field of recognitionList.
        viewModel.recognitionList.observe(viewLifecycleOwner, {
            viewAdapter.submitList(it)
        })
        viewModel.imageUri.observe(viewLifecycleOwner, {
            Glide.with(this).load(it).into(binding.localImageView)
        })
        return root
    }

    private fun onPickImage() {
        if (Build.VERSION.SDK_INT > 28) {
            pickImage()
        } else {
            checkPermissionAndAction()
        }
    }

    private fun checkPermissionAndAction() {
        if (this.checkPermission(READ_EXTERNAL_STORAGE_PERMISSION)) {
            pickImage()
        } else {
            callbackForPermissionResult.launch(READ_EXTERNAL_STORAGE_PERMISSION)
        }
    }

    private fun pickImage() {
        // Create intent for picking a photo from the gallery
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(intent)
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                imageUri = result.data?.data
                imageUri?.let {
                    // Load the image located at photoUri into selectedImage
                    viewModel.updateImageUri(it)
                    viewModel.inferImage(requireContext(), it)
                }
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

