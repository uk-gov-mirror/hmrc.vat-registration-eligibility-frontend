package www

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import identifiers.{ThresholdInTwelveMonthsId, ThresholdNextThirtyDaysId, ThresholdPreviousThirtyDaysId, VoluntaryRegistrationId}
import models.{ConditionalDateFormElement, CurrentProfile}
import org.jsoup.Jsoup
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class ThresholdPreviousThirtyDaysISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  val selectionFieldName  = s"${ThresholdPreviousThirtyDaysId}Selection"
  val dateFieldName       = s"${ThresholdPreviousThirtyDaysId}Date"
  val internalId          = "testInternalId"
  val pageHeading         = "Has Test Company ever expected to go over the VAT-registration threshold in a single 30-day period?"
  val pageHeadingAfter17  = "Has Test Company ever expected to make more than £85,000 in VAT-taxable sales in a single 30-day period?"
  val dateTimeFormatter   = DateTimeFormatter.ofPattern("dd MMMM yyyy")
  val localDate = LocalDate.of(2017,1,1)

  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())

  s"GET ${controllers.routes.ThresholdPreviousThirtyDaysController.onPageLoad().url}" should {
    "render the page" when {
      "no data is present in mongo" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val response = await(request)
        response.status mustBe 200
        val document = Jsoup.parse(response.body)
        document.getElementById("main-heading").text() mustBe pageHeading
        document.getElementById(s"$selectionFieldName-true").attr("checked") mustBe ""
        document.getElementById(s"$selectionFieldName-false").attr("checked") mustBe ""
      }

      "data (true, Some(2017-12-1)) is present in mongo" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        cacheSessionData(internalId, s"$ThresholdPreviousThirtyDaysId", ConditionalDateFormElement(true, Some(LocalDate.of(2017,12,1))))

        val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val response = await(request)
        response.status mustBe 200
        val document = Jsoup.parse(response.body)
        document.getElementById("main-heading").text() mustBe pageHeading
        document.getElementById(s"$selectionFieldName-true").attr("checked") mustBe "checked"
        document.getElementById(s"$selectionFieldName-false").attr("checked") mustBe ""
        document.getElementById(s"$dateFieldName.day").`val` mustBe "1"
        document.getElementById(s"$dateFieldName.month").`val` mustBe "12"
        document.getElementById(s"$dateFieldName.year").`val` mustBe "2017"
      }
    }
    "throw an exception" when {
      "when no incorp date is present" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubUnsuccessfulIncorpDataGet(status = 204)
        stubSuccessfulCompanyNameGet()
        stubAudits()
        val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val response = await(request)
        response.status mustBe 500
      }
    }
  }

  s"POST ${controllers.routes.ThresholdPreviousThirtyDaysController.onSubmit().url}" should {
    val incorpDate = LocalDate.of(2018,10,1).minusMonths(14)
    val dateBeforeIncorp = incorpDate.minusMonths(2)
    val dateAfterIncorp = incorpDate.plusMonths(2)

    "return a badrequest with form errors" when {
      "a date before the incorp date is passed in" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet(date = incorpDate)
        stubSuccessfulCompanyNameGet()
        stubAudits()


        val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("true"),
            s"$dateFieldName.day" -> Seq(s"${dateBeforeIncorp.getDayOfMonth}"),
            s"$dateFieldName.month" -> Seq(s"${dateBeforeIncorp.getMonthValue}"),
            s"$dateFieldName.year" -> Seq(s"${dateBeforeIncorp.getYear}")
          ))

        val response = await(request)
        response.status mustBe 400
        val document = Jsoup.parse(response.body)

        document.getElementById("main-heading").text() mustBe pageHeadingAfter17
        document.getElementById("error-summary-heading").text() mustBe "This page has errors"
        document.getElementsByAttributeValue("href",s"#$dateFieldName").text() mustBe s"Enter a date that's after the date the business was set up: ${incorpDate.format(dateTimeFormatter)}"
      }
    }
    s"redirect to ${controllers.routes.VATRegistrationExceptionController.onPageLoad().url}" when {
      "yes and a valid date is submitted, and Q1 is yes should also drop voluntary" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet(date = incorpDate)
        stubSuccessfulCompanyNameGet()
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
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VATRegistrationExceptionController.onPageLoad().url)
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdPreviousThirtyDaysId.toString, Some(ConditionalDateFormElement(true, Some(dateAfterIncorp))))
        verifySessionCacheData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, Some(ConditionalDateFormElement(true, Some(localDate))))
        verifySessionCacheData(internalId, VoluntaryRegistrationId.toString, Option.empty[Boolean])
      }
      "no is submitted and Q1 is yes" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet(date = incorpDate)
        stubSuccessfulCompanyNameGet()
        stubAudits()

        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(true, Some(localDate)))

        val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
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
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet(date = incorpDate)
        stubSuccessfulCompanyNameGet()
        stubAudits()
        cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData(internalId, ThresholdNextThirtyDaysId.toString, false)

        val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
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
      stubSuccessfulTxIdGet()
      stubSuccessfulIncorpDataGet(date = incorpDate)
      stubSuccessfulCompanyNameGet()
      stubAudits()
      cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(false, None))
      cacheSessionData(internalId, ThresholdNextThirtyDaysId.toString,true)
      cacheSessionData[Boolean](internalId, VoluntaryRegistrationId.toString, true)

      val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
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
      stubSuccessfulTxIdGet()
      stubSuccessfulIncorpDataGet(date = incorpDate)
      stubSuccessfulCompanyNameGet()
      stubAudits()
      cacheSessionData[ConditionalDateFormElement](internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(false, None))
      cacheSessionData(internalId, ThresholdNextThirtyDaysId.toString,false)
      cacheSessionData[Boolean](internalId, VoluntaryRegistrationId.toString, true)

      val request = buildClient("/gone-over-threshold-period").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
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