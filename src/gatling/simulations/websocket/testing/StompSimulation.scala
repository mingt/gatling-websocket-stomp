package websocket.testing

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import org.apache.commons.lang3.{RandomStringUtils, RandomUtils}

/**
  * Sample performance test with Gatling and STOMP over Websocket.
  */
class StompSimulation extends Simulation {

  // TODO：目前未有前置更新 token ，所以要测试哪个平台先手动在 postman 请求，再更新到这里。
  // 注意，可能这里测试服和本地、生产服的 token 不可混用了。
  val authToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0ZW5hbnRfaWQiOjEsIm1hY2hpbmVJZCI6IjEyMzQ1IiwidXNlcl9pZCI6MTEwMDQ4NywidXNlcl9uYW1lIjoibGFvc2hpMSIsInNjb3BlIjpbIm9wZW5pZCJdLCJhY3RpdmUiOnRydWUsImV4cCI6MTU5ODY2MTgxNiwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl0sImp0aSI6Ijk5ODE1MjY3LWE2MzQtNDUyYS1iNzJmLWY3NTE3ZjE5Y2UxZiIsImNsaWVudF9pZCI6ImFjbWUiLCJ1c2VybmFtZSI6Imxhb3NoaTEifQ.Zzv3XLpxUVoxv0OaxNYNrrdB8ys51LQRvga70XB04ysU3MNXTdatXSHCVD3CHTip8yuCZ5bEgl4tzlW2IWlb7b6Ex1G6KFAgRwU1wT0oFqBqoBcc-4jljWswIbVRF3ReLLcf9CwU3AWxKqj47BaHf7U5nqMt1OFwU0t38bizDzIpX8SeN4V--3kfKW_x2_a9mGo-ENSCKExQTD1HM9RRUDpPOtvwMFRfEWXUffNogabQAfqNitr0YwGBUFuG_RvtPHtNW4wIK-zZA4jXqu-G2XVCdb9wSUV5BofE0EY_zrRDINa5I4I4EAnQ5fn6aeE7pN8B3RkpX_k9n-p8jZNrpA";

  val httpConfig: HttpProtocolBuilder = http
    // .baseURL("http://localhost:8080")
    .baseURL("http://localhost:8082")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling")
    .wsBaseURL("ws://localhost:8082")
    .header("X-Auth-Token", authToken)

  val serverId: String = RandomUtils.nextInt(100, 1000).toString
  val sessionId: String = RandomStringUtils.randomAlphanumeric(8)
  val transport = "websocket"

  val scenario1: ScenarioBuilder = scenario("WebSocket")
    // .exec(http("Open home page").get("/"))
    // .pause(1)
    .exec(ws("Open websocket")
      // .open(s"/gs-guide-websocket/$serverId/$sessionId/$transport")
      .open(s"/api/wsteaching/websocket")
    )
    .pause(1)
    .exec(ws("Connect via STOMP")
      .sendText("[\"CONNECT\\naccept-version:1.1,1.0\\nheart-beat:10000,10000\\n\\n\\u0000\"]")
      // .check(wsAwait.within(10).until(1).regex(".*CONNECTED.*"))
    )
    .pause(1)
    .exec(ws("Subscribe")
      .sendText("[\"SUBSCRIBE\\nid:sub-0\\ndestination:/topic/greetings\\n\\n\\u0000\"]")
    )
    .pause(1)
    .repeat(10, "i") {
      exec(ws("Send message")
        .sendText("[\"SEND\\ndestination:/app/hello\\ncontent-length:15\\n\\n{\\\"name\\\":\\\"Sepp\\\"}\\u0000\"]")
        // .check(wsAwait.within(10).until(1).regex("MESSAGE\\\\ndestination:\\/topic\\/greetings\\\\ncontent-type:application\\/json;charset=UTF-8\\\\nsubscription:sub-0\\\\nmessage-id:[\\w\\d-]*\\\\ncontent-length:\\d*\\\\n\\\\n\\{\\\\\"content\\\\\":\\\\\"Hello, Sepp!\\\\\"\\}\\\\u0000"))
      ).pause(1)
    }
    .exec(ws("Close WS").close)

  setUp(scenario1.inject(atOnceUsers(1)).protocols(httpConfig))
}
