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

import common.enums.CacheKeys.IneligibilityReason
import common.enums.{EligibilityQuestions => Questions}
import fixtures.VatRegistrationFixture
import helpers.{ControllerSpec, FutureAssertions}
import mocks.{EligibilityServiceMock, ThresholdServiceMock}
import models.CurrentProfile
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Inspectors
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class EligibilityControllerSpec extends ControllerSpec with GuiceOneAppPerTest with VatRegistrationFixture
                                with EligibilityServiceMock with Inspectors with FutureAssertions with ThresholdServiceMock {

  class Setup {
    val testController = new EligibilityController {
      override val authConnector: AuthConnector = mockAuthClientConnector
      override val eligibilityService = mockEligibilityService
      override val keystoreConnector = mockKeystoreConnector
      override val currentProfileService = mockCurrentProfileService
      override val thresholdService = mockThresholdService

      override val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  val currentVatThreshold = "12345"

  "GET EligibilityController.showExemptionCriteria()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetEligibility(Future.successful(validEligibility))

      val expectedTitle = "Will the company apply for a VAT registration exception or exemption?"
      callAuthenticated(testController.showExemptionCriteria())(a => a includesText expectedTitle)
    }
  }

  "POST EligibilityController.submitExemptionCriteria()" should {

    "redirect to 'company will do any of' if answer is yes" in new Setup {
        mockSaveEligibility(Future.successful(validEligibility))
      submitAuthorised(testController.submitExemptionCriteria,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> Questions.applyingForVatExemption,
          s"${Questions.applyingForVatExemption}Radio" -> "false")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/apply-for-any")
    }

    "400 for malformed requests" in new Setup {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testController.submitExemptionCriteria,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.applyingForVatExemption}Radio" -> "foo")
      )(_ isA 400)
    }

    "show ineligible screen on yes submitted" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      when(mockKeystoreConnector.cache[String](any(), any())(any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      submitAuthorised(testController.submitExemptionCriteria,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.applyingForVatExemption}Radio" -> "true")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/cant-register")
    }
  }

  "GET EligibilityController.showHaveNino()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      mockGetEligibility(Future.successful(validEligibility))

      val expectedTitle = "Do you have a UK National Insurance number?"
      callAuthenticated(testController.showHaveNino())(_ includesText expectedTitle)
    }
  }

  "POST EligibilityController.submitHaveNino()" should {

    "redirect to 'doing business abroad' if answer is yes" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))
      submitAuthorised(testController.submitHaveNino,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> Questions.haveNino,
          s"${Questions.haveNino}Radio" -> "true")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/international-business")
    }

    "400 for malformed requests" in new Setup {
      submitAuthorised(testController.submitHaveNino,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.haveNino}Radio" -> "foo")
      )(_ isA 400)
    }

    "show ineligible screen on no submitted" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      when(mockKeystoreConnector.cache[String](any(), any())(any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      submitAuthorised(testController.submitHaveNino,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.haveNino}Radio" -> "false")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/cant-register")
    }
  }

  "GET EligibilityController.showDoingBusinessAbroad()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      mockGetEligibility(Future.successful(validEligibility))

      val expectedTitle = "Will the company do international business"
      callAuthenticated(testController.showDoingBusinessAbroad())(_ includesText expectedTitle)
    }
  }

  "POST EligibilityController.submitDoingBusinessAbroad()" should {

    "redirect to 'do any apply to you' if answer is no" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      submitAuthorised(testController.submitDoingBusinessAbroad,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> Questions.doingBusinessAbroad,
          s"${Questions.doingBusinessAbroad}Radio" -> "false")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/involved-more-business-changing-status")
    }

    "400 for malformed requests" in new Setup {
      submitAuthorised(testController.submitDoingBusinessAbroad,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.doingBusinessAbroad}Radio" -> "foo")
      )(_ isA 400)
    }

    "show ineligible screen on yes submitted" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))
      when(mockKeystoreConnector.cache[String](any(), any())(any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      submitAuthorised(testController.submitDoingBusinessAbroad,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.doingBusinessAbroad}Radio" -> "true")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/cant-register")
    }
  }

  "GET EligibilityController.showDoAnyApplyToYou()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      mockGetEligibility(Future.successful(validEligibility))
      val expectedTitle = "Are you involved with more than one business or changing the legal status of your business?"
      callAuthenticated(testController.showDoAnyApplyToYou())(_ includesText expectedTitle)
    }
  }

  "POST EligibilityController.submitDoAnyApplyToYou()" should {

    "redirect to 'do any apply to you' if answer is no" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      submitAuthorised(testController.submitDoAnyApplyToYou,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> Questions.doAnyApplyToYou,
          s"${Questions.doAnyApplyToYou}Radio" -> "false")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/agricultural-flat-rate")
    }

    "400 for malformed requests" in new Setup {
      submitAuthorised(testController.submitDoAnyApplyToYou,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.doAnyApplyToYou}Radio" -> "foo")
      )(_ isA 400)
    }

    "show ineligible screen on yes submitted" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      when(mockKeystoreConnector.cache[String](any(), any())(any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      submitAuthorised(testController.submitDoAnyApplyToYou,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.doAnyApplyToYou}Radio" -> "true")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/cant-register")
    }
  }

  "GET EligibilityController.showApplyingForAnyOf()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      mockGetEligibility(Future.successful(validEligibility))

      val expectedTitle = "Is the company applying for either the Agricultural Flat Rate Scheme or the Annual Accounting Scheme?"
      callAuthenticated(testController.showApplyingForAnyOf())(_ includesText expectedTitle)
    }
  }

  "POST EligibilityController.submitApplyingForAnyOf()" should {

    "redirect to 'do any apply to you' if answer is no" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      submitAuthorised(testController.submitApplyingForAnyOf,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> Questions.applyingForAnyOf,
          s"${Questions.applyingForAnyOf}Radio" -> "false")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/apply-exception-exemption")
    }

    "400 for malformed requests" in new Setup {
      submitAuthorised(testController.submitApplyingForAnyOf,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.applyingForAnyOf}Radio" -> "foo")
      )(_ isA 400)
    }

    "show ineligible screen on yes submitted" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      when(mockKeystoreConnector.cache[String](any(), any())(any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      submitAuthorised(testController.submitApplyingForAnyOf,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.applyingForAnyOf}Radio" -> "true")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/cant-register")
    }
  }

  "GET EligibilityController.showCompanyWillDoAnyOf()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      mockGetEligibility(Future.successful(validEligibility))

      val expectedTitle = "Will the company do any of the following once"
      callAuthenticated(testController.showCompanyWillDoAnyOf())(_ includesText expectedTitle)
    }
  }

  "POST EligibilityController.submitCompanyWillDoAnyOf()" should {

    "redirect to 'do any apply to you' if answer is no" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      submitAuthorised(testController.submitCompanyWillDoAnyOf,
        FakeRequest().withFormUrlEncodedBody(
          "question" -> Questions.companyWillDoAnyOf,
          s"${Questions.companyWillDoAnyOf}Radio" -> "false")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/check-confirm-eligibility")
    }

    "400 for malformed requests" in new Setup {

      submitAuthorised(testController.submitCompanyWillDoAnyOf,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.companyWillDoAnyOf}Radio" -> "foo")
      )(_ isA 400)
    }

    "show ineligible screen on yes submitted" in new Setup {
      mockSaveEligibility(Future.successful(validEligibility))

      when(mockKeystoreConnector.cache[String](any(), any())(any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      submitAuthorised(testController.submitCompanyWillDoAnyOf,
        FakeRequest().withFormUrlEncodedBody(s"${Questions.companyWillDoAnyOf}Radio" -> "true")
      )(_ redirectsTo "/check-if-you-can-register-for-vat/cant-register")
    }
  }

  "GET ineligible screen" should {

    //below the "" empty css class indicates that the section is showing (not "hidden")
    val eligibilityQuestions = Seq[(Questions.Value, String)](
      Questions.haveNino                -> """id="nino-text" class=""""",
      Questions.doingBusinessAbroad     -> """id="business-abroad-text" class=""""",
      Questions.doAnyApplyToYou         -> """id="do-any-apply-to-you-text" class=""""",
      Questions.applyingForAnyOf        -> """id="applying-for-any-of-text" class=""""",
      Questions.applyingForVatExemption -> """id="applying-for-vat-exemption-text"""",
      Questions.companyWillDoAnyOf      -> """id="company-will-do-any-of-text" class="""""
    )

    forAll(eligibilityQuestions) { case (question, expectedTitle) =>
      s"return HTML for relevant $question page" in new Setup {

        mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

        when(mockCurrentProfileService.getCurrentProfile()(ArgumentMatchers.any()))
          .thenReturn(Future.successful(currentProfile))

        when(mockKeystoreConnector.fetchAndGet[String](ArgumentMatchers.eq(IneligibilityReason.toString))(any(), any()))
          .thenReturn(Future.successful(Some(question.toString)))
        callAuthenticated(testController.ineligible())(_ includesText expectedTitle)
      }
    }

    "return an Internal Server Error when no reason found in keystore" in new Setup {
      when(mockKeystoreConnector.fetchAndGet[Questions.Value](ArgumentMatchers.eq(IneligibilityReason.toString))(any(), any()))
        .thenReturn(Future.successful(None))

      callAuthenticated(testController.ineligible()) {
        result => status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

}