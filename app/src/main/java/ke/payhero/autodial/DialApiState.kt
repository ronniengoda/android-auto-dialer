package ke.payhero.autodial

object DialApiState {
    const val PORT = 8080

    @Volatile
    var running: Boolean = false

    @Volatile
    var lastDial: String? = null

    @Volatile
    var lastMessage: String? = null
}
