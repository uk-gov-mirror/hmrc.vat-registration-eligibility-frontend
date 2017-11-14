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
import models.api.{VatEligibilityChoice, VatExpectedThresholdPostIncorp, VatThresholdPostIncorp}
import models.view._
import models.{CurrentProfile, S4LVatEligibilityChoice}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ThresholdSummaryControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {

    val testController = new ThresholdSummaryController()(
      mockMessages,
      mockS4LService,
      mockVatRegistrationService,
      mockCurrentProfileService,
      mockVatRegFrontendService
    ) {
      override val authConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }

  }

  val fakeRequest = FakeRequest(controllers.routes.ThresholdSummaryController.show())

  "Calling threshold summary to show the threshold summary page" should {
    "return HTML with a valid threshold summary view" in new Setup {
      save4laterReturns(S4LVatEligibilityChoice(
        overThreshold = Some(OverThresholdView(false, None)),
        expectationOverThreshold = Some(ExpectationOverThresholdView(false, None))
      ))
      callAuthorised(testController.show)(_ includesText "Check and confirm your answers")
    }

    "getVatThresholdAndExpectedThreshold returns a valid VatThresholdPostIncorp and VatExpectedThresholdPostIncorp" in new Setup {
      save4laterReturns(S4LVatEligibilityChoice(expectationOverThreshold = Some(ExpectationOverThresholdView(false,None)),overThreshold = Some(OverThresholdView(false))))

      testController.getVatThresholdAndExpectedThreshold() returns (validVatThresholdPostIncorp,VatExpectedThresholdPostIncorp(false,None))
    }

    "mapToModels returns a tuple of VatExpectedThreshold and VatThreshold" in new Setup {
      testController.mapToModels(
        Some(OverThresholdView(false,None)),
        Some(ExpectationOverThresholdView(false,None))) shouldBe (VatThresholdPostIncorp(false,None),VatExpectedThresholdPostIncorp(false,None))
    }

    "getThresholdSummary maps a valid VatThresholdSummary object to a Summary object" in new Setup {
      save4laterReturns(S4LVatEligibilityChoice(
        overThreshold = Some(OverThresholdView(false, None))
      ))
      save4laterReturns(S4LVatEligibilityChoice(
        expectationOverThreshold = Some(ExpectationOverThresholdView(false, None))
      ))


      testController.getThresholdSummary().map(summary => summary.sections.length shouldBe 2)
    }
  }

  s"POST ${controllers.routes.ThresholdSummaryController.submit()}" should {
    "redirect the user to the voluntary registration page if  if Q1A3 is false" in new Setup {
      save4laterReturns(S4LVatEligibilityChoice(
        expectationOverThreshold = Some(ExpectationOverThresholdView(false,None)),
        overThreshold = Some(OverThresholdView(false, None)
        )
      ))


      callAuthorised(testController.submit) {
        _ redirectsTo controllers.routes.VoluntaryRegistrationController.show.url
      }
    }

    "redirect the user to the completion capacity page if Q1A3 is true" in new Setup {

      save4laterExpectsSave[VoluntaryRegistration]()
      save4laterReturns(S4LVatEligibilityChoice(
        expectationOverThreshold = Some(ExpectationOverThresholdView(true,Some(testDate))),
        overThreshold = Some(OverThresholdView(true, Some(testDate)))
      ))

      when(mockVatRegistrationService.submitVatEligibility()(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(validVatServiceEligibility))
      when(mockVatRegistrationService.deleteVatScheme()(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful())

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry(ArgumentMatchers.any())).thenReturn("someEntryUrl")

      callAuthorised(testController.submit) {
        _ redirectsTo s"someEntryUrl"
      }
    }
  }
}
