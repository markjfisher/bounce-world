package domain

enum class ClientCommand(val event: Int, val cmd: String) {
    ENABLE_DARK_MODE(1, "enableDarkMode"),
    DISABLE_DARK_MODE(2, "disableDarkMode"),
    ENABLE_WHO(3, "enableWho"),
    DISABLE_WHO(4, "disableWho"),
    ENABLE_BROADCAST(5, "enableBroadcast"),
    DISABLE_BROADCAST(6, "disableBroadcast"),
    ENABLE_INFO(7, "enableInfo"),
    DISABLE_INFO(8, "disableInfo"),
    ;

    companion object {
        fun from(cmd: String): ClientCommand? {
            return entries.firstOrNull { it.cmd == cmd }
        }
    }
}