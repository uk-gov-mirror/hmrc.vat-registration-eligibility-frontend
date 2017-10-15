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
import models.view.{TaxableTurnover, VoluntaryRegistration}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class TaxableTurnoverControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {
    val testController = new TaxableTurnoverController()(mockMessages,
      mockS4LService,
      mockVatRegistrationService,
      mockCurrentProfileService,
      mockVatRegFrontendService
    ){

      override val authConnector: AuthConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  val fakeRequest = FakeRequest(routes.TaxableTurnoverController.show())

  s"GET ${routes.TaxableTurnoverController.show()}" should {

    "return HTML when there's a start date in S4L" in new Setup {
      val taxableTurnover = TaxableTurnover(TaxableTurnover.TAXABLE_YES)

      save4laterReturnsViewModel(taxableTurnover)()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      callAuthorised(testController.show()) {
        _ includesText "VAT taxable sales of more than £85,000 in the 30 days"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains data" in new Setup {
      save4laterReturnsNoViewModel[TaxableTurnover]()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.getVatScheme()(any(), any[HeaderCarrier]()))
        .thenReturn(Future.successful(validVatScheme))

      callAuthorised(testController.show) {
        _ includesText "VAT taxable sales of more than £85,000 in the 30 days"
      }
    }


    "return HTML when there's nothing in S4L and vatScheme contains no data" in new Setup{
      save4laterReturnsNoViewModel[TaxableTurnover]()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.getVatScheme()(any(), any[HeaderCarrier]()))
        .thenReturn(Future.successful(emptyVatScheme))

      callAuthorised(testController.show) {
        _ includesText "VAT taxable sales of more than £85,000 in the 30 days"
      }
    }
  }


  s"POST ${routes.TaxableTurnoverController.submit()} with Empty data" should {
    "return 400" in new Setup {

      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
      ))(result => result isA 400)
    }
  }

  s"POST ${routes.TaxableTurnoverController.submit()} with Taxable Turnover selected Yes" should {

    "return 303" in new Setup {
      save4laterExpectsSave[TaxableTurnover]()
      save4laterExpectsSave[VoluntaryRegistration]()

      when(mockVatRegistrationService.submitVatEligibility()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(validVatServiceEligibility))
      when(mockVatRegistrationService.deleteVatScheme()(Matchers.any(), Matchers.any())).thenReturn(Future.successful())

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry(Matchers.any())).thenReturn("someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "taxableTurnoverRadio" -> TaxableTurnover.TAXABLE_YES
      ))(_ redirectsTo "someUrl")

    }
  }

  s"POST ${routes.TaxableTurnoverController.submit()} with Taxable Turnover selected No" should {
    "return 303" in new Setup {
      save4laterExpectsSave[TaxableTurnover]()

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any())).thenReturn(Future.successful(currentProfile))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "taxableTurnoverRadio" -> TaxableTurnover.TAXABLE_NO
      ))(_ redirectsTo routes.VoluntaryRegistrationController.show.url)
    }
  }

}
