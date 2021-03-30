/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.ui.viewmodels

import android.content.Context
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.phenixrts.examples.frameready.common.*
import com.phenixrts.examples.frameready.repository.ChannelExpressRepository
import com.phenixrts.media.video.android.AndroidVideoFrame
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.Exception

private const val CAMERA_VIEW_RELEASE_DELAY = 100L

class FrameReadyViewModel(private val repository: ChannelExpressRepository) : ViewModel() {

    private lateinit var player: SimpleExoPlayer
    private var cameraViewOffset: Pair<Float, Float>? = null

    val onPlayerError = ConsumableLiveData<Unit>()
    val onCameraViewMoved = ConsumableLiveData<Pair<Float, Float>>()
    val onChannelExpressError = repository.onChannelExpressError
    val onChannelState = repository.onChannelState

    fun initPlayer(context: Context, onReady: (isReady: Boolean, player: Player) -> Unit) {
        player = SimpleExoPlayer.Builder(context).build()

        player.playWhenReady = true
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                onReady(state == Player.STATE_READY, player)
            }
        })
    }

    fun playVideo(uri: String) {
        try {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            resume()
        } catch (e: Exception) {
            onPlayerError.postConsumable(Unit)
        }
    }

    fun resume() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        player.release()
    }

    fun publishToChannel(cameraView: ImageView, playerView: PlayerView) = launchMain {
        Timber.d("Publishing to channel")
        repository.publishToChannel { frameNotification, videoFrame ->
            (playerView.videoSurfaceView as TextureView).bitmap?.let { playerBitmap ->
                try {
                    fun cameraBitmap() = videoFrame.bitmap.copy(videoFrame.bitmap.config, videoFrame.bitmap.isMutable)
                    val mixedBitmap = playerBitmap.mixWith(cameraBitmap())
                    launchMain { cameraView.setImageBitmap(cameraBitmap()) }
                    frameNotification.write(
                        AndroidVideoFrame(
                            mixedBitmap,
                            videoFrame.timestampInMicroseconds,
                            videoFrame.durationInMicroseconds
                        )
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to write frame")
                    frameNotification.write(videoFrame)
                }
            } ?: run {
                frameNotification.write(videoFrame)
            }
        }
    }

    fun moveCameraView(event: MotionEvent, cameraView: View) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (cameraView.isTouched(event.x, event.y)) {
                    cameraViewOffset = cameraView.getTouchOffset(event.x, event.y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                cameraViewOffset?.run {
                    onCameraViewMoved.postConsumable(Pair(event.x - first, event.y - second))
                }
            }
            MotionEvent.ACTION_UP -> {
                launchMain {
                    delay(CAMERA_VIEW_RELEASE_DELAY)
                    cameraViewOffset = null
                }
            }
        }
    }

}
