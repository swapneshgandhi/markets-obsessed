# markets-obsessed
- realtime sentiment analyzer service for twitter finance.
- Service analyzes sentiment of SPX, VIX, Long dated Treasuries, Dollar Index, Euro Dollar cross rate, Crude Oil, Gold and Emerging Markets. 

- ```sbt assembly```
- run play server using
```
scala -classpath /Users/sgandhi/repos/Markets-Obsessed/target/scala-2.11/Markets-Obsessed.jar -Dplay.crypto.secret="<secret-app-key>" play.core.server.NettyServer
```
