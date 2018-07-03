package www

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import identifiers.{ThresholdInTwelveMonthsId, ThresholdNextThirtyDaysId, ThresholdPreviousThirtyDaysId, VoluntaryRegistrationId}
import models.ConditionalDateFormElement
import org.jsoup.Jsoup
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class ThresholdInTwelveMonthsControllerISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  val selectionFieldName = s"${ThresholdInTwelveMonthsId}Selection"
  val dateFieldName = s"${ThresholdInTwelveMonthsId}Date"
  val internalId = "testInternalId"
  val pageHeading = "In any 12-month period has the business gone over the VAT-registration threshold?"
  val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())

  s"GET ${controllers.routes.ThresholdInTwelveMonthsController.onPageLoad().url}" should {
    "render the page" when {
      "no data is present in mongo" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubAudits()

        val request = buildClient("/gone-over-threshold").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val response = await(request)
        response.status mustBe 200
        val document = Jsoup.parse(response.body)

        document.getElementById("main-heading").text() mustBe pageHeading
        document.getElementById(s"${ThresholdInTwelveMonthsId}Selection-true").attr("checked") mustBe ""
        document.getElementById(s"${ThresholdInTwelveMonthsId}Selection-false").attr("checked") mustBe ""
      }

      "data (true, Some(2017-12-1)) is present in mongo" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubAudits()

        cacheSessionData(internalId, s"$ThresholdInTwelveMonthsId", ConditionalDateFormElement(true, Some(LocalDate.of(2017,12,1))))

        val request = buildClient("/gone-over-threshold").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val response = await(request)
        response.status mustBe 200
        val document = Jsoup.parse(response.body)
        document.getElementById("main-heading").text() mustBe pageHeading
        document.getElementById(s"${ThresholdInTwelveMonthsId}Selection-true").attr("checked") mustBe "checked"
        document.getElementById(s"${ThresholdInTwelveMonthsId}Selection-false").attr("checked") mustBe ""
        document.getElementById(s"${ThresholdInTwelveMonthsId}Date.month").`val` mustBe "12"
        document.getElementById(s"${ThresholdInTwelveMonthsId}Date.year").`val` mustBe "2017"
      }

      "throw an exception" when {
        "when no incorp date is present" in {
          stubSuccessfulLogin()
          stubSuccessfulRegIdGet()
          stubSuccessfulTxIdGet()
          stubUnsuccessfulIncorpDataGet(status = 204)
          stubAudits()
          val request = buildClient("/gone-over-threshold").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
          val response = await(request)
          response.status mustBe 500
        }
      }
    }
  }

  s"POST ${controllers.routes.ThresholdInTwelveMonthsController.onSubmit().url}" should {
    val incorpDate = LocalDate.now().minusMonths(14)
    val dateBeforeIncorp = incorpDate.minusMonths(2)
    val dateAfterIncorp = incorpDate.plusMonths(2)

    "return a badrequest with form errors" when {
      "a date before the incorp date is passed in" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet(date = incorpDate)
        stubAudits()


        val request = buildClient("/gone-over-threshold").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("true"),
            s"$dateFieldName.month" -> Seq(s"${dateBeforeIncorp.getMonthValue}"),
            s"$dateFieldName.year" -> Seq(s"${dateBeforeIncorp.getYear}")
          ))

        val response = await(request)
        response.status mustBe 400
        val document = Jsoup.parse(response.body)
        document.getElementById("main-heading").text() mustBe pageHeading
        document.getElementById("error-summary-heading").text() mustBe "This page has errors"
        document.getElementsByAttributeValue("href",s"#$dateFieldName").text() mustBe s"Enter a date that’s after the date the business was set up: ${incorpDate.format(dateTimeFormatter)}"
      }
    }
    s"redirect to ${controllers.routes.TurnoverEstimateController.onPageLoad().url}" when {
      "yes and a valid date is submitted when previous questions answered" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet(date = incorpDate)
        stubAudits()

        cacheSessionData(internalId, ThresholdNextThirtyDaysId.toString, false)
        cacheSessionData(internalId, ThresholdPreviousThirtyDaysId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData(internalId, VoluntaryRegistrationId.toString, true)

        val request = buildClient("/gone-over-threshold").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("true"),
            s"$dateFieldName.month" -> Seq(s"${dateAfterIncorp.getMonthValue}"),
            s"$dateFieldName.year" -> Seq(s"${dateAfterIncorp.getYear}")
          ))

        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.TurnoverEstimateController.onPageLoad().url)
        verifySessionCacheData(internalId, VoluntaryRegistrationId.toString, Option.empty[Boolean])
      }
    }
    s"redirect to ${controllers.routes.VoluntaryRegistrationController.onPageLoad().url}" when {
      "no is submitted when previous questions answered with no" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet(date = incorpDate)
        stubAudits()

        cacheSessionData(internalId, ThresholdNextThirtyDaysId.toString, false)
        cacheSessionData(internalId, ThresholdPreviousThirtyDaysId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData(internalId, VoluntaryRegistrationId.toString, true)

        val request = buildClient("/gone-over-threshold").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            selectionFieldName -> Seq("false")
          ))

        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VoluntaryRegistrationController.onPageLoad().url)
        verifySessionCacheData(internalId, VoluntaryRegistrationId.toString, Some(true))
      }
    }
  }

}
