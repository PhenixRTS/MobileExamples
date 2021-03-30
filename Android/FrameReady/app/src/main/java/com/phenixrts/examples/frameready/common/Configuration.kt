/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.common

import com.phenixrts.express.*
import com.phenixrts.pcast.*
import com.phenixrts.room.RoomServiceFactory
import com.phenixrts.suite.phenixdeeplink.models.PhenixConfiguration
import timber.log.Timber

fun getPublishToChannelOptions(configuration: PhenixConfiguration, userMediaStream: UserMediaStream
): PublishToChannelOptions {
    val channel = configuration.channels.first()
    val channelOptions = RoomServiceFactory.createChannelOptionsBuilder()
        .withName(channel)
        .withAlias(channel)
        .buildChannelOptions()
    var publishOptionsBuilder = PCastExpressFactory.createPublishOptionsBuilder()
        .withMediaConstraints(getDefaultUserMediaOptions())
        .withUserMedia(userMediaStream)
    publishOptionsBuilder = if (!configuration.publishToken.isNullOrBlank()) {
        Timber.d("Publishing with publish token: ${configuration.publishToken} on $channel")
        publishOptionsBuilder.withStreamToken(configuration.publishToken).withSkipRetryOnUnauthorized()
    } else {
        Timber.d("Publishing with capabilities on: $channel")
        publishOptionsBuilder.withCapabilities(arrayOf("ld", "streaming"))
    }
    return ChannelExpressFactory.createPublishToChannelOptionsBuilder()
        .withChannelOptions(channelOptions)
        .withPublishOptions(publishOptionsBuilder.buildPublishOptions())
        .buildPublishToChannelOptions()
}

fun getDefaultUserMediaOptions(): UserMediaOptions = UserMediaOptions().apply {
    videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(FacingMode.USER))
    videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
    videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(15.0))
    audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] =
        listOf(DeviceConstraint(AudioEchoCancelationMode.ON))
}
