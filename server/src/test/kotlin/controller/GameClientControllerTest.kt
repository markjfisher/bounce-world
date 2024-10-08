package controller

import domain.GameClient
import domain.GameClientInfo
import domain.ScreenSize
import domain.World
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@MicronautTest
class GameClientControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var world: World

    @MockBean(World::class)
    fun worldMock(): World = mockk(relaxed = true)

    @Test
    fun `register client should create client and return it`() {
        val gameClientInfo = GameClientInfo(
            name = "Test Client",
            version = 1,
            screenSize = ScreenSize(1920, 1080)
        )
        every { world.addClient(any<GameClient>()) } just Runs

        val request = HttpRequest.POST("/client", "Test Client,1,1920,1080")
        request.contentType(MediaType.TEXT_PLAIN_TYPE)
        val response = client.toBlocking().exchange(request, ByteArray::class.java)

        assertThat(response.status).isEqualTo(HttpStatus.CREATED)
        val body = response.body().first()
        assertThat(body).isEqualTo(0.toByte())

        verify(exactly = 1) { world.addClient(match {
            it.name == gameClientInfo.name && it.screenSize == gameClientInfo.screenSize
        })}
    }
}