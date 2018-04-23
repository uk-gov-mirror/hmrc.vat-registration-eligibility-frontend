/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import helpers.{ControllerSpec, FutureAssertions, MockMessages}
import mocks.ThresholdServiceMock
import models.CurrentProfile
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.view.ThresholdView
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ThresholdControllerSpec extends ControllerSpec with GuiceOneAppPerTest with MockMessages
                              with VatRegistrationFixture with ThresholdServiceMock with FutureAssertions {

  val expectedError = "Date of Incorporation data expected to be found in Incorporation"
  val expectedErrorNotWithin = "Incorporation date not within last 12 months"

  val testThresholdController = new ThresholdController {
    override val thresholdService = mockThresholdService
    override val vatRegFrontendService = mockVatRegFrontendService
    val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
    val authConnector = mockAuthClientConnector
    val currentProfileService = mockCurrentProfileService

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
     f(currentProfile)
    }
  }

  val testThresholdControllerWithinYear = new ThresholdController {
    override val thresholdService = mockThresholdService
    override val vatRegFrontendService = mockVatRegFrontendService
    val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
    val authConnector = mockAuthClientConnector
    val currentProfileService = mockCurrentProfileService

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
     f(currentProfile.copy(incorporationDate = Some(incorpDateWithinYear)))
    }
  }

  val testThresholdControllerWithoutIncorpDate = new ThresholdController {
    override val thresholdService = mockThresholdService
    override val vatRegFrontendService = mockVatRegFrontendService
    val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
    val authConnector = mockAuthClientConnector
    val currentProfileService = mockCurrentProfileService

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
      f(currentProfile.copy(incorporationDate = None))
    }
  }

  val currentVatThreshold = "12345"

  val fakeRequest = FakeRequest(routes.ThresholdController.goneOverTwelveShow())

  s"GET ${routes.ThresholdController.goneOverTwelveShow()}" should {

    val heading = "In any 12-month period has the company gone over the VAT-registration threshold?"

    "returnException if no IncorporationInfo Date present" in {
      val res = intercept[Exception](callAuthenticated(testThresholdControllerWithoutIncorpDate.goneOverTwelveShow) {
        a => status(a) mustBe 500
      }).getMessage
      res mustBe expectedError
    }

    "return HTML when there is no view data" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold))

      callAuthenticated(testThresholdController.goneOverTwelveShow) { res =>
        res passJsoupTest { doc =>
          doc.getElementById("pageHeading").text must include(heading)
          doc.getElementsByAttribute("checked").size mustBe 0
          doc.getElementById("overThreshold.month").attr("value") mustBe ""
          doc.getElementById("overThreshold.year").attr("value") mustBe ""
        }
      }
    }

    "return HTML when there's an over threshold view data with date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdOccuredTwelveMonth = Some(validThresholdView.copy(true, optTestDate)))))

      callAuthenticated(testThresholdController.goneOverTwelveShow) {
        _ passJsoupTest { doc =>
          doc.getElementById("overThreshold.month").attr("value") mustBe "3"
          doc.getElementById("overThreshold.year").attr("value") mustBe "2017"
          doc.getElementById("overThresholdRadio-true").attr("checked") mustBe "checked"
        }
      }
    }

    "return HTML when there's an over threshold view data with no date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdOccuredTwelveMonth = Some(validThresholdView.copy(false, None)))))

      callAuthenticated(testThresholdController.goneOverTwelveShow) {
        _ passJsoupTest { doc =>
          doc.getElementById("overThreshold.month").attr("value") mustBe ""
          doc.getElementById("overThreshold.year").attr("value") mustBe ""
          doc.getElementById("overThresholdRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }

  s"POST ${routes.ThresholdController.goneOverTwelveSubmit()}" should {
    "return Exception When Incorporation date is empty" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      val res = intercept[Exception](submitAuthorised
      (testThresholdControllerWithoutIncorpDate.goneOverTwelveSubmit, fakeRequest.withFormUrlEncodedBody())
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedError
    }

    "return 400 when no data posted" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(
        testThresholdController.goneOverTwelveSubmit(), fakeRequest.withFormUrlEncodedBody()) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 400 when partial data is posted" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(
        testThresholdController.goneOverTwelveSubmit(), fakeRequest.withFormUrlEncodedBody(
          "overThresholdRadio" -> "true",
          "overThreshold.month" -> "",
          "overThreshold.year" -> "2017"
        )) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 400 with incorrect data (date before incorporation date) - yes selected" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      val thresholdOverThresholdTrue = validThresholdPostIncorp.copy(overThresholdThirtyDays = Some(ThresholdView(true, optTestDate)))
      mockSaveOverTwelveThreshold(Future.successful(thresholdOverThresholdTrue))

      submitAuthorised(testThresholdController.goneOverTwelveSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "true",
        "overThreshold.month" -> "6",
        "overThreshold.year" -> "2016"
      )) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 303 with valid data - yes selected" in {
      val thresholdOverThresholdTrue = validThresholdPostIncorp.copy(overThresholdThirtyDays = Some(ThresholdView(true, Some(LocalDate.of(2017, 1, 30)))))
      mockSaveOverTwelveThreshold(Future.successful(thresholdOverThresholdTrue))
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testThresholdController.goneOverTwelveSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "true",
        "overThreshold.month" -> "1",
        "overThreshold.year" -> "2017"
      )) {
        _ redirectsTo controllers.routes.ThresholdSummaryController.show().url
      }
    }

    "return 303 with valid data - no selected" in {
      mockSaveOverTwelveThreshold(Future.successful(validThresholdPostIncorp))
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testThresholdController.goneOverTwelveSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "false"
      )) {
        _ redirectsTo controllers.routes.ThresholdSummaryController.show().url
      }
    }
  }

  s"GET ${routes.ThresholdController.goneOverSinceIncorpShow()}" should {

    val heading = "has the company made more than £12345 in VAT-taxable sales?"

    "returnException if no IncorporationInfo Date present" in {
      val res = intercept[Exception](callAuthenticated(testThresholdControllerWithoutIncorpDate.goneOverSinceIncorpShow) {
        a => status(a) mustBe 500
      }).getMessage
      res mustBe expectedError
    }

    "returnException if incorp date is not within year" in {
      val res = intercept[Exception](callAuthenticated(testThresholdController.goneOverSinceIncorpShow) {
        a => status(a) mustBe 500
      }).getMessage
      res mustBe expectedErrorNotWithin
    }

    "return HTML when there is no view data" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold))

      callAuthenticated(testThresholdControllerWithinYear.goneOverSinceIncorpShow) { res =>
        res passJsoupTest { doc =>
          doc.getElementById("pageHeading").text must include(heading)
          doc.getElementsByAttribute("checked").size mustBe 0
        }
      }
    }

    "return HTML when there's an over threshold view data with date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdOccuredTwelveMonth = Some(validThresholdView.copy(true, optTestDate)))))

      callAuthenticated(testThresholdControllerWithinYear.goneOverSinceIncorpShow) {
        _ passJsoupTest { doc =>
          doc.getElementById("overThresholdSinceIncorpRadio-true").attr("checked") mustBe "checked"
        }
      }
    }

    "return HTML when there's an over threshold view data with no date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdOccuredTwelveMonth = Some(validThresholdView.copy(false, None)))))

      callAuthenticated(testThresholdControllerWithinYear.goneOverSinceIncorpShow) {
        _ passJsoupTest { doc =>
          doc.getElementById("overThresholdSinceIncorpRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }

  s"POST ${routes.ThresholdController.goneOverSinceIncorpSubmit()}" should {
    "return Exception When Incorporation date is empty" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      val res = intercept[Exception](submitAuthorised
      (testThresholdControllerWithoutIncorpDate.goneOverSinceIncorpSubmit, fakeRequest.withFormUrlEncodedBody())
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedError
    }

    "return Exception When Incorporation date is not within year" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      val res = intercept[Exception](submitAuthorised
      (testThresholdController.goneOverSinceIncorpSubmit, fakeRequest.withFormUrlEncodedBody())
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedErrorNotWithin
    }

    "return 400 when no data posted" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testThresholdControllerWithinYear.goneOverSinceIncorpSubmit(), fakeRequest.withFormUrlEncodedBody()) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 303 with valid data - yes selected" in {
      val thresholdOverThresholdTrue = validThresholdPostIncorp.copy(overThresholdThirtyDays = Some(ThresholdView(true, Some(LocalDate.of(2017, 1, 30)))))
      mockSaveOverSinceIncorp(Future.successful(thresholdOverThresholdTrue))
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testThresholdControllerWithinYear.goneOverSinceIncorpSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdSinceIncorpRadio" -> "true"
      )) {
        _ redirectsTo controllers.routes.ThresholdController.overThresholdThirtyShow().url
      }
    }

    "return 303 with valid data - no selected" in {
      mockSaveOverSinceIncorp(Future.successful(validThresholdPostIncorp))
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testThresholdControllerWithinYear.goneOverSinceIncorpSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdSinceIncorpRadio" -> "false"
      )) {
        _ redirectsTo controllers.routes.ThresholdController.overThresholdThirtyShow().url
      }
    }
  }

  s"GET ${routes.ThresholdController.overThresholdThirtyShow()} with an incorporated company" should {

    val heading = "Over the next 30 days, do you think the company will make more than £12345 in VAT-taxable sales?"

    "return non prepopulated HTML" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold))
      callAuthenticated(testThresholdController.overThresholdThirtyShow) { res =>
        res passJsoupTest { doc =>
          doc.getElementById("pageHeading").text must include(heading)
          doc.getElementsByAttribute("checked").size mustBe 0
        }
      }
    }
    "return prepopulated HTML" when {
      "there's an over threshold view data with date" in {
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
        mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdThirtyDays = Some(validThresholdView.copy(true, optTestDate)))))

        callAuthenticated(testThresholdController.overThresholdThirtyShow) {
          _ passJsoupTest { doc =>
            doc.getElementById("overThresholdThirtyRadio-true").attr("checked") mustBe "checked"
          }
        }
      }
      "there's an over threshold view data with no date" in {
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
        mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdThirtyDays = Some(validThresholdView.copy(false, None)))))

        callAuthenticated(testThresholdController.overThresholdThirtyShow) {
          _ passJsoupTest { doc =>
            doc.getElementById("overThresholdThirtyRadio-false").attr("checked") mustBe "checked"
          }
        }
      }
    }
  }

  s"GET ${routes.ThresholdController.overThresholdThirtyShow()} with a non incorporated company" should {

    val heading = "Do you expect the company to make VAT taxable sales of more than £12345 in the 30 days after it's registered with Companies House?"

    "return non prepopulated HTML" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold))
      callAuthenticated(testThresholdControllerWithoutIncorpDate.overThresholdThirtyShow) { res =>
        res passJsoupTest { doc =>
          doc.getElementById("pageHeading").text must include(heading)
          doc.getElementsByAttribute("checked").size mustBe 0
        }
      }
    }
    "return prepopulated HTML" when {
      "there's an over threshold view data with true selected" in {
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
        mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdThirtyDaysPreIncorp = Some(true))))

        callAuthenticated(testThresholdControllerWithoutIncorpDate.overThresholdThirtyShow) {
          _ passJsoupTest { doc =>

            doc.getElementById("overThresholdThirtyPreIncorpRadio-true").attr("checked") mustBe "checked"
          }
        }
      }
      "there's an over threshold view data with false selcted" in {
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
        mockGetThreshold(Future.successful(emptyThreshold.copy(overThresholdThirtyDaysPreIncorp = Some(false))))

        callAuthenticated(testThresholdControllerWithoutIncorpDate.overThresholdThirtyShow) {
          _ passJsoupTest { doc =>
            doc.getElementById("overThresholdThirtyPreIncorpRadio-false").attr("checked") mustBe "checked"
          }
        }
      }
    }
  }


  s"POST ${routes.ThresholdController.overThresholdThirtySubmit()}" should {
    "return 400" when {
      "no data posted" in {
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

        submitAuthorised(
          testThresholdController.overThresholdThirtySubmit(), fakeRequest.withFormUrlEncodedBody()) {
          status(_) mustBe Status.BAD_REQUEST
        }
      }
    }
    "return 303 with valid data for an incorped company" when {
      "yes selected" in {
        val thresholdOverThresholdTrue = validThresholdPostIncorp.copy(overThresholdThirtyDays = Some(ThresholdView(true, Some(LocalDate.of(2017, 1, 30)))))
        mockSaveOverThreshold(Future.successful(thresholdOverThresholdTrue))
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

        submitAuthorised(testThresholdController.overThresholdThirtySubmit(), fakeRequest.withFormUrlEncodedBody(
          "overThresholdThirtyRadio" -> "true"
        )) {
          _ redirectsTo controllers.routes.ThresholdController.pastThirtyDaysShow().url
        }
      }
      "no selected" in {
        mockSaveOverThreshold(Future.successful(validThresholdPostIncorp))
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

        submitAuthorised(testThresholdController.overThresholdThirtySubmit(), fakeRequest.withFormUrlEncodedBody(
          "overThresholdThirtyRadio" -> "false"
        )) {
          _ redirectsTo controllers.routes.ThresholdController.pastThirtyDaysShow().url
        }
      }
    }
    "return 303 with valid data for an unincorped company" when {
      "yes selected" in {
        val thresholdOverThresholdTrue = validThresholdPostIncorp.copy(overThresholdThirtyDays = Some(ThresholdView(true, Some(LocalDate.of(2017, 1, 30)))))
        mockSaveOverThresholdThirtyPreIncorp(Future.successful(thresholdOverThresholdTrue))
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

        when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry)
          .thenReturn("someEntryUrl")

        submitAuthorised(testThresholdControllerWithoutIncorpDate.overThresholdThirtySubmit(), fakeRequest.withFormUrlEncodedBody(
          "overThresholdThirtyPreIncorpRadio" -> "true"
        )) {
          _ redirectsTo "someEntryUrl"
        }
      }
      "no selected" in {
        mockSaveOverThresholdThirtyPreIncorp(Future.successful(validThresholdPostIncorp))
        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

        submitAuthorised(testThresholdControllerWithoutIncorpDate.overThresholdThirtySubmit(), fakeRequest.withFormUrlEncodedBody(
          "overThresholdThirtyPreIncorpRadio" -> "false"
        )) {
          _ redirectsTo controllers.routes.VoluntaryRegistrationController.show().url
        }
      }
    }
  }

  s"GET ${routes.ThresholdController.pastThirtyDaysShow()}" should {
    val thresholdexpectOverThresholdTrue = ThresholdView(true, optTestDate)
    val thresholdexpectOverThresholdFalse = ThresholdView(false, None)

    "returnException if no IncorporationInfo Date present" in {
      mockGetThreshold(Future.successful(emptyThreshold.copy(pastOverThresholdThirtyDays = Some(thresholdexpectOverThresholdTrue))))
      val res = intercept[Exception](callAuthenticated(testThresholdControllerWithoutIncorpDate.pastThirtyDaysShow)
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedError
    }

    "return 200 and the page is NOT prepopulated if there is no view data" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold))

      callAuthenticated(testThresholdController.pastThirtyDaysShow()) {
        _ passJsoupTest { doc =>
          doc.getElementById("pastThirtyDayPeriod.day").attr("value") mustBe ""
          doc.getElementById("pastThirtyDayPeriod.month").attr("value") mustBe ""
          doc.getElementById("pastThirtyDayPeriod.year").attr("value") mustBe ""
          doc.getElementById("pastThirtyDayPeriodRadio-true").attr("checked") mustBe ""
        }
      }
    }

    "return 200 and elements are populated when there's a over threshold view with date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold.copy(pastOverThresholdThirtyDays = Some(thresholdexpectOverThresholdTrue))))

      callAuthenticated(testThresholdController.pastThirtyDaysShow()) {
        _ passJsoupTest { doc =>
          doc.getElementById("pastThirtyDayPeriod.day").attr("value") mustBe "21"
          doc.getElementById("pastThirtyDayPeriod.month").attr("value") mustBe "3"
          doc.getElementById("pastThirtyDayPeriod.year").attr("value") mustBe "2017"
          doc.getElementById("pastThirtyDayPeriodRadio-true").attr("checked") mustBe "checked"
        }
      }
    }

    "return 200 and elements are populated when there's a over threshold view with no date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      mockGetThreshold(Future.successful(emptyThreshold.copy(pastOverThresholdThirtyDays = Some(thresholdexpectOverThresholdFalse))))

      callAuthenticated(testThresholdController.pastThirtyDaysShow()) {
        _ passJsoupTest { doc =>
          doc.getElementById("pastThirtyDayPeriod.day").attr("value") mustBe ""
          doc.getElementById("pastThirtyDayPeriod.month").attr("value") mustBe ""
          doc.getElementById("pastThirtyDayPeriod.year").attr("value") mustBe ""
          doc.getElementById("pastThirtyDayPeriodRadio-true").attr("checked") mustBe ""
          doc.getElementById("pastThirtyDayPeriodRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }
  s"POST ${routes.ThresholdController.pastThirtyDaysSubmit()}" should {
    "return Exception When Incorporation date is empty" in {

      val res = intercept[Exception](submitAuthorised
      (testThresholdControllerWithoutIncorpDate.pastThirtyDaysSubmit(), fakeRequest.withFormUrlEncodedBody())
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedError
    }

    "return 400 when no data posted" in {

      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(
        testThresholdController.pastThirtyDaysSubmit(), fakeRequest.withFormUrlEncodedBody()) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 303 with valid data - yes selected" in {
      val thresholdexpectOverThresholdTrue = ThresholdView(true, Some(LocalDate.of(2017, 1, 1)))

      mockGetThreshold(Future.successful(emptyThreshold.copy(pastOverThresholdThirtyDays = Some(thresholdexpectOverThresholdTrue))))
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockPastThirty(Future.successful(validThresholdPostIncorp))

      submitAuthorised(testThresholdController.pastThirtyDaysSubmit(), fakeRequest.withFormUrlEncodedBody(
        "pastThirtyDayPeriodRadio" -> "true",
        "pastThirtyDayPeriod.day" -> "01",
        "pastThirtyDayPeriod.month" -> "01",
        "pastThirtyDayPeriod.year" -> "2017"
      )) {
        _ redirectsTo controllers.routes.ThresholdController.goneOverTwelveShow().url
      }
    }

  }
}
