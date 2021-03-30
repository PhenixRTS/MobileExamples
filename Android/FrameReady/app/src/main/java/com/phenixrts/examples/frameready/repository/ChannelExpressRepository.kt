/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.repository

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.phenixrts.common.RequestStatus
import com.phenixrts.environment.android.AndroidContext
import com.phenixrts.express.*
import com.phenixrts.room.RoomService
import com.phenixrts.examples.frameready.common.*
import com.phenixrts.examples.frameready.common.enums.ExpressError
import com.phenixrts.examples.frameready.common.enums.StreamStatus
import com.phenixrts.media.video.android.AndroidVideoFrame
import com.phenixrts.pcast.FrameNotification
import com.phenixrts.pcast.UserMediaStream
import com.phenixrts.pcast.android.AndroidReadVideoFrameCallback
import com.phenixrts.suite.phenixdeeplink.models.PhenixConfiguration
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val REINITIALIZATION_DELAY = 1000L

class ChannelExpressRepository(private val context: Application) {

    private var channelExpress: ChannelExpress? = null
    private var userMediaStream: UserMediaStream? = null
    private var expressPublisher: ExpressPublisher? = null
    private var roomService: RoomService? = null
    private var roomExpress: RoomExpress? = null
    private var currentConfiguration = PhenixConfiguration()

    val onChannelExpressError = ConsumableLiveData<ExpressError>()
    val onChannelState = MutableLiveData<StreamStatus>()

    private fun hasConfigurationChanged(configuration: PhenixConfiguration): Boolean = currentConfiguration != configuration

    private suspend fun initializeChannelExpress() {
        Timber.d("Creating Channel Express: $currentConfiguration")
        AndroidContext.setContext(context)
        var pcastBuilder = PCastExpressFactory.createPCastExpressOptionsBuilder()
            .withMinimumConsoleLogLevel("info")
            .withPCastUri(currentConfiguration.uri)
            .withUnrecoverableErrorCallback { status: RequestStatus?, description: String ->
                launchMain {
                    Timber.e("Unrecoverable error in PhenixSDK. Error status: [$status]. Description: [$description]")
                    onChannelExpressError.postConsumable(ExpressError.UNRECOVERABLE_ERROR)
                }
            }
        var deepLinkValid = false
        when {
            !currentConfiguration.authToken.isNullOrBlank() -> {
                Timber.d("Using authentication token: ${currentConfiguration.authToken}")
                pcastBuilder = pcastBuilder.withAuthenticationToken(currentConfiguration.authToken)
                deepLinkValid = true
            }
            !currentConfiguration.edgeToken.isNullOrBlank() -> {
                Timber.d("Using edge token: ${currentConfiguration.edgeToken}")
                pcastBuilder = pcastBuilder.withAuthenticationToken(currentConfiguration.edgeToken)
                deepLinkValid = true
            }
            !currentConfiguration.backend.isNullOrBlank() -> {
                Timber.d("Using backend Uri: ${currentConfiguration.backend}")
                pcastBuilder = pcastBuilder.withBackendUri(currentConfiguration.backend)
                deepLinkValid = true
            }
        }

        val pcastExpressOptions = pcastBuilder.buildPCastExpressOptions()
        val roomExpressOptions = RoomExpressFactory.createRoomExpressOptionsBuilder()
            .withPCastExpressOptions(pcastExpressOptions)
            .buildRoomExpressOptions()

        val channelExpressOptions = ChannelExpressFactory.createChannelExpressOptionsBuilder()
            .withRoomExpressOptions(roomExpressOptions)
            .buildChannelExpressOptions()

        ChannelExpressFactory.createChannelExpress(channelExpressOptions)?.let { express ->
            channelExpress = express
            roomExpress = express.roomExpress
            val userMedia = express.pCastExpress.getUserMedia(getDefaultUserMediaOptions())
            Timber.d("Media stream collected from pCast")
            userMediaStream = userMedia
        }

        if (userMediaStream == null) {
            onChannelExpressError.postConsumable(ExpressError.UNRECOVERABLE_ERROR)
        } else if (!deepLinkValid) {
            onChannelExpressError.postConsumable(ExpressError.DEEP_LINK_ERROR)
        }
    }

    suspend fun setupChannelExpress(configuration: PhenixConfiguration) {
        if (hasConfigurationChanged(configuration)) {
            Timber.d("Channel Express configuration has changed: $configuration")
            currentConfiguration = configuration
            channelExpress?.dispose()
            channelExpress = null
            Timber.d("Channel Express disposed")
            delay(REINITIALIZATION_DELAY)
            initializeChannelExpress()
        }
    }

    suspend fun waitForPCast(): Unit = suspendCoroutine {
        launchMain {
            Timber.d("Waiting for pCast")
            if (channelExpress == null) {
                initializeChannelExpress()
            }
            channelExpress?.pCastExpress?.waitForOnline()
            it.resume(Unit)
        }
    }

    suspend fun publishToChannel(onFrameReady: (frameNotification: FrameNotification, videoFrame: AndroidVideoFrame) -> Unit) = launchIO {
        if (userMediaStream == null || channelExpress == null) {
            Timber.d("Repository not initialized properly")
            onChannelExpressError.postConsumable(ExpressError.UNRECOVERABLE_ERROR)
        }

        userMediaStream?.mediaStream?.videoTracks?.lastOrNull()?.let { videoTrack ->
            userMediaStream?.setFrameReadyCallback(videoTrack) { frameNotification ->
                frameNotification?.read(object : AndroidReadVideoFrameCallback() {
                    override fun onVideoFrameEvent(videoFrame: AndroidVideoFrame?) {
                        videoFrame?.let { frame ->
                            onFrameReady(frameNotification, frame)
                        }
                    }
                })
            }
        }
        Timber.d("Publishing stream")
        channelExpress?.publishToChannel(
            getPublishToChannelOptions(currentConfiguration, userMediaStream!!))?.let { status ->
            launchMain {
                Timber.d("Stream is published: $status")
                expressPublisher = status.publisher
                roomService = status.roomService
                onChannelState.value = status.streamStatus
            }
        }
    }

    fun isChannelExpressInitialized(): Boolean = channelExpress != null

}
