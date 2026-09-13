package com.handsign.poc.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handsign.poc.data.prefs.AppPreferences
import com.handsign.poc.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun createUser(displayName: String) {
        if (displayName.isBlank()) return
        viewModelScope.launch {
            val avatarSeed = System.currentTimeMillis().toString()
            userRepository.createUser(displayName.trim(), avatarSeed)
            prefs.setOnboardingDone(true)
            _done.value = true
        }
    }
}
