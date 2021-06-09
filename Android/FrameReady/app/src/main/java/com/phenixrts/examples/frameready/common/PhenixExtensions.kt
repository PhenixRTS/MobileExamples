/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.common

import com.phenixrts.common.RequestStatus
import com.phenixrts.room.RoomService
import com.phenixrts.examples.frameready.common.enums.StreamStatus
import com.phenixrts.express.*
import com.phenixrts.pcast.UserMediaOptions
import com.phenixrts.pcast.UserMediaStream
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun PCastExpress.waitForOnline() = suspendCoroutine<Unit> { continuation ->
    waitForOnline {
        continuation.resume(Unit)
    }
}

suspend fun PCastExpress.getUserMedia(options: UserMediaOptions): UserMediaStream? = suspendCoroutine { continuation ->
    getUserMedia(options) { status, stream ->
        Timber.d("Collecting media stream from pCast: $status")
        continuation.resume(stream)
    }
}

suspend fun ChannelExpress.publishToChannel(options: PublishToChannelOptions): PublishState = suspendCoroutine { continuation ->
    publishToChannel(options) { requestStatus: RequestStatus?, roomService: RoomService?, publisher: ExpressPublisher? ->
        launchMain {
            Timber.d("Stream published: $requestStatus")
            if (requestStatus == RequestStatus.OK && roomService != null && publisher != null) {
                continuation.resume(PublishState(StreamStatus.CONNECTED, roomService, publisher))
            } else {
                continuation.resume(PublishState(StreamStatus.FAILED))
            }
        }
    }
}

data class PublishState(
    val streamStatus: StreamStatus,
    val roomService: RoomService? = null,
    val publisher: ExpressPublisher? = null
)
