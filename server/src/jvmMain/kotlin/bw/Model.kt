package bw

import domain.World

fun WorldShared.Companion.from(world: World): WorldShared = WorldShared(
    width = world.getWorldWidth(),
    height = world.getWorldHeight(),
    upTime = world.currentUptime,
    clients = world.clients().associate { client ->
        client.id to GameClientShared(
            id = client.id,
            name = client.name,
            version = client.version,
            position = Pair(client.position.x, client.position.y),
            screenSize = Pair(client.screenSize.width, client.screenSize.height)
        )
    },
    isFrozen = world.isFrozen,
    isWrapping = world.isWrapping,
    bodies = world.currentSimulator.bodies.map { body ->
        BodyShared(
            id = body.id,
            position = Pair(body.position.x, body.position.y),
            velocity = Pair(body.velocity.x, body.velocity.y),
            mass = body.mass,
            radius = body.radius,
            shapeId = body.shapeId
        )
    }
)
