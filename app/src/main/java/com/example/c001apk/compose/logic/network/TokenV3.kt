package com.example.c001apk.compose.logic.network

import java.security.MessageDigest
import java.util.Base64
import org.mindrot.jbcrypt.BCrypt

/**
 * Protocol compatibility from coolapk-desktop (MIT), through TG-Twilight; see THIRD_PARTY_PORTS.md.
 */
object TokenV3 {
    private val lookup =
        Base64.getDecoder()
            .decode(
                "VFRCVU9GUXNRMEVsTFVNa1dERWpRU0VzVXlFbUxETkFVeTFEUEZZdUl5MGlNVU01SXpCVUpGY3RNeWtoTVVRMUpTMURPU1V4TXpoUUxWTXdWMDBzSXloU01UUXNXVEZEUkZFd1EwUlZNU1FrVVMwaktTTXRKRGhZTFZRa1VqRXpLU1FzTTBCVkxUUXdVaTAwTlNNc00yQlhMVk1sSkMxRE5GbE5MQ1EwV1RFekxTUXhRMEVoTEVNbElqRXpLRmN3TXowbExpTkVWekVqUFNZdU5Ea2tMRk13Vml4VEtGa3RRemhYTEZNc1dDNGpMU1l0VTJCVFRUQTBMRkV1TXpoVExqTWhJakZETUZNc0kwQlZMQ1FrVUN3ekpTSXNJekJTTUVNOFVDeEVMU0l0STJCUUxVUW9VeTBqTEZNc0l5VWtNRlF3VmswdFJDeFlNVE1sSkM0ME5TRXdVMFJRTEZNaElpMGtOU013VTJCUUxqTmdVeXdqT0ZFc05DeFRMRU13V0N4VUxTRXRJeVJUTEROQkl5MDBKRlpOTVVNc1VEQkVKRmd0TkMwa0xEUWtXQzFETUZNeFJDa2pMVk0wVml4VE1GZ3hJeTBtTFZNeEpTMHpKRlF4TTJCUUxTUTRXVEJETUZNdUkwVWhUU3d6UEZndFJEQlJNRE5FVVMwak9GWXVKRGtsTVNRMFV6QXpOU013UkRra01UTWtWU3hVTUZRdFJDeFlNVE5GSXl4RUxGWXNJelVoTGlRMFYwMHhOQ3hWTENNMFZ5NHpKRlV4TkN4VExGUTRWeTRqWUZZeEpEVWpMVU13VXpGRFBGSXNNMEVsTERNc1ZEQTBPU011TkNrbUxFUWtWaXhFTVNKTkxUTW9VaXd6TEZjdFUwUlFMaVF3VlRGREpTVXdVeWtqTVVNaEl5MHpJU1VzSTBFak1UTXdVUzFVTkZjeFJERWxMVVF0SlM0a05GSXNJemhaVFRCVFlGRXdReVJZTFNNbElTNDBMRmdzVkRSWkxVUW9XVEJEUUZZdFJDeFdMak5FVkRCRE1Ga3RORFVsTEZRNFZTMVVKRmd3TXpSV0xEUTRVVTB3UXkwbUxVUTRXUzFUTVNNc1V5VWlNVU5BVVN4VUxGWXVJMFJVTGlNc1dERkVMRk14STJCUUxWTTBXUzRrT0Zjd00wUlhMVk1vV0MwaktGRXhMQ00wVml4VE5GQXRSRFJXTFVRbEpDMVRKRkF3TkN4Z1lB",
            )
    private val encoder = Base64.getEncoder()
    private const val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun md5(value: String) =
        MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8)).joinToString(
            "",
        ) {
            "%02x".format(it.toInt() and 255)
        }

    private fun base64(value: String) =
        encoder.withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    fun create(
        device: String,
        appCode: Int = 2604201,
        timestamp: Long = System.currentTimeMillis() / 1000,
    ): String {
        for (offset in 0..15) {
            val time = timestamp + offset
            val index = Math.floorMod(time + appCode, 100).toInt() * 4 + 0x80
            val segment =
                String(
                    Base64.getDecoder().decode(lookup.copyOfRange(index, index + 0x80)),
                    Charsets.UTF_8,
                )
            val plain = "com.coolapk.market&$segment&${md5(device)}&$time&$appCode"
            val password = md5(encoder.encodeToString(plain.toByteArray(Charsets.UTF_8)))
            val source = base64("${time.toString(16)}/${md5(plain)}").take(22)
            val salt =
                source.take(21) + alphabet[Math.floorMod(alphabet.indexOf(source[21]) - 5, 64)]
            if (!Regex("^[./A-Za-z0-9]{22}$").matches(salt)) continue
            val hash = BCrypt.hashpw(password, "\$2a\$04\$$salt").replaceRange(0, 3, "\$2y")
            return "v3${base64(hash)}"
        }
        error("无法生成请求签名，请检查系统时间")
    }
}
