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

import common.enums.CacheKeys.IneligibilityReason
import connectors.KeystoreConnector
import fixtures.VatRegistrationFixture
import helpers.{S4LMockSugar, VatRegSpec}
import models.CurrentProfile
import models.api.VatServiceEligibility
import models.view.EligibilityQuestion
import models.view.EligibilityQuestion.{ApplyingForVatExemptionQuestion, DoingBusinessAbroadQuestion, HaveNinoQuestion}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class EligibilityControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {
    val testController = new EligibilityController(mockKeystoreConnector,
      mockCurrentProfileService,
      mockMessages,
      mockVatRegistrationService,
      mockS4LService) {
      override val authConnector: AuthConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }

    def setupIneligibilityReason(keystoreConnector: KeystoreConnector, question: EligibilityQuestion) =
      when(mockKeystoreConnector.fetchAndGet[String](ArgumentMatchers.eq(IneligibilityReason.toString))(any(), any()))
        .thenReturn(Some(question.name).pure)
  }

  "GET EligibilityController.showExemptionCriteria()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      save4laterReturnsViewModel[VatServiceEligibility](validServiceEligibility)()
      val expectedTitle = "Will the company apply for a VAT registration exception or exemption?"
      callAuthorised(testController.showExemptionCriteria())(_ includesText expectedTitle)
    }
  }

  "POST ServiceCriteriaQuestionsController.submit()" should {

    "redirect to company will do any of answer is yes" in new Setup {
      when(mockVatRegistrationService.submitVatEligibility()(any(), any())).thenReturn(validServiceEligibility.pure)
      val currentQuestion = ApplyingForVatExemptionQuestion

      setupIneligibilityReason(mockKeystoreConnector, currentQuestion)
      save4laterReturnsViewModel(validServiceEligibility)()
      save4laterExpectsSave[VatServiceEligibility]()

      submitAuthorised(testController.submitExemptionCriteria,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> currentQuestion.name,
          s"${currentQuestion.name}Radio" -> (!currentQuestion.exitAnswer).toString)
      )(_ redirectsTo "/check-if-you-can-register-for-vat/apply-for-any")
    }

    "redirect to company will do any of answer is yes if s4l and backend is empty" in new Setup {
      when(mockVatRegistrationService.submitVatEligibility()(any(), any())).thenReturn(validServiceEligibility.pure)
      val currentQuestion = ApplyingForVatExemptionQuestion

      setupIneligibilityReason(mockKeystoreConnector, currentQuestion)
      save4laterReturnsNoViewModel[VatServiceEligibility]()
      when(mockVatRegistrationService.getVatScheme()(any(),any())).thenReturn(validVatScheme.copy(vatServiceEligibility = None).pure)
      save4laterExpectsSave[VatServiceEligibility]()

      submitAuthorised(testController.submitExemptionCriteria,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> currentQuestion.name,
          s"${currentQuestion.name}Radio" -> (!currentQuestion.exitAnswer).toString)
      )(_ redirectsTo "/check-if-you-can-register-for-vat/apply-for-any")
    }

    "400 for malformed requests" in new Setup {
      val currentQuestion = ApplyingForVatExemptionQuestion

      submitAuthorised(testController.submitExemptionCriteria,
        FakeRequest().withFormUrlEncodedBody(s"${currentQuestion.name}Radio" -> "foo")
      )(_ isA 400)
    }

    "show ineligible screen on no submitted" in new Setup {
      val currentQuestion = ApplyingForVatExemptionQuestion

      when(mockVatRegistrationService.submitVatEligibility()(any(), any())).thenReturn(validServiceEligibility.pure)
      setupIneligibilityReason(mockKeystoreConnector, currentQuestion)
      save4laterReturnsViewModel(validServiceEligibility)()
      save4laterExpectsSave[VatServiceEligibility]()
      when(mockKeystoreConnector.cache[String](any(), any())(any(), any())).thenReturn(CacheMap("id", Map()).pure)

      submitAuthorised(testController.submitExemptionCriteria,
        FakeRequest().withFormUrlEncodedBody(s"${currentQuestion.name}Radio" -> (currentQuestion.exitAnswer).toString)
      )(_ redirectsTo "/check-if-you-can-register-for-vat/cant-register")
    }


  }

}
