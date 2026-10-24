package net.simtx.mobile

import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Minimal RTL-TCP client. The server sends a 12-byte RTL0 dongle header,
 * followed by raw unsigned 8-bit I/Q sample pairs.
 */
object RtlTcpClient {
    fun connect(host: String, port: Int): Socket {
        val socket = Socket()
        socket.tcpNoDelay = true
        socket.connect(InetSocketAddress(host, port), 7000)
        socket.soTimeout = 7000
        try {
            val input = BufferedInputStream(socket.getInputStream())
            val header = ByteArray(12)
            var offset = 0
            while (offset < header.size) {
                val count = input.read(header, offset, header.size - offset)
                if (count < 0) throw IllegalStateException("Server closed during RTL-TCP handshake")
                offset += count
            }
            if (String(header, 0, 4, Charsets.US_ASCII) != "RTL0") {
                throw IllegalStateException("Server did not return an RTL0 header")
            }
            socket.soTimeout = 0
            return socket
        } catch (error: Throwable) {
            runCatching { socket.close() }
            throw error
        }
    }

    /** RTL-TCP commands use one command byte followed by a big-endian uint32. */
    fun setFrequency(socket: Socket, frequencyHz: Long) {
        require(frequencyHz in 1L..0xFFFF_FFFFL) { "Frequency must fit in the RTL-TCP uint32 range" }
        val output = DataOutputStream(socket.getOutputStream())
        output.writeByte(0x01) // SET_FREQUENCY
        output.writeInt(frequencyHz.toInt())
        output.flush()
    }
}
