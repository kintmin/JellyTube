package com.kintmin.domain.step.usecase

import javax.inject.Inject

class CalculateStepCountUseCase @Inject constructor() {

    operator fun invoke(stepSensors: List<Long>): Int {
        if (stepSensors.size < 2) return 0

        var total = 0L
        var previous = stepSensors[0]

        for (index in 1 until stepSensors.size) {
            val current = stepSensors[index]

            if (current >= previous) {
                total += current - previous
            }

            previous = current
        }

        return total.toInt()
    }
}