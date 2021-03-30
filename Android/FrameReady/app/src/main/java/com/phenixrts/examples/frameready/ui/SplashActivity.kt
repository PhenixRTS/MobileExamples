/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.phenixrts.examples.frameready.FrameReadyApp
import com.phenixrts.examples.frameready.R
import com.phenixrts.examples.frameready.common.*
import com.phenixrts.examples.frameready.common.enums.ExpressError
import com.phenixrts.examples.frameready.databinding.ActivitySplashBinding
import com.phenixrts.examples.frameready.repository.ChannelExpressRepository
import com.phenixrts.suite.phenixdeeplink.models.DeepLinkStatus
import com.phenixrts.suite.phenixdeeplink.models.PhenixConfiguration
import timber.log.Timber
import javax.inject.Inject

private const val TIMEOUT_DELAY = 10000L

class SplashActivity : EasyPermissionActivity() {

    @Inject lateinit var repository: ChannelExpressRepository
    private lateinit var binding: ActivitySplashBinding

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        launchMain {
            binding.root.showSnackBar(getString(R.string.err_network_problems))
        }
    }

    override fun isAlreadyInitialized() = repository.isChannelExpressInitialized()

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        FrameReadyApp.component.inject(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository.onChannelExpressError.observeConsumable(this, { error ->
            Timber.d("Room express failed")
            showErrorDialog(error)
        })
        super.onCreate(savedInstanceState)
    }

    override fun onDeepLinkQueried(status: DeepLinkStatus, configuration: PhenixConfiguration, deepLink: String) {
        launchMain {
            when (status) {
                DeepLinkStatus.RELOAD -> showErrorDialog(ExpressError.CONFIGURATION_CHANGED_ERROR)
                DeepLinkStatus.READY -> if (arePermissionsGranted()) {
                    showLandingScreen(configuration)
                } else {
                    askForPermissions { granted ->
                        if (granted) {
                            showLandingScreen(configuration)
                        } else {
                            onDeepLinkQueried(status, configuration, deepLink)
                        }
                    }
                }
            }
        }
    }

    private fun showLandingScreen(config: PhenixConfiguration) = launchMain {
        if (config.channels.first().isBlank()) {
            showErrorDialog(ExpressError.DEEP_LINK_ERROR)
            return@launchMain
        }
        Timber.d("Waiting for PCast")
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DELAY)
        repository.setupChannelExpress(config)
        repository.waitForPCast()
        timeoutHandler.removeCallbacks(timeoutRunnable)
        Timber.d("Navigating to Landing Screen")
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}
