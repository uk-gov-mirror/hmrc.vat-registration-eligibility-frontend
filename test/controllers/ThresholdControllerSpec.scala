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
import models.view.OverThresholdView
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.exceptions.TestFailedException
import play.api.http.Status
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ThresholdControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  object TestThresholdController extends ThresholdController() {
    override val authConnector = mockAuthConnector

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
      f(currentProfile)
    }
  }

  val fakeRequest = FakeRequest(routes.ThresholdController.goneOverShow())

  s"GET ${routes.ThresholdController.goneOverShow()}" should {
    "returnException if no IncorporationInfo Date present" in {
      val overThreshold = OverThresholdView(true, Some(LocalDate.of(2017, 6, 30)))

      save4laterReturnsViewModel(overThreshold)()

      assertThrows[TestFailedException]{
        callAuthorised(TestThresholdController.goneOverShow)(_ =>fail())
      }
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

      when(mockVatRegistrationService.getVatScheme()(Matchers.any(), Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validVatScheme))

      callAuthorised(TestThresholdController.goneOverShow) {
        _ includesText "VAT taxable turnover gone over"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains no data" in {
      save4laterReturnsNoViewModel[OverThresholdView]()

      when(mockVatRegistrationService.getVatScheme()(Matchers.any(), Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(emptyVatScheme))

      callAuthorised(TestThresholdController.goneOverShow) {
        _ includesText "VAT taxable turnover gone over"
      }
    }
  }

  s"POST ${routes.ThresholdController.goneOverSubmit()}" should {
    "return Exception When Incorporation date is empty" in {

      assertThrows[TestFailedException]{
        submitAuthorised(TestThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody()) {
          (_ =>fail())
        }
      }
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
        _ redirectsTo controllers.routes.ThresholdSummaryController.show.url
      }
    }

    "return 303 with valid data - no selected" in {
      save4laterExpectsSave[OverThresholdView]()

      submitAuthorised(TestThresholdController.goneOverSubmit(), fakeRequest.withFormUrlEncodedBody(
        "overThresholdRadio" -> "false"
      )) {
        _ redirectsTo controllers.routes.ThresholdSummaryController.show.url
      }
    }

  }
}
