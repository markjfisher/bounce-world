# bounce-world

The bounce world coordinator service.

## releases

Releases can be found at https://github.com/markjfisher/bounce-world/releases

## running

To run the service with default parameters (listening on all bindings on port 8080, in 'grid' configuration for clients), download the jar, then run it with

```shell
# substitute the correct version here
$ java -jar path/to/server-1.0.8-all.jar
```

To run with 'table' settings, so all clients add to the right, with port 8081, use:

```shell
$ MICRONAUT_SERVER_PORT=8081 WORLD_LOCATION_PATTERN=right java -jar /path/to/server-1.0.8-all.jar
```

There are many parameters that can be changed by setting them on the command line before the java command.
These can all be found in [the application config file](server/src/main/resources/application.toml)

Here are the useful values with their defaults.

- `WORLD_HEARTBEAT_TIMEOUT_MILLIS=60000`
This is the timeout before a client that hasn't asked for any data is removed from the server, and their
slot is freed so the next connection will take that same slot.

- `WORLD_INITIAL_SPEED=11`
This is the initial magnitude of velocity that bodies start with in the world when they are spawned.

- `WORLD_UPDATES_PER_SECOND=10`

This is the frame rate of the server for bodies locations and collisions being calculated. It does
not directly affect the clients, it just gives more accuracy to all the bodies over time.

- `WORLD_LOCATION_PATTERN=grid`

This is either `grid` or `right` and controls where the next client will be add in the world view.
`right` is useful for demos at shows where all the screens are laid out on a table going to the right. 

- `WORLD_ENABLE_WRAPPING=false`

Changes simulator to use a wrapped world rather than bouncing off the edges of the world, so bodies
will wrap up and down, left and right instead of bouncing within the boundary. With wrapping it's like
there are no walls around the edges to keep the bodies in.

- `MICRONAUT_SERVER_HOST=0.0.0.0`

By default, the server will listen on all bindings on the local network adapter. Change this to a particular IP
address if you have several bindings and only want to listen on one of them, e.g. 127.0.0.1 for only on
the local machine with no one outside being able to connect. This is useful for connecting emulators only on
the local machine.

## CLI interaction

The world service has a REST interface that supports many commands to interact with the application.

### creating a client for testing

This is only useful for testing, as the client will time out after 60s.

- POST `/client`, body: `name,version,x-width,y-width`

```shell
$ curl -s -X POST http://oscar.home:8080/client -d 'fen2,1,40,22' -o - | xxd -p
00
```
which shows the hexadecimal value of the client id.

### simple list of clients with ids

- GET `/get-clients`

returns a simple array of the clients ids and names as a json object:

```
❯ curl -s -X GET http://oscar.home:8080/get-clients
[{"id":0,"name":"fen3"},{"id":1,"name":"fen2"},{"id":2,"name":"fen1"}]
```

### reordering clients

- GET `/reorder/id1,id2,id3,...`


## building

Use the gradle wrapper script to download gradle and build the libraries
```shell
$ ./gradlew
```

The built library can be found at `server/build/libs/server-<VERSION>-all.jar`