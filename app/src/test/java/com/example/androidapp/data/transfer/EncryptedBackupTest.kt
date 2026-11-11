package com.example.androidapp.data.transfer

import com.example.androidapp.domain.DataResult
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The password-protected backup container (ROADMAP N294).
 *
 * The plaintext is the ordinary backup; the container's only promise is that the password — and nothing
 * else — opens it, that the bytes are authenticated, and that a file this app cannot open says so rather
 * than restoring something partial.
 */
class EncryptedBackupTest {

    private val plaintext = """{"schemaVersion":9,"programs":[]}"""
    private val password = "correct horse battery"

    @Test
    fun aRoundTrip_returnsThePlaintext() {
        val envelope = (EncryptedBackup.encrypt(plaintext, password) as DataResult.Success).data

        assertThat(EncryptedBackup.isEncrypted(envelope)).isTrue()
        assertThat(EncryptedBackup.isEncrypted(plaintext)).isFalse()
        assertThat((EncryptedBackup.decrypt(envelope, password) as DataResult.Success).data)
            .isEqualTo(plaintext)
    }

    @Test
    fun aPlainBackupThatQuotesTheMarker_isNotAnEnvelope() {
        // B411: the cheap scan is what lets an ordinary file skip the parse, and the parse is what
        // keeps a note that merely names the format from being taken for an envelope.
        val quoting = """{"schemaVersion":9,"sessions":[],"notes":["workout-encrypted-backup"]}"""

        assertThat(EncryptedBackup.isEncrypted(quoting)).isFalse()
    }

    @Test
    fun aWrongPassword_isRefused() {
        val envelope = (EncryptedBackup.encrypt(plaintext, password) as DataResult.Success).data

        val opened = EncryptedBackup.decrypt(envelope, "not the password")

        assertThat(opened).isInstanceOf(DataResult.Failure::class.java)
    }

    @Test
    fun aTamperedCiphertext_isRefused() {
        val envelope = (EncryptedBackup.encrypt(plaintext, password) as DataResult.Success).data
        val parsed = Json.decodeFromString(EncryptedBackupEnvelope.serializer(), envelope)
        val bytes = Base64.getDecoder().decode(parsed.ciphertext)
        bytes[0] = (bytes[0].toInt() xor 1).toByte()
        val tampered = Json.encodeToString(
            EncryptedBackupEnvelope.serializer(),
            parsed.copy(ciphertext = Base64.getEncoder().encodeToString(bytes)),
        )

        assertThat(EncryptedBackup.decrypt(tampered, password))
            .isInstanceOf(DataResult.Failure::class.java)
    }

    @Test
    fun aShortPassword_isNotAcceptedForANewFile() {
        assertThat(EncryptedBackup.encrypt(plaintext, "short"))
            .isInstanceOf(DataResult.Failure::class.java)
    }

    @Test
    fun aNewFile_carriesTheOwaspWorkFactor() {
        // B410: the count travels in the envelope, so the file itself says how hard it is to guess.
        val envelope = (EncryptedBackup.encrypt(plaintext, password) as DataResult.Success).data
        val parsed = Json.decodeFromString(EncryptedBackupEnvelope.serializer(), envelope)

        assertThat(parsed.iterations).isAtLeast(600_000)
    }

    @Test
    fun aFileFromTheCheaperWorkFactor_stillOpens() {
        // B410 raised the count; the cheap number was what earlier builds wrote, and a backup whose
        // reader demanded today's count would be unreadable exactly when it is needed.
        val legacy = envelopeAt(plaintext, password, iterations = 210_000)

        assertThat((EncryptedBackup.decrypt(legacy, password) as DataResult.Success).data)
            .isEqualTo(plaintext)
    }

    @Test
    fun anUnknownEnvelopeVersion_isRefused() {
        val envelope = (EncryptedBackup.encrypt(plaintext, password) as DataResult.Success).data
        val parsed = Json.decodeFromString(EncryptedBackupEnvelope.serializer(), envelope)

        assertThat(EncryptedBackup.decrypt(envelope, password)).isInstanceOf(DataResult.Success::class.java)
        assertThat(
            EncryptedBackup.decrypt(
                Json.encodeToString(
                    EncryptedBackupEnvelope.serializer(),
                    parsed.copy(version = 2),
                ),
                password,
            ),
        ).isInstanceOf(DataResult.Failure::class.java)
    }

    /** An envelope as the earlier build wrote one, at whatever count it used then. */
    private fun envelopeAt(plaintext: String, password: String, iterations: Int): String {
        val salt = ByteArray(16) { it.toByte() }
        val iv = ByteArray(12) { (it + 1).toByte() }
        val key = SecretKeySpec(
            SecretKeyFactory.getInstance(EncryptedBackup.KDF).generateSecret(
                PBEKeySpec(password.toCharArray(), salt, iterations, 256),
            ).encoded,
            "AES",
        )
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Json.encodeToString(
            EncryptedBackupEnvelope.serializer(),
            EncryptedBackupEnvelope(
                iterations = iterations,
                salt = Base64.getEncoder().encodeToString(salt),
                iv = Base64.getEncoder().encodeToString(iv),
                ciphertext = Base64.getEncoder().encodeToString(ciphertext),
            ),
        )
    }
}
