package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.HeaderNames

class NinoControllerISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  s"${controllers.routes.NinoController.onSubmit()}" should {
    "redirect to Threshold In Twelve Months if the answer is yes" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/have-nino").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdInTwelveMonthsController.onPageLoad().url)
    }
    "redirect to Eligibility Dropout if the answer is no" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/have-nino").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("false"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.EligibilityDropoutController.onPageLoad("nino").url)
    }
  }
}