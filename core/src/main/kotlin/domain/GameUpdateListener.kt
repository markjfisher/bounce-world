package domain

interface GameUpdateListener {
    suspend fun update(game: Game)
}
