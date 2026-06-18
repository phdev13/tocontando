package com.phdev.quantofalta.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generate() = baselineRule.collect(
        packageName = "com.phdev.quantofalta",
        profileBlock = {
            // 1. Inicialização a frio do app
            pressHome()
            startActivityAndWait()

            // 2. Esperar o carregamento da lista
            // Assuming the list has a test tag or is a scrollable View
            device.wait(Until.hasObject(By.scrollable(true)), 5000)

            // 3. Fazer um scroll simples na tela inicial para carregar componentes LazyColumn
            val list = device.findObject(By.scrollable(true))
            if (list != null) {
                list.setGestureMargin(device.displayWidth / 5)
                list.scroll(Direction.DOWN, 0.5f)
                device.waitForIdle()
                list.scroll(Direction.UP, 0.5f)
                device.waitForIdle()
            }

            // 4. (Opcional) Clicar no botão de adicionar evento para carregar a tela de criação
            val fab = device.findObject(By.res("add_event_fab")) // If has resource id
            if (fab != null) {
                fab.click()
                device.waitForIdle()
                device.pressBack()
                device.waitForIdle()
            }
        }
    )
}
