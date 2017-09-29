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
import models.view.VoluntaryRegistration
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class VoluntaryRegistrationControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {
    val testController = new VoluntaryRegistrationController()(mockMessages,
      mockS4LService,
      mockCurrentProfileService,
      mockVatRegistrationService,
      mockVatRegFrontendService
    ) {
      override val authConnector: AuthConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  val fakeRequest = FakeRequest(routes.VoluntaryRegistrationController.show())

  s"GET ${routes.VoluntaryRegistrationController.show()}" should {
    "return HTML Voluntary Registration  page with no Selection" in new Setup {
      val voluntaryRegistration = VoluntaryRegistration("")

      save4laterReturnsViewModel(voluntaryRegistration)()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      callAuthorised(testController.show()) {
        _ includesText "Do you want to register voluntarily for VAT?"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains data" in new Setup {
      save4laterReturnsNoViewModel[VoluntaryRegistration]()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.getVatScheme()(any(), any[HeaderCarrier]()))
        .thenReturn(Future.successful(validVatScheme))

      callAuthorised(testController.show) {
        _ includesText "Do you want to register voluntarily for VAT?"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains no data" in new Setup {
      save4laterReturnsNoViewModel[VoluntaryRegistration]()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.getVatScheme()(any(), any[HeaderCarrier]()))
        .thenReturn(Future.successful(emptyVatScheme))

      callAuthorised(testController.show) {
        _ includesText "Do you want to register voluntarily for VAT?"
      }
    }
  }

  s"POST ${routes.VoluntaryRegistrationController.submit()} with Empty data" should {
    "return 400" in new Setup {

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
      ))(result => result isA 400)
    }
  }

  s"POST ${routes.VoluntaryRegistrationController.submit()} with Voluntary Registration selected Yes" should {
    "return 303" in new Setup {
      save4laterExpectsSave[VoluntaryRegistration]()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry(Matchers.any())).thenReturn("someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationRadio" -> VoluntaryRegistration.REGISTER_YES
      ))(_ redirectsTo controllers.routes.VoluntaryRegistrationReasonController.show().url)
    }
  }

  s"POST ${routes.VoluntaryRegistrationController.submit()} with Voluntary Registration selected No" should {
    "redirect to the welcome page" in new Setup {
      when(mockS4LService.clear()(any[HeaderCarrier](), any())).thenReturn(Future.successful(validHttpResponse))
      save4laterExpectsSave[VoluntaryRegistration]()
      when(mockVatRegistrationService.deleteVatScheme()(any[HeaderCarrier](), any()))
        .thenReturn(Future.successful(()))

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlWelcome(Matchers.any())).thenReturn(s"someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationRadio" -> VoluntaryRegistration.REGISTER_NO
      ))(_ redirectsTo "someUrl")
    }
  }
}
