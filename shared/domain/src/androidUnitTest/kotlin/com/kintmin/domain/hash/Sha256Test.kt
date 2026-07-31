package com.kintmin.domain.hash

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest
import kotlin.random.Random

class Sha256Test {

    private fun hashOf(bytes: ByteArray): String = Sha256().apply { update(bytes) }.digestHex()

    private fun referenceHashOf(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    // ─── FIPS 180-4 표준 테스트 벡터 ──────────────────────────────────────

    @Test
    fun `빈 입력의 해시가 표준 벡터와 일치한다`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hashOf(ByteArray(0)),
        )
    }

    @Test
    fun `abc의 해시가 표준 벡터와 일치한다`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hashOf("abc".toByteArray()),
        )
    }

    @Test
    fun `두 블록 메시지의 해시가 표준 벡터와 일치한다`() {
        val message = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hashOf(message.toByteArray()),
        )
    }

    @Test
    fun `백만 글자 메시지의 해시가 표준 벡터와 일치한다`() {
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            hashOf(ByteArray(1_000_000) { 'a'.code.toByte() }),
        )
    }

    // ─── 스트리밍 동작 ────────────────────────────────────────────────────

    @Test
    fun `여러 조각으로 나눠 넣어도 한 번에 넣은 것과 같은 해시가 나온다`() {
        val random = Random(seed = 20260730)
        val data = random.nextBytes(300_000)

        val streamed = Sha256()
        var index = 0
        while (index < data.size) {
            // 블록 경계와 어긋나는 임의 크기로 잘라 넣는다
            val chunk = minOf(random.nextInt(1, 5000), data.size - index)
            streamed.update(data, index, chunk)
            index += chunk
        }

        assertEquals(hashOf(data), streamed.digestHex())
    }

    @Test
    fun `블록 경계 전후 길이에서 MessageDigest와 같은 결과를 낸다`() {
        val random = Random(seed = 1)
        val boundaryLengths = listOf(0, 1, 55, 56, 57, 63, 64, 65, 119, 120, 121, 127, 128, 129)

        boundaryLengths.forEach { length ->
            val data = random.nextBytes(length)
            assertEquals("length=$length", referenceHashOf(data), hashOf(data))
        }
    }

    @Test
    fun `대용량 임의 데이터에서 MessageDigest와 같은 결과를 낸다`() {
        val data = Random(seed = 42).nextBytes(5 * 1024 * 1024)
        assertEquals(referenceHashOf(data), hashOf(data))
    }

    // ─── 사용 규약 ────────────────────────────────────────────────────────

    @Test(expected = IllegalStateException::class)
    fun `digestHex는 두 번 호출할 수 없다`() {
        val hasher = Sha256()
        hasher.digestHex()
        hasher.digestHex()
    }

    @Test(expected = IllegalStateException::class)
    fun `digestHex 이후에는 update할 수 없다`() {
        val hasher = Sha256()
        hasher.digestHex()
        hasher.update("more".toByteArray())
    }
}
