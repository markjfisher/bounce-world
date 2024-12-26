package domain

enum class StatusEvent(val value: Int) {
    CLIENT_CHANGE(1),
    OBJECT_CHANGE(2),
    FROZEN(4),
    CLIENT_CMD_EVENT(8),

    COLLISION(32)
}
