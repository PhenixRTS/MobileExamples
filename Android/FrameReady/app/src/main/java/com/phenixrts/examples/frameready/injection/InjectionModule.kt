/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.injection

import com.phenixrts.examples.frameready.FrameReadyApp
import com.phenixrts.examples.frameready.repository.ChannelExpressRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InjectionModule(private val context: FrameReadyApp) {

    @Singleton
    @Provides
    fun provideChannelExpressRepository() = ChannelExpressRepository(context)

}
