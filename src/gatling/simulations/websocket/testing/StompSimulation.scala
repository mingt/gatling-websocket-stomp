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
  val authToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0ZW5hbnRfaWQiOjEsIm1hY2hpbmVJZCI6IjEyMzQ1IiwidXNlcl9pZCI6MTEwMDQ4NywidXNlcl9uYW1lIjoibGFvc2hpMSIsInNjb3BlIjpbIm9wZW5pZCJdLCJhY3RpdmUiOnRydWUsImV4cCI6MTU5OTA1MTgzOSwiZGVwdF9pZCI6bnVsbCwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIiwiUk9MRV9URUFDSEVSIl0sImp0aSI6ImFmZTFlZDY1LTgwMDUtNDYyMy1hY2Y3LWUwYjUxNWQ1YTgxZSIsImNsaWVudF9pZCI6ImFjbWUiLCJ1c2VybmFtZSI6Imxhb3NoaTEifQ.WHGUeOwG9lIJ8twIl_6rbmmpGQU9dunOiIuIqJpu_lookvnLItRJG1Wd-SghnCsCLv9aVLhlxPobY75GmsxJweE4fbTTJaVsQfCdVxFRPB4x3FxL6teQlnr9X-wzARGCk5ytG0ao0tZwnTv-PmWJ_6AJNIterSnaDnJCaqMlTlSaKyAVsdXPtNZoluNyHTgXG2CvkhS4zr8BO7W6sMbnYa0P8GDgId1bk-a76PzkJmMnunOT49QeGXy5fRgihbDdl6-jy2N5YXjQDx65HkR5K5qQWy--9oV2teUOMM3ppnJCPzfukdbRZQQVJPI2uzviIXSc8JN7AOnP6QWGMAWQxw";

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
    .repeat(100, "i") { // 每个用户持续发 100 条信息，间隔 1 秒
      exec(ws("Send message")
        .sendText("[\"SEND\\ndestination:/app/hello\\ncontent-length:15\\n\\n{\\\"name\\\":\\\"Sepp\\\"}\\u0000\"]")
        // .check(wsAwait.within(10).until(1).regex("MESSAGE\\\\ndestination:\\/topic\\/greetings\\\\ncontent-type:application\\/json;charset=UTF-8\\\\nsubscription:sub-0\\\\nmessage-id:[\\w\\d-]*\\\\ncontent-length:\\d*\\\\n\\\\n\\{\\\\\"content\\\\\":\\\\\"Hello, Sepp!\\\\\"\\}\\\\u0000"))
      ).pause(1)
    }
    .pause(60) // 等待一段时间后主动关闭连接。注意有没有主动 heartbeat 心跳的情况，若没，Spring Websocket 默认 15 秒可能就已断开连接
    .exec(ws("Close WS").close)

  setUp(scenario1.inject(

      rampUsers(2000) over (20)
      // rampUsers(5000) over (60)

      , nothingFor(40)
      , atOnceUsers(500)

    ).protocols(httpConfig))
}
