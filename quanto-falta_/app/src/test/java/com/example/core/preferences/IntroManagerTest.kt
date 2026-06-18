package com.phdev.quantofalta.core.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IntroManagerTest {

    @Test
    fun defaultIntroCompletedIsFalse() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val introManager = IntroManager(context)
        
        val isCompleted = introManager.isIntroCompleted.first()
        assertEquals(false, isCompleted)
    }

    @Test
    fun saveIntroCompletedSetsValue() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val introManager = IntroManager(context)
        
        introManager.setIntroCompleted(true)
        val isCompleted = introManager.isIntroCompleted.first()
        assertEquals(true, isCompleted)
    }
}
