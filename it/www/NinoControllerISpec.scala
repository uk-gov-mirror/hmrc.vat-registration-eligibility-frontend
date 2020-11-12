package www

import java.time.LocalDate

import featureswitch.core.config.{FeatureSwitching, TrafficManagement}
import helpers.{AuthHelper, IntegrationSpecBase, SessionStub, TrafficManagementStub}
import models.{Draft, RegistrationInformation, VatReg}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{CREATED, OK, TOO_MANY_REQUESTS}
import play.mvc.Http.HeaderNames

class NinoControllerISpec extends IntegrationSpecBase with AuthHelper with SessionStub with FeatureSwitching with TrafficManagementStub {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  val testRegId = "testRegId"
  val testInternalId = "testInternalId"
  val testDate = LocalDate.now

  s"${controllers.routes.NinoController.onSubmit()}" should {
    "redirect to VAT Exception if the answer is no" in {
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()

      val request = buildClient("/have-nino").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("false"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VATExceptionKickoutController.onPageLoad().url)
    }
    "redirect to Threshold In Twelve Months if the feature switch is disabled" in {
      disable(TrafficManagement)
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
    "redirect to Threshold In Twelve Months if the answer is yes and TrafficManagement returns allocated" in {
      enable(TrafficManagement)
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()
      stubAllocation(testRegId)(CREATED)

      val request = buildClient("/have-nino").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdInTwelveMonthsController.onPageLoad().url)
    }
    "redirect to VAT Exception if the answer is yes and TrafficManagement returns Quota Reached" in {
      enable(TrafficManagement)
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()
      stubAllocation(testRegId)(TOO_MANY_REQUESTS)

      val request = buildClient("/have-nino").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VATExceptionKickoutController.onPageLoad().url)
    }
    "redirect to Threshold In Twelve Months if the answer is yes, TrafficManagement returns Quota Reached but RegistrationInformation matches" in {
      enable(TrafficManagement)
      stubSuccessfulLogin()
      stubSuccessfulRegIdGet()
      stubAudits()
      stubAllocation(testRegId)(TOO_MANY_REQUESTS)
      stubGetRegistrationInformation(OK, Some(RegistrationInformation(testInternalId, testRegId, Draft, Some(testDate), VatReg)))

      val request = buildClient("/have-nino").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
        .post(Map(
          "value" -> Seq("true"))
        )
      val response = await(request)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdInTwelveMonthsController.onPageLoad().url)
    }
  }
}