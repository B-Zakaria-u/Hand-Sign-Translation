package com.handsign.poc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.handsign.poc.data.prefs.AppPreferences
import com.handsign.poc.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import com.handsign.poc.data.repository.UserRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        lifecycleScope.launch {
            if (userRepository.getActiveUser() == null) {
                userRepository.createUser("User", System.currentTimeMillis().toString())
            }
        }

        binding.bottomNav.setupWithNavController(navController)
    }
}
