package org.ghrobotics.lib.commands

import org.ghrobotics.lib.mathematics.units.Time

open class CommandTask(val command: Command, private val onFinish: (CommandTask, Time) -> Unit) {
    private var running = false

    suspend fun start0(startTime: Time) {
        assert(!running) { "You tried to start command task for ${command::class.java.simpleName} when it is already running!" }
        running = true

        command.internalStart(startTime) { stopTime -> onFinish(this@CommandTask, stopTime) }
    }

    suspend fun stop0(stopTime: Time) {
        assert(running) { "You tried to stop a command task that isn't running" }
        running = false
        command.internalStop()
        stop(stopTime)
    }

    protected open fun stop(stopTime: Time) {}
}