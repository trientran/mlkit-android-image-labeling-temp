package dev.troyt.imagelabeling.ui.images

import android.content.Intent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dev.troyt.imagelabeling.databinding.FragmentImagesBinding
import dev.troyt.imagelabeling.ui.defaultDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ImagesFragment : Fragment() {

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val viewModel: ImagesViewModel by viewModels()
    private var _binding: FragmentImagesBinding? = null
    private lateinit var viewAdapter: ImagesAdapter

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagesBinding.inflate(inflater, container, false)
        val rootView: View = binding.root

        binding.pickPhotoBtn.setOnClickListener { onPickPhoto() }
        binding.clearBtn.setOnClickListener { onClearBtnClick() }

        // Initialising the RecyclerView and its linked Adapter
        viewAdapter = ImagesAdapter(requireContext())
        binding.recyclerView.adapter = viewAdapter

        // initialize an instance of linear layout manager
        val layoutOrientation =
            (binding.recyclerView.layoutManager as LinearLayoutManager).orientation
        val dividerItemDecoration = DividerItemDecoration(requireContext(), layoutOrientation)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)
        Log.d(tag, "trien")
        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.recognitionList.collect {
                    viewAdapter.submitList(it)
                }
            }
        }
        return rootView
    }

    private fun onPickPhoto() {
        // Create intent for picking a photo from the gallery
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        resultLauncher.launch(intent)
    }

    private fun onClearBtnClick() {
        viewModel.clearAllData()
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data?.clipData?.let { clipData ->
                    viewModel.inferImages(requireContext(), clipData)
                        .catch { Log.e(tag, it.localizedMessage ?: "some error") }
                        .onEach { viewModel.addData(it) }
                        .flowOn(defaultDispatcher)
                        .launchIn(lifecycleScope)
                }
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
