package ke.payhero.autodial

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors

class LocalDialServer(
    private val port: Int,
    private val onDial: (String) -> Boolean
) {
    @Volatile
    private var running = false

    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private val workers = Executors.newCachedThreadPool()

    fun start() {
        if (running) return
        serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
        running = true
        DialApiState.running = true

        acceptThread = Thread({
            while (running) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    workers.execute { handle(socket) }
                } catch (_: SocketException) {
                    if (!running) break
                } catch (_: IOException) {
                    if (!running) break
                }
            }
        }, "autodial-http").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        DialApiState.running = false
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
        serverSocket = null
        workers.shutdownNow()
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 10_000
        socket.use { client ->
            val input = BufferedInputStream(client.getInputStream())
            val headerBlock = readHeaders(input) ?: return
            val lines = headerBlock.split("\r\n", "\n")
            val requestLine = lines.firstOrNull().orEmpty()
            val tokens = requestLine.split(" ")
            val method = tokens.getOrNull(0)?.uppercase().orEmpty()
            val path = tokens.getOrNull(1)?.substringBefore("?").orEmpty().ifBlank { "/" }

            val headers = mutableMapOf<String, String>()
            for (line in lines.drop(1)) {
                val separator = line.indexOf(':')
                if (separator > 0) {
                    headers[line.substring(0, separator).trim().lowercase()] =
                        line.substring(separator + 1).trim()
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 8192) ?: 0
            val body = if (contentLength > 0) readBody(input, contentLength) else ""

            val response = route(method, path, body)
            writeResponse(client.getOutputStream(), response)
        }
    }

    private fun route(method: String, path: String, body: String): HttpResponse {
        if (method == "OPTIONS") {
            return HttpResponse(204, "")
        }

        val normalized = path.trimEnd('/').ifBlank { "/" }

        if (method == "GET" && (normalized == "/" || normalized == "/health")) {
            return HttpResponse(
                200,
                JSONObject()
                    .put("success", true)
                    .put("service", "AutoDial")
                    .put("port", port)
                    .put("post", "/dial")
                    .put("example", JSONObject().put("dial", "*344#"))
                    .toString()
            )
        }

        if (method == "POST" && (normalized == "/" || normalized == "/dial")) {
            val dial = extractDial(body)
            if (dial == null) {
                DialApiState.lastMessage = "Invalid JSON"
                return HttpResponse(
                    400,
                    JSONObject()
                        .put("success", false)
                        .put("error", "Expected JSON like {\"dial\":\"*344#\"}")
                        .toString()
                )
            }

            val placed = onDial(dial)
            DialApiState.lastDial = dial
            DialApiState.lastMessage = if (placed) "Dialed" else "Call permission missing"
            return if (placed) {
                HttpResponse(
                    200,
                    JSONObject()
                        .put("success", true)
                        .put("dial", dial)
                        .toString()
                )
            } else {
                HttpResponse(
                    403,
                    JSONObject()
                        .put("success", false)
                        .put("error", "CALL_PHONE permission is not granted")
                        .put("dial", dial)
                        .toString()
                )
            }
        }

        return HttpResponse(
            404,
            JSONObject()
                .put("success", false)
                .put("error", "Use POST /dial")
                .toString()
        )
    }

    private fun extractDial(body: String): String? {
        return try {
            JSONObject(body).optString("dial").trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun readHeaders(input: BufferedInputStream): String? {
        val buffer = ByteArrayOutputStream()
        var previous = -1
        while (true) {
            val next = input.read()
            if (next == -1) return null
            buffer.write(next)
            if (previous == '\r'.code && next == '\n'.code && buffer.size() >= 4) {
                val bytes = buffer.toByteArray()
                val size = bytes.size
                if (bytes[size - 4] == '\r'.code.toByte() &&
                    bytes[size - 3] == '\n'.code.toByte() &&
                    bytes[size - 2] == '\r'.code.toByte() &&
                    bytes[size - 1] == '\n'.code.toByte()
                ) {
                    return String(bytes, Charsets.ISO_8859_1)
                }
            }
            previous = next
            if (buffer.size() > 16_384) return null
        }
    }

    private fun readBody(input: BufferedInputStream, length: Int): String {
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(bytes, offset, length - offset)
            if (read < 0) break
            offset += read
        }
        return String(bytes, 0, offset, Charsets.UTF_8)
    }

    private fun writeResponse(output: OutputStream, response: HttpResponse) {
        val bodyBytes = response.body.toByteArray(Charsets.UTF_8)
        val statusText = when (response.status) {
            200 -> "OK"
            204 -> "No Content"
            400 -> "Bad Request"
            403 -> "Forbidden"
            else -> "Not Found"
        }
        val header = buildString {
            append("HTTP/1.1 ${response.status} $statusText\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        output.write(header.toByteArray(Charsets.ISO_8859_1))
        if (bodyBytes.isNotEmpty()) {
            output.write(bodyBytes)
        }
        output.flush()
    }

    private data class HttpResponse(val status: Int, val body: String)
}
