

package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class ThresholdNextThirtyDaysISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())

  s" ${controllers.routes.ThresholdNextThirtyDaysController.onSubmit()}" should {
    s"redirect to ${controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad()} with value of true" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubSuccessfulTxIdGet()
      stubSuccessfulIncorpDataGet()
      stubSuccessfulCompanyNameGet()
      stubAudits()

      val request = buildClient("/make-more-taxable-sales").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad().url)
    }
    s"redirect to ${controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad()} with value of false" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubSuccessfulTxIdGet()
      stubSuccessfulIncorpDataGet()
      stubSuccessfulCompanyNameGet()
      stubAudits()

      val request = buildClient("/make-more-taxable-sales").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("false"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad().url)
    }
  }
}
