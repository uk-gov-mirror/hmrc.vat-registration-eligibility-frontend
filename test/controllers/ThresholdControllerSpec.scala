/*
 * Copyright 2017 HM Revenue & Customs
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
import helpers.{S4LMockSugar, VatRegSpec}
import models.CurrentProfile
import models.view.{ExpectationOverThresholdView, OverThresholdView}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ThresholdControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  val expectedError = "Date of Incorporation data expected to be found in Incorporation"

  object TestThresholdController extends ThresholdController() {
    override val authConnector = mockAuthConnector

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
     f(currentProfile)
    }
  }

  object TestThresholdControllerWithoutIncorpDate extends ThresholdController() {
    override val authConnector = mockAuthConnector

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
      f(currentProfile.copy(incorporationDate = None))
    }
  }

  val fakeRequest = FakeRequest(routes.ThresholdController.goneOverShow())

  s"GET ${routes.ThresholdController.goneOverShow()}" should {
    "returnException if no IncorporationInfo Date present" in {
      val overThreshold = OverThresholdView(true, Some(LocalDate.of(2017, 6, 30)))

      save4laterReturnsViewModel(overThreshold)()

    val res = intercept[Exception](callAuthorised(TestThresholdControllerWithoutIncorpDate.goneOverShow)
    (a => status(a) shouldBe 500)).getMessage
      res shouldBe expectedError
    }

    "return HTML when there's a over threshold view in S4L" in {
      val overThreshold = OverThresholdView(true, Some(LocalDate.of(2017, 6, 30)))

      save4laterReturnsViewModel(overThreshold)()

      callAuthorised(TestThresholdController.goneOverShow) {
        _ includesText "VAT taxable turnover gone over"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains data" in {
      save4laterReturnsNoViewModel[OverThresholdView]()

      when(mockVatRegistrationService.getVatScheme()(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validVatScheme))

      callAuthorised(TestThresholdController.goneOverShow) {
        _ includesText "VAT taxable turnover gone over"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains no data" in {
      save4laterReturnsNoViewModel[OverThresholdView]()

      when(mockVatRegistrationService.getVatScheme()(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(emptyVatScheme))

      callAuthorised(TestThresholdController.goneOverShow) {
        _ includesText "VAT taxable turnover gone over"
      }
    }
  }

  s"POST ${routes.ThresholdController.goneOverSubmit()}" should {
    "return Exception When Incorporation date is empty" in {

      val res = intercept[Exception](submitAuthorised
      (TestThresholdControllerWithoutIncorpDate.goneOverSubmit, fakeRequest.withFormUrlEncodedBody())
      (a => status(a) shouldBe 500)).getMessage
      res shouldBe expectedError
    }

    "return 400 when no data posted" in {

      submitAuthorised(
        TestThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody()) {
        status(_) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 when partial data is posted" in {

      submitAuthorised(
        TestThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
          "overThresholdRadio" -> "true",
          "overThreshold.month" -> "",
          "overThreshold.year" -> "2017"
        )) {
        status(_) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 with incorrect data (date before incorporation date) - yes selected" in {
      save4laterExpectsSave[OverThresholdView]()

      submitAuthorised(TestThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "true",
        "overThreshold.month" -> "6",
        "overThreshold.year" -> "2016"
      )) {
        status(_) shouldBe Status.BAD_REQUEST
      }
    }

    "return 303 with valid data - yes selected" in {
      save4laterExpectsSave[OverThresholdView]()

      submitAuthorised(TestThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "true",
        "overThreshold.month" -> "1",
        "overThreshold.year" -> "2017"
      )) {
        _ redirectsTo controllers.routes.ThresholdController.expectationOverShow().url
      }
    }

    "return 303 with valid data - no selected" in {
      save4laterExpectsSave[OverThresholdView]()

      submitAuthorised(TestThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "false"
      )) {
        _ redirectsTo controllers.routes.ThresholdController.expectationOverShow().url
      }
    }

  }
  s"GET ${routes.ThresholdController.expectationOverShow()}" should {
    "returnException if no IncorporationInfo Date present" in {
      val expectation = ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 6, 30)))

      save4laterReturnsViewModel(expectation)()

      val res = intercept[Exception](callAuthorised(TestThresholdControllerWithoutIncorpDate.expectationOverShow)
      (a => status(a) shouldBe 500)).getMessage
      res shouldBe expectedError
    }
    "return 200 and elements are populated when there's a over threshold view in S4L" in {
      val expectation = ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 6, 30)))

      save4laterReturnsViewModel(expectation)()

      callAuthorised(TestThresholdController.expectationOverShow()) {
        a =>
          a.map(a => {val res = Jsoup.parse(contentAsString(a))
          res.getElementById("expectationOverThreshold.day").attr("value") shouldBe("30")
          res.getElementById("expectationOverThreshold.month").attr("value") shouldBe("6")
          res.getElementById("expectationOverThreshold.year").attr("value") shouldBe("2017")
          res.getElementById("expectationOverThresholdRadio-true").attr("checked") shouldBe "checked"})

          }
      }
    }
  s"POST ${routes.ThresholdController.expectationOverSubmit()}" should {
    "return Exception When Incorporation date is empty" in {

      val res = intercept[Exception](submitAuthorised
      (TestThresholdControllerWithoutIncorpDate.expectationOverSubmit(), fakeRequest.withFormUrlEncodedBody())
      (a => status(a) shouldBe 500)).getMessage
      res shouldBe expectedError
    }

    "return 400 when no data posted" in {

      submitAuthorised(
        TestThresholdController.expectationOverSubmit(), fakeRequest.withFormUrlEncodedBody()) {
        status(_) shouldBe Status.BAD_REQUEST
      }
    }
    "return 303 with valid data - yes selected" in {
      save4laterExpectsSave[ExpectationOverThresholdView]()

      submitAuthorised(TestThresholdController.expectationOverSubmit(), fakeRequest.withFormUrlEncodedBody(
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
