# markets-obsessed
realtime sentiment analyzer service for finance twitter

- ```sbt assembly```
- run play server using
```
scala -classpath /Users/sgandhi/repos/Markets-Obsessed/target/scala-2.11/Markets-Obsessed.jar -Dplay.crypto.secret="<secret-app-key>" play.core.server.NettyServer
```