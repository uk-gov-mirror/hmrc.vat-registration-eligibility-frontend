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

import fixtures.VatRegistrationFixture
import helpers.{ControllerSpec, FutureAssertions}
import mocks.ThresholdServiceMock
import models.CurrentProfile
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TaxableTurnoverControllerSpec extends ControllerSpec with GuiceOneAppPerTest with VatRegistrationFixture
  with FutureAssertions with ThresholdServiceMock {

  class Setup {
    val testController = new TaxableTurnoverController {
      override val authConnector: AuthConnector = mockAuthClientConnector
      override val thresholdService = mockThresholdService
      override val vatRegFrontendService = mockVatRegFrontendService
      override val currentProfileService = mockCurrentProfileService
      override val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  val currentVatThreshold = "12345"

  val fakeRequest = FakeRequest(routes.TaxableTurnoverController.show())

  s"GET ${routes.TaxableTurnoverController.show()}" should {
    val expectedText = s"Over the next 30 days, do you think the company will make more than £$currentVatThreshold in VAT-taxable sales?"

    "return 200 with HTML not prepopulated when there is no view data" in new Setup {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold))

      callAuthenticated(testController.show) { res =>
        res includesText expectedText
        res passJsoupTest { doc =>
          doc.getElementById("taxableTurnoverRadio-true").attr("checked") mustBe ""
          doc.getElementById("taxableTurnoverRadio-false").attr("checked") mustBe ""
        }
      }
    }

    "return 200 with HTML prepopulated to YES when there is view data" in new Setup {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold.copy(taxableTurnover = Some(true))))

      callAuthenticated(testController.show) {
        _ passJsoupTest { doc =>
          doc.getElementById("taxableTurnoverRadio-true").attr("checked") mustBe "checked"
          doc.getElementById("taxableTurnoverRadio-false").attr("checked") mustBe ""
        }
      }
    }

    "return 200 with HTML prepopulated to NO when there is view data" in new Setup {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockGetThreshold(Future.successful(emptyThreshold.copy(taxableTurnover = Some(false))))

      callAuthenticated(testController.show) {
        _ passJsoupTest { doc =>
          doc.getElementById("taxableTurnoverRadio-true").attr("checked") mustBe ""
          doc.getElementById("taxableTurnoverRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }

  s"POST ${routes.TaxableTurnoverController.submit()} with Empty data" should {
    "return 400" in new Setup {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
      ))(result => result isA 400)
    }
  }

  s"POST ${routes.TaxableTurnoverController.submit()} with Taxable Turnover selected Yes" should {

    "return 303" in new Setup {
      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockSaveTaxableTurnover(Future.successful(validThresholdPreIncorp.copy(taxableTurnover = Some(true))))
      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry)
        .thenReturn("someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "taxableTurnoverRadio" -> "true"
      ))(_ redirectsTo "someUrl")

    }
  }

  s"POST ${routes.TaxableTurnoverController.submit()} with Taxable Turnover selected No" should {
    "return 303" in new Setup {

      mockFetchCurrentVatThreshold(Future.successful(currentVatThreshold))
      mockSaveTaxableTurnover(Future.successful(validThresholdPreIncorp))

      mockGetThreshold(Future.successful(emptyThreshold.copy(taxableTurnover = Some(false))))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "taxableTurnoverRadio" -> "false"
      ))(_ redirectsTo routes.VoluntaryRegistrationController.show.url)
    }
  }

}
