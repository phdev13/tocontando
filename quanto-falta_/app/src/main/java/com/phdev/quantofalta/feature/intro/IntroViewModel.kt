package com.phdev.quantofalta.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.core.preferences.IntroManager
import kotlinx.coroutines.launch

class IntroViewModel(
    private val introManager: IntroManager
) : ViewModel() {

    fun completeIntro() {
        viewModelScope.launch {
            introManager.setIntroCompleted(true)
        }
    }
}
