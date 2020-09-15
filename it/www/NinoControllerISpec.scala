package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import identifiers.{AgriculturalFlatRateSchemeId, VATExemptionId, ZeroRatedSalesId}
import play.api.libs.json.Format._
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class NinoControllerISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())

  s"${controllers.routes.NinoController.onSubmit()}" should {
    "redirect to Agricultural Flat Rate Scheme if the answer is yes" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/have-nino").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.AgriculturalFlatRateSchemeController.onPageLoad().url)
    }
    "redirect to Eligibility Dropout if the answer is no" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/have-nino").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("false"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.EligibilityDropoutController.onPageLoad("nino").url)
    }
  }
}