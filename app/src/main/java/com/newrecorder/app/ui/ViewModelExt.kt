package com.newrecorder.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.newrecorder.app.NewRecorderApp
import com.newrecorder.app.di.AppContainer

/** وصول سريع لحاوية الاعتماديات من داخل الـ ViewModels. */
val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NewRecorderApp).container
