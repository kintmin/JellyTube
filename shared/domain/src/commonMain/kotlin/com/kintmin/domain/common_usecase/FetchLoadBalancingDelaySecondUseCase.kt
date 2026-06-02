package com.kintmin.domain.common_usecase

import kotlin.random.Random

class FetchLoadBalancingDelaySecondUseCase constructor() {

    operator fun invoke(userCode: String, random: Random, maxSecond: Long): Long {
        if (userCode.isEmpty()) {
            return random.nextLong(0, maxSecond)
        } else {
            val hash = fnv1a64(userCode)
            return (hash % maxSecond.toULong()).toLong()
        }
    }

    private fun fnv1a64(input: String): ULong {
        var hash = 0xcbf29ce484222325UL
        val prime = 0x100000001b3UL

        for (byte in input.encodeToByteArray()) {
            hash = hash xor byte.toUByte().toULong()
            hash *= prime
        }

        return hash
    }
}