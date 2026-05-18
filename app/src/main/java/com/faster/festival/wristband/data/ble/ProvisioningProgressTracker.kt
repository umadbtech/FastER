package com.faster.festival.wristband.data.ble

import com.faster.festival.wristband.domain.model.ProvisioningProgress
import com.faster.festival.wristband.domain.model.ProvisioningStep
import com.faster.festival.wristband.domain.model.StepStatus
import com.faster.festival.wristband.domain.model.WristbandError

/**
 * Tiny mutable state-builder used inside the manager's `provision()` flow to
 * assemble [ProvisioningProgress] snapshots step by step.
 */
internal class ProvisioningProgressTracker {
    private val steps: MutableMap<ProvisioningStep, StepStatus> = ProvisioningStep.values()
        .associateWith<ProvisioningStep, StepStatus> { StepStatus.Pending }
        .toMutableMap()
    private var current: ProvisioningStep? = null
    private var error: WristbandError? = null
    private var done = false

    fun start(s: ProvisioningStep) { current = s; steps[s] = StepStatus.Running }
    fun complete(s: ProvisioningStep) { steps[s] = StepStatus.Success }
    fun fail(e: WristbandError) {
        error = e
        current?.let { steps[it] = StepStatus.Failed(e.userMessage) }
        done = true
    }
    fun finish() { done = true }

    fun snapshot() = ProvisioningProgress(
        steps = steps.toMap(),
        currentStep = current,
        terminalError = error,
        finished = done
    )
}
