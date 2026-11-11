package com.example.androidapp.data.transfer

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A password-protected backup file (ROADMAP N294).
 *
 * The plaintext is the ordinary backup JSON; this is a container around it, so the import path still reads one
 * document and the backup schema does not move because the bytes are encrypted. The key is derived from the
 * **password**, not held in Android Keystore: a Keystore key cannot leave the device, and a backup whose key
 * stayed behind would be unreadable exactly when it is needed — on the replacement phone. PBKDF2-HMAC-SHA256
 * and AES-GCM are both in `javax.crypto`, so the promise of no new dependency holds.
 *
 * The envelope is JSON so a file is self-describing: the KDF, its cost, the salt and the IV travel with the
 * ciphertext. GCM authenticates the ciphertext, so a wrong password and an edited file are the same refusal
 * rather than two ways to produce a bad restore.
 */
@Serializable
data class EncryptedBackupEnvelope(
    val format: String = EncryptedBackup.FORMAT,
    val version: Int = EncryptedBackup.VERSION,
    val kdf: String = EncryptedBackup.KDF,
    val iterations: Int = EncryptedBackup.ITERATIONS,
    /** Base64 of the 16-byte random salt. */
    val salt: String,
    /** Base64 of the 12-byte random GCM nonce. */
    val iv: String,
    /** Base64 of the GCM ciphertext, authentication tag included. */
    val ciphertext: String,
)

object EncryptedBackup {

    const val FORMAT = "workout-encrypted-backup"
    const val VERSION = 1
    const val KDF = "PBKDF2WithHmacSHA256"

    /** A short password is a lock with the key in it; the dialog enforces this too. */
    const val MIN_PASSWORD_LENGTH = 8

    /**
     * OWASP's floor for PBKDF2-HMAC-SHA256; the number travels in the envelope (B410).
     *
     * The table gives 600,000 for SHA-256 and 220,000 for SHA-512 — the cheaper figure is the one
     * this constant used to carry, which is why it is stated with its hash. Raising it is a write
     * change only: a file keeps the count it was written with, and [MIN_ITERATIONS] still opens it.
     */
    const val ITERATIONS = 600_000

    /** Refuse a file that asks for an absurd work factor rather than hanging the import. */
    private const val MIN_ITERATIONS = 10_000
    private const val MAX_ITERATIONS = 1_000_000
    private val ITERATION_BOUNDS = MIN_ITERATIONS..MAX_ITERATIONS

    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    private val json = Json { encodeDefaults = true }

    /**
     * Whether [text] is this envelope rather than a plain backup. A malformed file is not one.
     *
     * The marker is looked for before anything is parsed (B411): a plain import is the large document,
     * and building a `JsonElement` tree of it just to read one key was the expensive half of detecting
     * a file that is not encrypted. Only a file that mentions the marker pays the parse, and the parse
     * is what keeps a note that merely quotes the name from being taken for an envelope.
     */
    fun isEncrypted(text: String): Boolean {
        if (!text.contains(FORMAT)) return false
        return runCatching {
            val root = json.parseToJsonElement(text).jsonObject
            root["format"]?.jsonPrimitive?.contentOrNull == FORMAT
        }.getOrDefault(false)
    }

    /** Encrypts the ordinary backup [plaintext] with [password], returning the envelope JSON. */
    fun encrypt(plaintext: String, password: String): DataResult<String> {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return DataResult.Failure(
                DataError.Invalid("Use a password of at least $MIN_PASSWORD_LENGTH characters."),
            )
        }
        return try {
            val random = SecureRandom()
            val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
            val iv = ByteArray(IV_BYTES).also(random::nextBytes)
            val key = deriveKey(password, salt, ITERATIONS)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            DataResult.Success(
                json.encodeToString(
                    EncryptedBackupEnvelope.serializer(),
                    EncryptedBackupEnvelope(
                        salt = encoder.encodeToString(salt),
                        iv = encoder.encodeToString(iv),
                        ciphertext = encoder.encodeToString(ciphertext),
                    ),
                ),
            )
        } catch (_: GeneralSecurityException) {
            DataResult.Failure(DataError.Invalid("The backup could not be encrypted."))
        } catch (_: SerializationException) {
            DataResult.Failure(DataError.Invalid("The backup could not be encrypted."))
        } catch (_: IllegalArgumentException) {
            DataResult.Failure(DataError.Invalid("The backup could not be encrypted."))
        }
    }

    /**
     * Opens [text] with [password], or says why it could not.
     *
     * No distinction between a wrong password and a damaged ciphertext: GCM cannot tell them apart, and a
     * sentence that guessed would be a lie on one of the two.
     */
    // Each refusal is a different sentence for the lifter; a single exit would flatten them.
    @Suppress("ReturnCount")
    fun decrypt(text: String, password: String): DataResult<String> {
        val envelope = try {
            json.decodeFromString(EncryptedBackupEnvelope.serializer(), text)
        } catch (_: SerializationException) {
            return DataResult.Failure(DataError.Invalid("That is not an encrypted backup file."))
        } catch (_: IllegalArgumentException) {
            return DataResult.Failure(DataError.Invalid("That is not an encrypted backup file."))
        }
        if (envelope.format != FORMAT || envelope.version != VERSION || envelope.kdf != KDF) {
            return DataResult.Failure(DataError.Invalid("That encrypted backup is not one this app can open."))
        }
        if (envelope.iterations !in ITERATION_BOUNDS) {
            return DataResult.Failure(DataError.Invalid("That encrypted backup has an invalid work factor."))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return DataResult.Failure(DataError.Invalid("That password did not open the file."))
        }
        return try {
            val salt = decoder.decode(envelope.salt)
            val iv = decoder.decode(envelope.iv)
            val ciphertext = decoder.decode(envelope.ciphertext)
            if (salt.size != SALT_BYTES || iv.size != IV_BYTES) {
                return DataResult.Failure(DataError.Invalid("That encrypted backup is damaged."))
            }
            val key = deriveKey(password, salt, envelope.iterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            val plaintext = cipher.doFinal(ciphertext)
            DataResult.Success(plaintext.toString(Charsets.UTF_8))
        } catch (_: AEADBadTagException) {
            DataResult.Failure(DataError.Invalid("That password did not open the file."))
        } catch (_: GeneralSecurityException) {
            DataResult.Failure(DataError.Invalid("That encrypted backup could not be opened."))
        } catch (_: IllegalArgumentException) {
            DataResult.Failure(DataError.Invalid("That encrypted backup is damaged."))
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            val secret = SecretKeyFactory.getInstance(KDF).generateSecret(spec)
            SecretKeySpec(secret.encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()
}
