package com.phdev.quantofalta.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeFirstScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollHome() = benchmarkRule.measureRepeated(
        packageName = "com.phdev.quantofalta",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()

        // Wait until the lazy column is displayed
        // In compose, we need to ensure the list is semantically scrollable.
        // The list should have a test tag or we find it by scrollable property
        val list = device.wait(Until.findObject(By.scrollable(true)), 5000)
        
        if (list != null) {
            // Set gesture margin to avoid triggering system navigation
            list.setGestureMargin(device.displayWidth / 5)
            // Scroll down and up
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }
    }
}
