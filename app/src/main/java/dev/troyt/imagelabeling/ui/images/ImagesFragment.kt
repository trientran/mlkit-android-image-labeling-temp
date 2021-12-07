package dev.troyt.imagelabeling.ui.images

import android.content.ClipData
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dev.troyt.imagelabeling.databinding.FragmentImagesBinding
import dev.troyt.imagelabeling.ui.Recognition
import dev.troyt.imagelabeling.ui.defaultDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ImagesFragment : Fragment() {

    // Contains the recognition result. Since  it is a viewModel, it will survive screen rotations
    private val viewModel: ImagesViewModel by viewModels()
    private var _binding: FragmentImagesBinding? = null
    private var imageCount: Int = 0
    private lateinit var viewAdapter: ImagesAdapter
    private lateinit var clipData: ClipData

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

        lifecycleScope.launch {
            viewModel.uiState.collect {
                //recogViewModel.addData(it)
                val adapter = binding.recyclerView.adapter as ImagesAdapter
                val lastRowIndex = adapter.itemCount
                adapter.submitList(it)
                // adapter.notifyItemInserted(lastRowIndex)
                Log.d("trien1", it.toString())

            }
        }
        /* lifecycleScope.launch {
             // repeatOnLifecycle launches the block in a new coroutine every time the
             // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
             repeatOnLifecycle(Lifecycle.State.STARTED) {
                 // Trigger the flow and start listening for values.
                 // Note that this happens when lifecycle is STARTED and stops
                 // collecting when the lifecycle is STOPPED
                 viewModel.uiState.collect {
                         //recogViewModel.addData(it)
                         Log.d("trien1", it.toString())
                         val adapter = binding.recyclerView.adapter as ImagesAdapter
                         val lastRowIndex = adapter.itemCount
                         adapter.submitList(it)
                         adapter.notifyItemInserted(lastRowIndex)
                     }
             }

             *//*lifecycleScope.launch(Dispatchers.Default) {
                predictImageFlow(clipData).
                recogViewModel.updateData(recognitionList)
                Log.d("trien", recognitionList.toString())
                }
*//*
            *//* for (i in 0 until clipData.itemCount) {
                 val imageUri: Uri = clipData.getItemAt(i).uri
                 val bitmap = imageUri.toScaledBitmap(requireContext(), 224, 224)
                 if (bitmap != null) {
                     //predictImage(imageUri, bitmap)

                 }
             }*//*
        }*/
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
                    Log.d("trien2", imageCount.toString())

                    viewModel.inferImages(requireContext(), clipData)
                        .flowOn(defaultDispatcher)
                        .onEach {
                            //recogViewModel.addData(it)
                            val recognitionList = mutableListOf<Recognition>()
                            recognitionList.add(it)
                            viewModel.updateFlowData(recognitionList)
                            Log.d("trien3", recognitionList.toString())
                        }.onCompletion { Log.d("trien4", "fragment canceled") }
                        .launchIn(lifecycleScope)
                    // Start a coroutine in the lifecycle scope

                }
            }
        }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}