package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.HeaderNames

class VATRegistrationExceptionISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  s"${controllers.routes.VATRegistrationExceptionController.onSubmit()}" should {
    s"redirect to ${controllers.routes.TurnoverEstimateController.onPageLoad()} if answer is yes" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/registration-exception").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.TurnoverEstimateController.onPageLoad.url)
    }
    s"redirect to ${controllers.routes.TurnoverEstimateController.onPageLoad()} if answer is false" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/registration-exception").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("false"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.TurnoverEstimateController.onPageLoad().url)
    }
  }
}
