package com.polyhistor.micguard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.polyhistor.micguard.data.AppRepository

class MicGuardViewModelFactory(
    private val appRepository: AppRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MicGuardViewModel::class.java)) {
            return MicGuardViewModel(appRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 