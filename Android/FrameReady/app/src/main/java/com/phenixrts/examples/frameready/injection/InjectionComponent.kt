/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.injection

import com.phenixrts.examples.frameready.ui.MainActivity
import com.phenixrts.examples.frameready.ui.SplashActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [InjectionModule::class])
interface InjectionComponent {
    fun inject(target: SplashActivity)
    fun inject(target: MainActivity)
}
