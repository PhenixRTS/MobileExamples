/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.ui

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.phenixrts.examples.frameready.BuildConfig
import com.phenixrts.examples.frameready.FrameReadyApp
import com.phenixrts.examples.frameready.R
import com.phenixrts.examples.frameready.common.enums.ExpressError
import com.phenixrts.examples.frameready.common.enums.StreamStatus
import com.phenixrts.examples.frameready.common.lazyViewModel
import com.phenixrts.examples.frameready.common.showErrorDialog
import com.phenixrts.examples.frameready.common.showSnackBar
import com.phenixrts.examples.frameready.databinding.ActivityMainBinding
import com.phenixrts.examples.frameready.repository.ChannelExpressRepository
import com.phenixrts.examples.frameready.ui.viewmodels.FrameReadyViewModel
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject lateinit var repository: ChannelExpressRepository
    private lateinit var binding: ActivityMainBinding

    private val viewModel: FrameReadyViewModel by lazyViewModel({ application as FrameReadyApp }, {
        FrameReadyViewModel(repository)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FrameReadyApp.component.inject(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.videoInput.append(BuildConfig.EXAMPLE_VIDEO_URL)
        binding.videoInput.addTextChangedListener {
            val url = binding.videoInput.text.toString()
            binding.videoOpen.isEnabled = url.isNotBlank()
        }

        binding.videoOpen.setOnClickListener {
            viewModel.playVideo(binding.videoInput.text.toString())
        }

        viewModel.onPlayerError.observeConsumable(this) {
            binding.root.showSnackBar(getString(R.string.err_failed_to_load_video))
        }
        viewModel.onChannelExpressError.observeConsumable(this) { error ->
            Timber.d("Channel Express failed: $error")
            showErrorDialog(error)
        }
        viewModel.onChannelState.observe(this) { status ->
            Timber.d("Stream state changed: $status")
            hideLoading()
            if (status == StreamStatus.FAILED) {
                Timber.d("Stream failed")
                showErrorDialog(ExpressError.STREAM_ERROR)
            }
        }

        showLoading()
        viewModel.initPlayer(this) { isReady, player ->
            binding.playerView.player = player
            binding.playerView.useController = true
            binding.loadingProgressBar.visibility = if(isReady) View.GONE else View.VISIBLE
        }

        viewModel.onCameraViewMoved.observeConsumable(this) { position ->
            binding.cameraViewHolder.x = position.first
            binding.cameraViewHolder.y = position.second
        }

        viewModel.playVideo(binding.videoInput.text.toString())
        viewModel.publishToChannel(binding.cameraView, binding.playerView)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean = with(binding) {
        event?.run { viewModel.moveCameraView(this, cameraViewHolder) }
        return super.dispatchTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.cameraViewHolder.run {
            val marginEnd = resources.getDimension(R.dimen.camera_view_margin_end)
            val marginBottom = resources.getDimension(R.dimen.camera_view_margin_bottom)
            x = binding.root.width - width - marginEnd
            y = binding.root.height - height - marginBottom
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.resume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.release()
    }

    private fun hideLoading() {
        binding.loadingProgressBar.visibility = View.GONE
    }

    private fun showLoading() {
        binding.loadingProgressBar.visibility = View.VISIBLE
    }
}
