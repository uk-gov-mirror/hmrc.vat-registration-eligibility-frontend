package www

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import identifiers.{ThresholdInTwelveMonthsId, ThresholdNextThirtyDaysId, ThresholdPreviousThirtyDaysId, VoluntaryRegistrationId}
import models.ConditionalDateFormElement
import org.jsoup.Jsoup
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.HeaderNames
import play.api.test.Helpers._
import utils.TimeMachine
import play.api.inject.bind
import helpers.FakeTimeMachine

class ThresholdPreviousThirtyDaysISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  val selectionFieldName = "value"
  val dateFieldName = s"${ThresholdPreviousThirtyDaysId}Date"
  val internalId = "testInternalId"
  val pageHeading = "Has Test Company ever expected to go over the VAT-registration threshold in a single 30-day period?"
  val pageHeadingAfter17 = "Has Test Company ever expected to make more than £85,000 in VAT-taxable sales in a single 30-day period?"
  val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
  val localDate = LocalDate.of(2020, 1, 1)

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .overrides(bind[TimeMachine].to[FakeTimeMachine])
    .build()

  s"GET ${controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad().url}" should {
    "render the page" when {
      "no data is present in mongo" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()

        val request = buildClient("/gone-over-threshold-period").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val response = await(request)

        response.status mustBe OK
      }

      "data is present in mongo" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()
        cacheSessionData(internalId, s"$ThresholdPreviousThirtyDaysId", ConditionalDateFormElement(true, Some(LocalDate.of(2017, 12, 1))))

        val request = buildClient("/gone-over-threshold-period").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val response = await(request)

        response.status mustBe OK
      }
    }
  }

  s"POST ${controllers.routes.ThresholdPreviousThirtyDaysController.onSubmit().url}" should {
    val incorpDate = LocalDate.of(2020, 1, 1).minusMonths(14)
    val dateBeforeIncorp = incorpDate.minusMonths(2)
    val dateAfterIncorp = incorpDate.plusMonths(2)

    s"redirect to ${controllers.routes.VATRegistrationExceptionController.onPageLoad().url}" when {
      "yes and a valid date is submitted, and Q1 is yes should also drop voluntary" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(true, Some(localDate)))
        cacheSessionData[Boolean](internalId, VoluntaryRegistrationId.toString, true)

        val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("true"),
            s"$dateFieldName.day" -> Seq(s"${dateAfterIncorp.getDayOfMonth}"),
            s"$dateFieldName.month" -> Seq(s"${dateAfterIncorp.getMonthValue}"),
            s"$dateFieldName.year" -> Seq(s"${dateAfterIncorp.getYear}")
          ))

        val response = await(request)
        response.status mustBe SEE_OTHER
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VATRegistrationExceptionController.onPageLoad().url)
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdPreviousThirtyDaysId.toString, Some(ConditionalDateFormElement(true, Some(dateAfterIncorp))))
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, Some(ConditionalDateFormElement(true, Some(localDate))))
        verifySessionCacheData(internalId, VoluntaryRegistrationId.toString, Option.empty[Boolean])
      }
      "no is submitted and Q1 is yes" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()

        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(true, Some(localDate)))

        val request = buildClient("/gone-over-threshold-period").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("false")
          ))

        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VATRegistrationExceptionController.onPageLoad().url)

        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdPreviousThirtyDaysId.toString, Some(ConditionalDateFormElement(false, None)))
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, Some(ConditionalDateFormElement(true, Some(localDate))))
      }
    }
    s"redirect to ${controllers.routes.VoluntaryRegistrationController.onPageLoad().url}" when {
      "no to all three threshold questions" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdNextThirtyDaysId.toString, ConditionalDateFormElement(false, None))

        val request = buildClient("/gone-over-threshold-period").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("false")
          ))
        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VoluntaryRegistrationController.onPageLoad().url)
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdPreviousThirtyDaysId.toString, Some(ConditionalDateFormElement(false, None)))
      }
    }

    s"redirect to ${controllers.routes.TurnoverEstimateController.onPageLoad().url}" when {
      "no to Q1, and yes to Q2 but no to Q3 which should also clear voluntary flag" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdNextThirtyDaysId.toString, ConditionalDateFormElement(true, None))
        cacheSessionData[Boolean](internalId, VoluntaryRegistrationId.toString, true)

        val request = buildClient("/gone-over-threshold-period").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("false")
          ))
        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.TurnoverEstimateController.onPageLoad().url)
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdPreviousThirtyDaysId.toString, Some(ConditionalDateFormElement(false, None)))
        verifySessionCacheData(internalId, VoluntaryRegistrationId.toString, Option.empty[Boolean])
      }
      "no to Q1, and no to Q2 but yes to Q3 and should also clear voluntary flag" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubAudits()
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdNextThirtyDaysId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData[Boolean](internalId, VoluntaryRegistrationId.toString, true)

        val request = buildClient("/gone-over-threshold-period").withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("true"),
            s"$dateFieldName.day" -> Seq(s"${dateAfterIncorp.getDayOfMonth}"),
            s"$dateFieldName.month" -> Seq(s"${dateAfterIncorp.getMonthValue}"),
            s"$dateFieldName.year" -> Seq(s"${dateAfterIncorp.getYear}")
          ))

        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.TurnoverEstimateController.onPageLoad().url)
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdPreviousThirtyDaysId.toString, Some(ConditionalDateFormElement(true, Some(dateAfterIncorp))))
        verifySessionCacheData(internalId, VoluntaryRegistrationId.toString, Option.empty[Boolean])
      }
    }
  }
}