/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */
package com.phenixrts.examples.webview.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phenixrts.examples.webview.R
import com.phenixrts.examples.webview.WebViewApp
import com.phenixrts.examples.webview.common.hideKeyboard
import com.phenixrts.examples.webview.common.launchMain
import com.phenixrts.examples.webview.common.lazyViewModel
import com.phenixrts.examples.webview.common.showSnackBar
import com.phenixrts.examples.webview.databinding.ActivityMainBinding
import com.phenixrts.examples.webview.ui.viewmodels.WebViewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WebViewModel by lazyViewModel ({ application as WebViewApp }, {
        WebViewModel()
    })

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webUrl.setOnEditorActionListener { _: TextView?, _: Int, _: KeyEvent? ->
            val newUrl = binding.webUrl.text.toString()
            loadUrl(newUrl)
            true
        }

        binding.webLoad.setOnClickListener {
            loadUrl(binding.webUrl.text.toString())
        }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.databaseEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.builtInZoomControls = true
        binding.webView.settings.displayZoomControls = false
        binding.webView.settings.loadWithOverviewMode = true

        binding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                showLoading()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideLoading()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                binding.root.showSnackBar(getString(R.string.err_failed_to_join_channel))
                hideLoading()
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.proceed()
                binding.root.showSnackBar(getString(R.string.err_failed_to_join_channel))
                hideLoading()
            }
        }

        viewModel.webUrl.observe(this, { url ->
            loadUrl(url)
        })
    }

    private fun showLoading() = launchMain {
        binding.webLoad.visibility = View.GONE
        binding.webLoading.visibility = View.VISIBLE
    }

    private fun hideLoading() = launchMain {
        binding.webLoad.visibility = View.VISIBLE
        binding.webLoading.visibility = View.GONE
    }

    private fun loadUrl(url: String) {
        Timber.d("Loading URL: $url")
        hideKeyboard()
        binding.webUrl.setText("")
        binding.webUrl.append(url)
        binding.webView.loadUrl(url)
    }
}
