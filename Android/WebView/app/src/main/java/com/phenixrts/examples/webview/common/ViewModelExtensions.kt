/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.webview.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.phenixrts.examples.webview.WebViewApp

inline fun <reified T : ViewModel> lazyViewModel(noinline owner: (() -> WebViewApp), noinline creator: (() -> T)? = null) =
    lazy {
        if (creator == null)
            ViewModelProvider(owner()).get(T::class.java)
        else
            ViewModelProvider(owner(), BaseViewModelFactory(creator)).get(T::class.java)
    }

class BaseViewModelFactory<T>(val creator: () -> T) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}
