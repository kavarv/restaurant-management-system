Place SockJS and STOMP.js client libraries here:
  - sockjs.min.js  (from sockjs-client npm package or Spring Boot WebJars)
  - stomp.min.js   (from @stomp/stompjs npm package)

Alternative: add to pom.xml as WebJars and serve via /webjars/sockjs-client/...
  <dependency>
    <groupId>org.webjars</groupId>
    <artifactId>sockjs-client</artifactId>
    <version>1.5.1</version>
  </dependency>
  <dependency>
    <groupId>org.webjars</groupId>
    <artifactId>stomp-websocket</artifactId>
    <version>2.3.4</version>
  </dependency>
Then update layout.html script src paths accordingly.
