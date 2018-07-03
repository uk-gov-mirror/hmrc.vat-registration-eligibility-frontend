package helpers

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.libs.Crypto
import play.api.libs.ws.WSCookie
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Crypted, PlainText}
import uk.gov.hmrc.http.SessionKeys

trait AuthHelper extends SessionCookieBaker {

  private[helpers] val defaultUser = "/foo/bar"

  val sessionId = "session-ac4ed3e7-dbc3-4150-9574-40771c4285c1"

  private def cookieData(additionalData: Map[String, String], userId: String = defaultUser): Map[String, String] = {
    Map(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.userId -> userId,
      SessionKeys.token -> "token",
      SessionKeys.authProvider -> "GGW",
      SessionKeys.lastRequestTimestamp -> new java.util.Date().getTime.toString
    ) ++ additionalData
  }

  def getSessionCookie(additionalData: Map[String, String] = Map(), userId: String = defaultUser) = {
    cookieValue(cookieData(additionalData, userId))
  }

  def stubSuccessfulLogin(userId: String = defaultUser, withSignIn: Boolean = false) = {
    if( withSignIn ) {
      val continueUrl = "/wibble"
      stubFor(get(urlEqualTo(s"/gg/sign-in?continue=${continueUrl}"))
        .willReturn(aResponse()
          .withStatus(303)
          .withHeader(HeaderNames.SET_COOKIE, getSessionCookie())
          .withHeader(HeaderNames.LOCATION, continueUrl)))
    }

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(ok("""{ "internalId" : "testInternalId" }""")))
  }

  def stubAudits() = {
    stubFor(post(urlMatching("/write/audit"))
      .willReturn(
        aResponse().
          withStatus(204)
      )
    )

    stubFor(post(urlMatching("/write/audit/merged"))
      .willReturn(
        aResponse().
          withStatus(204)
      )
    )
  }

  def stubSuccessfulRegIdGet(): Unit ={
    stubFor(
      get(
        urlMatching("/business-registration/business-tax-registration")
      ).willReturn(ok("""{"registrationID":"testRegId"}""")))
  }

  def stubSuccessfulTxIdGet(regId: String = "testRegId"): Unit ={
    stubFor(
      get(
        urlMatching(s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration")
      ).willReturn(ok("""{"confirmationReferences":{"transaction-id" : "testTxId"}}""")))
  }

  def stubSuccessfulIncorpDataGet(txId: String = "testTxId", date: LocalDate = LocalDate.now().minusYears(2)): Unit = {
    stubFor(
      get(
        urlMatching(s"/incorporation-information/$txId/incorporation-update")
      ).willReturn(ok(s"""{"incorporationDate":"$date"}""")))
  }
  def stubUnsuccessfulIncorpDataGet(txId: String = "testTxId", status: Int): Unit = {
    stubFor(
      get(
        urlMatching(s"/incorporation-information/$txId/incorporation-update")
      ).willReturn(aResponse().withStatus(status).withBody("{}")))
  }
}

trait SessionCookieBaker {
  val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="
  def cookieValue(sessionData: Map[String,String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      PlainText(Crypto.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }

  def getCookieData(cookie: WSCookie): Map[String, String] = {
    getCookieData(cookie.value.get)
  }

  def getCookieData(cookieData: String): Map[String, String] = {

    val decrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).decrypt(Crypted(cookieData)).value
    val result = decrypted.split("&")
      .map(_.split("="))
      .map { case Array(k, v) => (k, URLDecoder.decode(v, StandardCharsets.UTF_8.name()))}
      .toMap

    result
  }
}