package net.simtx.mobile

import java.net.URI

data class SimTxConnection(val apiBaseUrl: String, val username: String, val password: String) {
    fun validate(): String? {
        val uri = runCatching { URI(apiBaseUrl.trim()) }.getOrNull() ?: return "Enter a valid API base URL."
        if (uri.scheme != "https") return "Only HTTPS endpoints are allowed."
        if (uri.host.isNullOrBlank()) return "The URL must include a host."
        if (username.isBlank()) return "Enter your SimTX username."
        if (password.isBlank()) return "Enter your password."
        return null
    }
}

enum class RadioSessionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
data class Station(val id: String, val name: String, val callsign: String, val status: String)

/**
 * Deliberately no guessed HTTP paths. Implement real operations only after
 * verifying the official API's authentication and v25 TLS/Protocol Buffers schema.
 */
interface SimTxService {
    suspend fun signIn(connection: SimTxConnection): Result<Unit>
    suspend fun listStations(): Result<List<Station>>
}
