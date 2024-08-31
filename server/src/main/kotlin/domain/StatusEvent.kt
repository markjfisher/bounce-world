package domain

enum class StatusEvent(val value: Int) {
    CLIENT_CHANGE(1),
    OBJECT_CHANGE(2),
    FROZEN(4),
    WRAPPING_TOGGLE(8),
    SPEED_CHANGE(16),
    COLLISION(32)
}
