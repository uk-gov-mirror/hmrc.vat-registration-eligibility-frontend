

package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.HeaderNames

class ThresholdNextThirtyDaysISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  //TODO - fix when we determine how to deal with dates for VAT threshold
  s" ${controllers.routes.ThresholdNextThirtyDaysController.onSubmit()}" ignore {
    s"redirect to ${controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad()} with value of true" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/make-more-taxable-sales").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
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
      stubAudits()

      val request = buildClient("/make-more-taxable-sales").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("false"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad().url)
    }
  }
}
