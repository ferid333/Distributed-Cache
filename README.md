# Distributed Cache

A Java 21 distributed in-memory cache server with HTTP and TCP APIs, configurable eviction, TTL expiration, cache metrics, list operations, and basic cluster routing/replication.

The project is built with Spring Boot and Gradle. It can run as a single local cache node or as multiple coordinated nodes using consistent hashing and a configurable replication factor.

## Features

- In-memory key/value cache backed by `ConcurrentHashMap`
- Configurable maximum capacity
- TTL support for string values
- Background cleanup of expired entries
- LRU and MRU eviction policies
- String and integer key codecs
- String values and list values
- HTTP REST API for cache operations
- TCP command protocol with line-based and RESP-style command support
- Client library for TCP access
- Cache metrics: hits, misses, evictions, expirations, and hit rate
- Cluster membership, health checks, gossip-based topology sharing, routing, and replication
- Docker image support
- Unit tests with JUnit 5 and Mockito
- Checkstyle configuration

## Technologies

- Java 21
- Spring Boot 3.3.4
- Gradle
- Spring Web
- JUnit 5
- Mockito
- Checkstyle
- Docker

## Project Structure

```text
.
|-- build.gradle
|-- Dockerfile
|-- settings.gradle
|-- config/
|   `-- checkstyle/
|-- src/
|   |-- main/
|   |   |-- java/org/cache/
|   |   |   |-- Main.java
|   |   |   |-- client/              # TCP client API and serializers
|   |   |   |-- cluster/             # Cluster membership, gossip, health, hashing
|   |   |   |-- config/              # YAML config loading and validation
|   |   |   |-- core/                # Cache abstractions and local cache implementation
|   |   |   |-- eviction/            # LRU/MRU eviction policies
|   |   |   |-- network/             # HTTP controllers and TCP server/connection code
|   |   |   `-- protocol/            # Command processor, codecs, handlers
|   |   `-- resources/
|   |       |-- config.yml
|   `-- test/
|       |-- java/org/cache/
|       `-- resources/config/
`-- gradle/
```

## Requirements

- JDK 21
- Gradle wrapper included in this repository
- Docker, optional

On Windows, use `gradlew.bat`. On macOS/Linux, use `./gradlew`.

## Build

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

## Run Tests

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

## Run the Application

Default configuration is loaded from `src/main/resources/config.yml`.

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

The default `config.yml` starts:

- HTTP server on port `8080`
- Client TCP server on port `2020`
- Cluster TCP server on port `10001`

## Configuration

Configuration is loaded from a classpath YAML file. The default file name is `config.yml`.

You can choose another config file with the `cache.config` system property when running the built jar:

```bash
java -Dcache.config=config-node-a.yml -jar build/libs/distributed-cache-1.0-SNAPSHOT.jar
```

Example:

```yaml
capacity: 100
defaultTtlMillis: 0
eviction-policy: lru
key-type: string

node:
  id: node-a
  host: localhost
  http-port: 8080
  tcp-port: 2020
  cluster-port: 10001

cluster:
  replication-factor: 1
  nodes:
    - id: node-a
      host: localhost
      http-port: 8080
      tcp-port: 2020
      cluster-port: 10001
    - id: node-b
      host: localhost
      http-port: 8081
      tcp-port: 2021
      cluster-port: 10002
```

### Configuration Options

| Key | Description | Default |
| --- | --- | --- |
| `capacity` | Maximum number of entries stored locally before eviction | `1000` |
| `defaultTtlMillis` | Default TTL in milliseconds. `0` means no expiration | `0` |
| `eviction-policy` | Eviction strategy: `lru` or `mru` | `lru` |
| `key-type` | Key codec: `string` or `int` | `string` |
| `node.id` | Current node id | `node-a` |
| `node.host` | Current node host | `localhost` |
| `node.http-port` | HTTP API port | `8080` |
| `node.tcp-port` | Public TCP cache command port | `2020` |
| `node.cluster-port` | Internal cluster command port | `10001` |
| `cluster.replication-factor` | Number of owners selected for each key | `1` when cluster config exists |
| `cluster.nodes` | List of known cache nodes | none |

If the `cluster` section is omitted, the application runs as a single local node.

## HTTP API

Base path: `/cache`

### Health Check

```bash
curl http://localhost:8080/cache/ping
```

Response:

```json
{"value":"PONG"}
```

You can echo a custom value:

```bash
curl "http://localhost:8080/cache/ping?value=hello"
```

### Put a String Value

```bash
curl -X PUT http://localhost:8080/cache/1 \
  -H "Content-Type: application/json" \
  -d '{"value":"Ferid"}'
```

With TTL:

```bash
curl -X PUT http://localhost:8080/cache/1 \
  -H "Content-Type: application/json" \
  -d '{"value":"Ferid","ttlMillis":60000}'
```

### Get a String Value

```bash
curl http://localhost:8080/cache/1
```

Response:

```json
{"value":"Ferid"}
```

### Delete a Key

```bash
curl -X DELETE http://localhost:8080/cache/1
```

### Clear the Cache

```bash
curl -X DELETE http://localhost:8080/cache
```

### Cache Size

```bash
curl http://localhost:8080/cache/size
```

Response:

```json
{"size":1}
```

### Metrics

```bash
curl http://localhost:8080/cache/metrics
```

Response:

```json
{
  "hits": 10,
  "misses": 2,
  "evictions": 1,
  "expirations": 0,
  "hitRate": 0.8333333333333334
}
```

### List Operations

Append a value to a list:

```bash
curl -X POST http://localhost:8080/cache/10/list \
  -H "Content-Type: application/json" \
  -d '{"value":"write-readme"}'
```

Read a range:

```bash
curl "http://localhost:8080/cache/10/list?from=0&to=10"
```

Response:

```json
{"values":["write-readme"]}
```

## Cluster HTTP API

Base path: `/cluster`

Add a node:

```bash
curl -X POST http://localhost:8080/cluster/nodes \
  -H "Content-Type: application/json" \
  -d '{"id":"node-c","host":"localhost","httpPort":8082,"tcpPort":2022,"clusterPort":10003}'
```

Remove a node:

```bash
curl -X DELETE http://localhost:8080/cluster/nodes/node-c
```

When membership changes, the node broadcasts the updated topology to peers.

## TCP API

The TCP server accepts simple line commands and RESP-style array commands. By default, the public TCP port is `2020`.

Line protocol examples:

```text
PING
PUT 1 Ferid
PUT 2 Ferid 60000
GET 1
DELETE 1
SIZE
METRICS
PUSH 10 write-readme
LRANGE 10 0 10
CLEAR
```

Common responses:

```text
OK
PONG
VALUE Ferid
NOT_FOUND
SIZE 1
METRICS hits=1 misses=0 evictions=0 expirations=0 hitRate=1.0
LIST write-readme
ERROR usage: GET key
```

You can test the line protocol with tools such as `nc`:

```bash
nc localhost 2020
```

Then type commands, one per line.

## Supported TCP Commands

| Command | Description |
| --- | --- |
| `PING [value]` | Returns `PONG` or the provided value |
| `PUT key value [ttlMillis]` | Stores a string value |
| `GET key` | Reads a string value |
| `DELETE key` | Deletes a key |
| `SIZE` | Returns local cache size |
| `CLEAR` | Clears local cache |
| `METRICS` | Returns cache metrics |
| `PUSH key value` | Appends a value to a list |
| `LRANGE key [from] to` | Reads list values in a range |
| `CLUSTER_ADD_NODE id host httpPort tcpPort clusterPort` | Adds a node and gossips topology |
| `CLUSTER_REMOVE_NODE nodeId` | Removes a node and gossips topology |
| `TOPOLOGY_DIGEST` | Internal topology digest command |
| `TOPOLOGY_GET` | Internal topology fetch command |
| `TOPOLOGY_APPLY ...` | Internal topology apply command |

## Clustering

The cluster layer uses consistent hashing to choose owners for a key. Writes are sent to every owner selected by the configured replication factor. Reads try the selected owners and return from the first available owner.

Important cluster behavior:

- `cluster.replication-factor` controls how many nodes own each key.
- Unavailable nodes are skipped by the hash ring.
- Each node has a public TCP port and an internal cluster TCP port.
- The public TCP/HTTP API can forward requests to the correct owner node.
- Internal cluster commands are processed without recursive forwarding.
- Health checks run periodically and mark peers as `HEALTHY`, `SUSPECTED`, or `UNAVAILABLE`.
- Gossip runs periodically and exchanges topology versions/fingerprints between peers.

### Run Two Local Nodes

Open two terminals.

Terminal 1:

```bash
./gradlew bootJar
java -Dcache.config=config-node-a.yml -jar build/libs/distributed-cache-1.0-SNAPSHOT.jar
```

Terminal 2:

```bash
java -Dcache.config=config-node-b.yml -jar build/libs/distributed-cache-1.0-SNAPSHOT.jar
```

Node A:

- HTTP: `8080`
- TCP: `2020`
- Cluster TCP: `10001`

Node B:

- HTTP: `8081`
- TCP: `2021`
- Cluster TCP: `10002`

## Docker

Build the image:

```bash
docker build -t distributed-cache .
```

Run with the default classpath config:

```bash
docker run --rm -p 8080:8080 -p 2020:2020 -p 10001:10001 distributed-cache
```

The Dockerfile uses:

- `eclipse-temurin:21-jdk` for building
- `eclipse-temurin:21-jre` for runtime
- `CACHE_CONFIG=config.yml` as the default config file
- `JAVA_OPTS` for additional JVM options

Example:

```bash
docker run --rm \
  -e CACHE_CONFIG=config-node-a.yml \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  -p 8080:8080 \
  -p 2020:2020 \
  -p 10001:10001 \
  distributed-cache
```

## Java TCP Client Support

The `org.cache.client` package contains a `CacheClient` abstraction and a `TcpCacheClient` implementation. The current `TcpCacheClient` constructors are package-private, so application code outside `org.cache.client` should either add a small factory/builder or use the lower-level `RespCommandClient` directly.

Low-level example:

```java
RespCommandClient client = new RespCommandClient();

client.send("localhost", 2020, List.of("PUT", "1", "Ferid", "60000"));
List<String> value = client.send("localhost", 2020, List.of("GET", "1"));
client.send("localhost", 2020, List.of("DELETE", "1"));
```

## Cache Semantics

- `PUT` stores string values.
- `PUSH` creates or updates list values.
- Calling `GET` on a list key returns a wrong-type error.
- Calling `LRANGE` on a string key returns a wrong-type error.
- `ttlMillis = 0` means the entry does not expire.
- Expired entries are removed on read and by a scheduled cleanup task.
- `SIZE` reports the current local node size.
- `CLEAR` clears the local cache service.

## Development Commands

Run tests:

```bash
./gradlew test
```

Run Checkstyle:

```bash
./gradlew checkstyleMain checkstyleTest
```

Build the boot jar:

```bash
./gradlew bootJar
```

Run the full verification task:

```bash
./gradlew check
```

## License

This project is licensed under the MIT License. See `LICENSE` for details.
