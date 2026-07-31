package com.kintmin.domain.hash

/**
 * 순수 Kotlin SHA-256 구현 (FIPS 180-4).
 *
 * 플랫폼 crypto API(`java.security.MessageDigest` 등)에 의존하지 않으므로
 * 바이트를 흘려보내는 쪽이 어느 모듈이든 도메인 규약대로 동일한 해시를 계산할 수 있다.
 * 파일 중복 판정 키를 만드는 용도이며 보안 목적이 아니다.
 *
 * 스트리밍 사용:
 * ```
 * val hasher = Sha256()
 * while (읽을 게 남았으면) hasher.update(buffer, 0, read)
 * val hex = hasher.digestHex()
 * ```
 */
class Sha256 {

    private val state = INITIAL_STATE.copyOf()
    private val block = ByteArray(BLOCK_SIZE)
    private val schedule = IntArray(64)
    private var blockSize = 0
    private var totalByteCount = 0L
    private var finished = false

    /** [bytes]의 [offset]부터 [length] 바이트를 해시에 이어 붙인다. */
    fun update(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        check(!finished) { "digestHex() 호출 후에는 update()할 수 없습니다." }

        var index = offset
        var remaining = length
        totalByteCount += length.toLong()

        if (blockSize > 0) {
            val fill = minOf(BLOCK_SIZE - blockSize, remaining)
            bytes.copyInto(block, blockSize, index, index + fill)
            blockSize += fill
            index += fill
            remaining -= fill
            if (blockSize == BLOCK_SIZE) {
                processBlock(block, 0)
                blockSize = 0
            }
        }

        while (remaining >= BLOCK_SIZE) {
            processBlock(bytes, index)
            index += BLOCK_SIZE
            remaining -= BLOCK_SIZE
        }

        if (remaining > 0) {
            bytes.copyInto(block, 0, index, index + remaining)
            blockSize = remaining
        }
    }

    /** 패딩을 붙여 해시를 확정하고 소문자 16진수 64자로 반환한다. 한 번만 호출할 수 있다. */
    fun digestHex(): String {
        check(!finished) { "digestHex()는 한 번만 호출할 수 있습니다." }
        finished = true

        val bitCount = totalByteCount * 8

        block[blockSize++] = 0x80.toByte()
        if (blockSize > BLOCK_SIZE - LENGTH_FIELD_SIZE) {
            while (blockSize < BLOCK_SIZE) block[blockSize++] = 0
            processBlock(block, 0)
            blockSize = 0
        }
        while (blockSize < BLOCK_SIZE - LENGTH_FIELD_SIZE) block[blockSize++] = 0
        for (shift in 56 downTo 0 step 8) {
            block[blockSize++] = (bitCount ushr shift).toByte()
        }
        processBlock(block, 0)

        val hex = StringBuilder(64)
        for (word in state) {
            for (shift in 28 downTo 0 step 4) {
                hex.append(HEX_DIGITS[(word ushr shift) and 0xf])
            }
        }
        return hex.toString()
    }

    private fun processBlock(source: ByteArray, offset: Int) {
        for (i in 0 until 16) {
            val at = offset + i * 4
            schedule[i] = ((source[at].toInt() and 0xff) shl 24) or
                ((source[at + 1].toInt() and 0xff) shl 16) or
                ((source[at + 2].toInt() and 0xff) shl 8) or
                (source[at + 3].toInt() and 0xff)
        }
        for (i in 16 until 64) {
            val previous = schedule[i - 15]
            val recent = schedule[i - 2]
            val s0 = rotateRight(previous, 7) xor rotateRight(previous, 18) xor (previous ushr 3)
            val s1 = rotateRight(recent, 17) xor rotateRight(recent, 19) xor (recent ushr 10)
            schedule[i] = schedule[i - 16] + s0 + schedule[i - 7] + s1
        }

        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]
        var e = state[4]
        var f = state[5]
        var g = state[6]
        var h = state[7]

        for (i in 0 until 64) {
            val s1 = rotateRight(e, 6) xor rotateRight(e, 11) xor rotateRight(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + s1 + ch + ROUND_CONSTANTS[i] + schedule[i]
            val s0 = rotateRight(a, 2) xor rotateRight(a, 13) xor rotateRight(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj

            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        state[0] += a
        state[1] += b
        state[2] += c
        state[3] += d
        state[4] += e
        state[5] += f
        state[6] += g
        state[7] += h
    }

    private fun rotateRight(value: Int, bitCount: Int): Int =
        (value ushr bitCount) or (value shl (32 - bitCount))

    private companion object {

        const val BLOCK_SIZE = 64
        const val LENGTH_FIELD_SIZE = 8
        const val HEX_DIGITS = "0123456789abcdef"

        /** 처음 8개 소수 제곱근 소수부 상위 32비트 */
        val INITIAL_STATE: IntArray = longArrayOf(
            0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
            0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
        ).toIntArray()

        /** 처음 64개 소수 세제곱근 소수부 상위 32비트 */
        val ROUND_CONSTANTS: IntArray = longArrayOf(
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
            0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
        ).toIntArray()

        fun LongArray.toIntArray(): IntArray = IntArray(size) { this[it].toInt() }
    }
}
