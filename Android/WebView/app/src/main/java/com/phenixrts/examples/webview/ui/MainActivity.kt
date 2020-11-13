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
import com.phenixrts.examples.webview.ui.viewmodels.WebViewModel
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val viewModel: WebViewModel by lazyViewModel ({ application as WebViewApp }, {
        WebViewModel()
    })

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        web_url.setOnEditorActionListener { _: TextView?, _: Int, _: KeyEvent? ->
            val newUrl = web_url.text.toString()
            loadUrl(newUrl)
            true
        }

        web_load.setOnClickListener {
            loadUrl(web_url.text.toString())
        }

        web_view.settings.javaScriptEnabled = true
        web_view.settings.databaseEnabled = true
        web_view.settings.domStorageEnabled = true
        web_view.settings.cacheMode = WebSettings.LOAD_DEFAULT
        web_view.settings.mediaPlaybackRequiresUserGesture = false
        web_view.settings.useWideViewPort = true
        web_view.settings.builtInZoomControls = true
        web_view.settings.displayZoomControls = false
        web_view.settings.loadWithOverviewMode = true

        web_view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        web_view.webChromeClient = WebChromeClient()
        web_view.webViewClient = object : WebViewClient() {

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
                root_view.showSnackBar(getString(R.string.err_failed_to_join_channel))
                hideLoading()
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.proceed()
                root_view.showSnackBar(getString(R.string.err_failed_to_join_channel))
                hideLoading()
            }
        }

        viewModel.webUrl.observe(this, { url ->
            loadUrl(url)
        })
    }

    private fun showLoading() = launchMain {
        web_load.visibility = View.GONE
        web_loading.visibility = View.VISIBLE
    }

    private fun hideLoading() = launchMain {
        web_load.visibility = View.VISIBLE
        web_loading.visibility = View.GONE
    }

    private fun loadUrl(url: String) {
        Timber.d("Loading URL: $url")
        hideKeyboard()
        web_url.setText("")
        web_url.append(url)
        web_view.loadUrl(url)
    }
}
