package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class VATRegistrationExceptionISpec extends IntegrationSpecBase with AuthHelper with SessionStub {
  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())
  s"${controllers.routes.VATRegistrationExceptionController.onSubmit()}" should {
    "redirect to dropout if answer is yes" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/registration-exception").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.EligibilityDropoutController.onPageLoad("vatRegistrationException").url)
    }
    s"redirect to ${controllers.routes.TurnoverEstimateController.onPageLoad()} if answer is false" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()

        val request = buildClient("/registration-exception").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            "value" -> Seq("false"))
          )
        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.TurnoverEstimateController.onPageLoad().url)
      }
  }
}
