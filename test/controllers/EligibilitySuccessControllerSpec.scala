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
import models.external.IncorporationInfo
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class EligibilitySuccessControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  val specificDate = LocalDate.of(2017, 11, 12)

  class Setup {
    val testController = new EligibilitySuccessController()(
      mockMessages,
      mockKeystoreConnector,
      mockCurrentProfileService,
      mockVatRegistrationService
    ){
      override val authConnector: AuthConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
    val INCORPORATION_STATUS = "incorporationStatus"
  }

  val fakeRequest = FakeRequest(routes.EligibilitySuccessController.show())

  s"GET ${routes.EligibilitySuccessController.show()}" should {

    "return HTML when there's a eligibility success view in S4L" in new Setup {
      callAuthorised(testController.show) {
        _ includesText "You can register for VAT using this service"
      }
    }

  }

  s"POST ${routes.EligibilitySuccessController.submit()}" should {
    "return 303 with valid data - Company NOT INCORPORATED" in new Setup{
      when(mockVatRegistrationService.getIncorporationDate(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

      mockKeystoreFetchAndGet[IncorporationInfo](INCORPORATION_STATUS, None)

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody()) {
        _ redirectsTo controllers.routes.TaxableTurnoverController.show().url
      }
    }

  }

  s"POST ${routes.EligibilitySuccessController.submit()}" should {
    "return 303 with valid data - Company INCORPORATED" in new Setup{
      when(mockVatRegistrationService.getIncorporationDate(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(specificDate)))

      mockKeystoreFetchAndGet[IncorporationInfo](INCORPORATION_STATUS, Some(testIncorporationInfo))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody()) {
        _ redirectsTo controllers.routes.ThresholdController.goneOverShow().url
      }
    }

  }
}
