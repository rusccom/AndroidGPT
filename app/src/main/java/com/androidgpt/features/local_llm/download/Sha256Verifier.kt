package com.androidgpt.features.local_llm.download

import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class Sha256Verifier @Inject constructor() {
    fun verify(file: File, expectedHex: String?): Boolean {
        if (expectedHex.isNullOrBlank()) return true
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(BUFFER)
            while (true) {
                val n = stream.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        val actual = md.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedHex.trim(), ignoreCase = true)
    }

    private companion object { const val BUFFER = 1 shl 16 }
}
