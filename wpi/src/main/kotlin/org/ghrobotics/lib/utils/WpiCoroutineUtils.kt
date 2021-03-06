package org.ghrobotics.lib.utils

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.launchFrequency(
        frequency: Int = 50,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
): Job {
    if (frequency <= 0) throw IllegalArgumentException("Frequency cannot be lower then 1!")
    return launch(context, start) {
        loopFrequency(frequency, block)
    }
}

suspend fun CoroutineScope.loopFrequency(
    frequency: Int = 50,
    block: suspend CoroutineScope.() -> Unit
) {

    val notifier = FalconNotifier(frequency)
    notifier.updateAlarm()

    while (isActive) {
        notifier.waitForAlarm()
        block(this)
        notifier.updateAlarm()
    }
}