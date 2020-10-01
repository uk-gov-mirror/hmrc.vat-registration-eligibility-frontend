package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class VATExemptionISpec extends IntegrationSpecBase with AuthHelper with SessionStub {
  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())
  s"${controllers.routes.VATExemptionController.onSubmit()}" should {
    s"redirect the user to ${controllers.routes.RegisteringBusinessController.onPageLoad()} page when answer is true" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/vat-exemption").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true")
        ))
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.RegisteringBusinessController.onPageLoad().url)

    }
    s"redirect the user to ${controllers.routes.RegisteringBusinessController.onPageLoad()} when answer is false" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/vat-exemption").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("false")
        ))
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.RegisteringBusinessController.onPageLoad().url)

    }
  }

}
