/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.webview.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.phenixrts.examples.webview.BuildConfig
import com.phenixrts.examples.webview.R
import com.phenixrts.examples.webview.WebViewApp
import com.phenixrts.examples.webview.common.launchMain
import com.phenixrts.examples.webview.common.lazyViewModel
import com.phenixrts.examples.webview.ui.viewmodels.WebViewModel
import timber.log.Timber

private const val QUERY_URL = "url"

class SplashActivity : AppCompatActivity() {

    private val viewModel: WebViewModel by lazyViewModel ({ application as WebViewApp }, {
        WebViewModel()
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        checkDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkDeepLink(intent)
    }

    private fun checkDeepLink(intent: Intent?) {
        Timber.d("Checking deep link: ${intent?.data}")
        intent?.data?.getQueryParameter(QUERY_URL)?.let { url ->
            intent.data?.fragment?.let { channel ->
                showLandingScreen("$url#$channel")
                return
            }
        }
        showLandingScreen(BuildConfig.DEMO_URL)
    }

    private fun showLandingScreen(url: String) = launchMain {
        Timber.d("Navigating to Landing Screen")
        viewModel.webUrl.value = url
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}
