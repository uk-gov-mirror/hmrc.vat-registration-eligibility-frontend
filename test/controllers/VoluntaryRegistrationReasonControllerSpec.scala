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

import fixtures.VatRegistrationFixture
import helpers.{S4LMockSugar, VatRegSpec}
import models.CurrentProfile
import models.view.VoluntaryRegistrationReason
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class VoluntaryRegistrationReasonControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {
    val testController = new VoluntaryRegistrationReasonController()(mockMessages,
      mockS4LService,
      mockVatRegistrationService,
      mockCurrentProfileService,
      mockVatRegFrontendService
      ) {
      override val authConnector: AuthConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  val fakeRequest = FakeRequest(routes.VoluntaryRegistrationReasonController.show())

  s"GET ${routes.VoluntaryRegistrationReasonController.show()}" should {
    "return HTML Voluntary Registration Reason page with no Selection" in new Setup{
      val voluntaryRegistrationReason = VoluntaryRegistrationReason("")

      save4laterReturnsViewModel(voluntaryRegistrationReason)()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      callAuthorised(testController.show()){
        _ includesText "Which one applies to the company?"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains data" in new Setup {
      save4laterReturnsNoViewModel[VoluntaryRegistrationReason]()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.getVatScheme()(any(), any()))
        .thenReturn(Future.successful(validVatScheme))

      callAuthorised(testController.show) {
        _ includesText "Which one applies to the company?"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains no data" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      save4laterReturnsNoViewModel[VoluntaryRegistrationReason]()

      when(mockVatRegistrationService.getVatScheme()(any(), any()))
        .thenReturn(Future.successful(emptyVatScheme))

      callAuthorised(testController.show) {
        _ includesText "Which one applies to the company?"
      }
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Empty data" should {
    "return 400" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
      ))(result => result isA 400)
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Voluntary Registration Reason selected Sells" should {
    "return 303" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.submitVatEligibility()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(validVatServiceEligibility))
      when(mockVatRegistrationService.deleteVatScheme()(Matchers.any(), Matchers.any())).thenReturn(Future.successful())

      save4laterExpectsSave[VoluntaryRegistrationReason]()
      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry(Matchers.any())).thenReturn(s"someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationReasonRadio" -> VoluntaryRegistrationReason.SELLS
      ))(_ redirectsTo s"someUrl")
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Voluntary Registration Reason selected Intends to sell" should {
    "return 303" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.submitVatEligibility()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(validVatServiceEligibility))
      when(mockVatRegistrationService.deleteVatScheme()(Matchers.any(), Matchers.any())).thenReturn(Future.successful())

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry(Matchers.any())).thenReturn(s"someUrl")
      save4laterExpectsSave[VoluntaryRegistrationReason]()

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationReasonRadio" -> VoluntaryRegistrationReason.SELLS
      )){_ redirectsTo s"someUrl"}
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Voluntary Registration selected No" should {
    "redirect to the welcome page" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlWelcome(Matchers.any())).thenReturn(s"someUrl")

      when(mockS4LService.clear()(any(), any())).thenReturn(Future.successful(validHttpResponse))
      save4laterExpectsSave[VoluntaryRegistrationReason]()
      when(mockVatRegistrationService.deleteVatScheme()(any(), any())).thenReturn(Future.successful(()))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationReasonRadio" -> VoluntaryRegistrationReason.NEITHER
      ))(_ redirectsTo "someUrl")
    }
  }
}
