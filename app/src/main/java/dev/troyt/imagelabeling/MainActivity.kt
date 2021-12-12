package dev.troyt.imagelabeling

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.linkfirebase.FirebaseModelSource
import dev.troyt.imagelabeling.databinding.ActivityMainBinding
import dev.troyt.imagelabeling.ui.ioDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_images, R.id.navigation_camera)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        lifecycleScope.launch(ioDispatcher) {
            // Specify the name you assigned in the Firebase console.
            val remoteModel = CustomRemoteModel
                .Builder(FirebaseModelSource.Builder("model").build())
                .build()

            val downloadConditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            RemoteModelManager.getInstance().download(remoteModel, downloadConditions)
                .addOnSuccessListener {
                    Timber.d("Model download completed")
                }
        }
    }
}