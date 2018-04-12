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
import models.view.{ExpectationOverThresholdView, OverThresholdView}
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

  val testThresholdController = new ThresholdController {
    override val thresholdService = mockThresholdService
    val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
    val authConnector = mockAuthClientConnector
    val currentProfileService = mockCurrentProfileService

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
     f(currentProfile)
    }
  }

  val testThresholdControllerWithoutIncorpDate = new ThresholdController {
    override val thresholdService = mockThresholdService
    val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
    val authConnector = mockAuthClientConnector
    val currentProfileService = mockCurrentProfileService

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
      f(currentProfile.copy(incorporationDate = None))
    }
  }

  val currentVatThreshold = "12345"

  val fakeRequest = FakeRequest(routes.ThresholdController.goneOverShow())

  s"GET ${routes.ThresholdController.goneOverShow()}" should {
    val expectedText = "VAT taxable turnover gone over"

    "returnException if no IncorporationInfo Date present" in {
      val res = intercept[Exception](callAuthenticated(testThresholdControllerWithoutIncorpDate.goneOverShow) {
        a => status(a) mustBe 500
      }).getMessage
      res mustBe expectedError
    }

    "return HTML when there is no view data" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThresholdViewModel[OverThresholdView](Future.successful(None))

      callAuthenticated(testThresholdController.goneOverShow) { res =>
        res includesText expectedText
        res passJsoupTest { doc =>
          doc.getElementById("pageHeading").text must include(incorpDate.format(FORMAT_DD_MMMM_Y))
          doc.getElementsByAttribute("checked").size mustBe 0
          doc.getElementById("overThreshold.month").attr("value") mustBe ""
          doc.getElementById("overThreshold.year").attr("value") mustBe ""
        }
      }
    }

    "return HTML when there's an over threshold view data with date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThresholdViewModel[OverThresholdView](Future.successful(Some(validOverThresholdView.copy(true, Some(LocalDate.of(2017, 6, 30))))))

      callAuthenticated(testThresholdController.goneOverShow) {
        _ passJsoupTest { doc =>
          doc.getElementById("overThreshold.month").attr("value") mustBe "6"
          doc.getElementById("overThreshold.year").attr("value") mustBe "2017"
          doc.getElementById("overThresholdRadio-true").attr("checked") mustBe "checked"
        }
      }
    }

    "return HTML when there's an over threshold view data with no date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThresholdViewModel[OverThresholdView](Future.successful(Some(validOverThresholdView.copy(false, None))))

      callAuthenticated(testThresholdController.goneOverShow) {
        _ passJsoupTest { doc =>
          doc.getElementById("overThreshold.month").attr("value") mustBe ""
          doc.getElementById("overThreshold.year").attr("value") mustBe ""
          doc.getElementById("overThresholdRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }

  s"POST ${routes.ThresholdController.goneOverSubmit()}" should {
    "return Exception When Incorporation date is empty" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      val res = intercept[Exception](submitAuthorised
      (testThresholdControllerWithoutIncorpDate.goneOverSubmit, fakeRequest.withFormUrlEncodedBody())
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedError
    }

    "return 400 when no data posted" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(
        testThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody()) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 400 when partial data is posted" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(
        testThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
          "overThresholdRadio" -> "true",
          "overThreshold.month" -> "",
          "overThreshold.year" -> "2017"
        )) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 400 with incorrect data (date before incorporation date) - yes selected" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      val thresholdOverThresholdTrue = validThresholdPostIncorp.copy(overThreshold = Some(OverThresholdView(true, Some(LocalDate.of(2017, 6, 30)))))
      mockSaveThreshold(Future.successful(thresholdOverThresholdTrue))

      submitAuthorised(testThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "true",
        "overThreshold.month" -> "6",
        "overThreshold.year" -> "2016"
      )) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 303 with valid data - yes selected" in {
      val thresholdOverThresholdTrue = validThresholdPostIncorp.copy(overThreshold = Some(OverThresholdView(true, Some(LocalDate.of(2017, 1, 30)))))
      mockSaveThreshold(Future.successful(thresholdOverThresholdTrue))
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "true",
        "overThreshold.month" -> "1",
        "overThreshold.year" -> "2017"
      )) {
        _ redirectsTo controllers.routes.ThresholdController.expectationOverShow().url
      }
    }

    "return 303 with valid data - no selected" in {
      mockSaveThreshold(Future.successful(validThresholdPostIncorp))
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "false"
      )) {
        _ redirectsTo controllers.routes.ThresholdController.expectationOverShow().url
      }
    }

  }
  s"GET ${routes.ThresholdController.expectationOverShow()}" should {
    val thresholdexpectOverThresholdTrue = ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 6, 30)))
    val thresholdexpectOverThresholdFalse = ExpectationOverThresholdView(false, None)

    "returnException if no IncorporationInfo Date present" in {
      mockGetThresholdViewModel[ExpectationOverThresholdView](Future.successful(Some(thresholdexpectOverThresholdTrue)))
      val res = intercept[Exception](callAuthenticated(testThresholdControllerWithoutIncorpDate.expectationOverShow)
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedError
    }

    "return 200 and the page is NOT prepopulated if there is no view data" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThresholdViewModel[ExpectationOverThresholdView](Future.successful(None))

      callAuthenticated(testThresholdController.expectationOverShow()) {
        _ passJsoupTest { doc =>
          doc.getElementById("expectationOverThreshold.day").attr("value") mustBe ""
          doc.getElementById("expectationOverThreshold.month").attr("value") mustBe ""
          doc.getElementById("expectationOverThreshold.year").attr("value") mustBe ""
          doc.getElementById("expectationOverThresholdRadio-true").attr("checked") mustBe ""
        }
      }
    }

    "return 200 and elements are populated when there's a over threshold view with date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThresholdViewModel[ExpectationOverThresholdView](Future.successful(Some(thresholdexpectOverThresholdTrue)))

      callAuthenticated(testThresholdController.expectationOverShow()) {
        _ passJsoupTest { doc =>
          doc.getElementById("expectationOverThreshold.day").attr("value") mustBe "30"
          doc.getElementById("expectationOverThreshold.month").attr("value") mustBe "6"
          doc.getElementById("expectationOverThreshold.year").attr("value") mustBe "2017"
          doc.getElementById("expectationOverThresholdRadio-true").attr("checked") mustBe "checked"
        }
      }
    }

    "return 200 and elements are populated when there's a over threshold view with no date" in {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      mockGetThresholdViewModel[ExpectationOverThresholdView](Future.successful(Some(thresholdexpectOverThresholdFalse)))

      callAuthenticated(testThresholdController.expectationOverShow()) {
        _ passJsoupTest { doc =>
          doc.getElementById("expectationOverThreshold.day").attr("value") mustBe ""
          doc.getElementById("expectationOverThreshold.month").attr("value") mustBe ""
          doc.getElementById("expectationOverThreshold.year").attr("value") mustBe ""
          doc.getElementById("expectationOverThresholdRadio-true").attr("checked") mustBe ""
          doc.getElementById("expectationOverThresholdRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }
  s"POST ${routes.ThresholdController.expectationOverSubmit()}" should {
    "return Exception When Incorporation date is empty" in {

      val res = intercept[Exception](submitAuthorised
      (testThresholdControllerWithoutIncorpDate.expectationOverSubmit(), fakeRequest.withFormUrlEncodedBody())
      (a => status(a) mustBe 500)).getMessage
      res mustBe expectedError
    }

    "return 400 when no data posted" in {

      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(
        testThresholdController.expectationOverSubmit(), fakeRequest.withFormUrlEncodedBody()) {
        status(_) mustBe Status.BAD_REQUEST
      }
    }

    "return 303 with valid data - yes selected" in {
      val thresholdexpectOverThresholdTrue = ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 1, 1)))

      mockGetThresholdViewModel[ExpectationOverThresholdView](Future.successful(Some(thresholdexpectOverThresholdTrue)))

      mockSaveThreshold(Future.successful(validThresholdPostIncorp))

      submitAuthorised(testThresholdController.expectationOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "expectationOverThresholdRadio" -> "true",
        "expectationOverThreshold.day" -> "01",
        "expectationOverThreshold.month" -> "01",
        "expectationOverThreshold.year" -> "2017"
      )) {
        _ redirectsTo controllers.routes.ThresholdSummaryController.show().url
      }
    }

  }
}
