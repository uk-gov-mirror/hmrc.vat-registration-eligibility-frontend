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

package connectors

import mocks.VatMocks
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.Future

class BusinessRegistrationConnectorSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAwaits with DefaultAwaitTimeout {
  implicit val hc = HeaderCarrier()

  trait Setup {
    val connector = new BusinessRegistrationConnector {
      override val businessRegUrl = "testBusinessRegUrl"
      override val http = mockWSHttp
    }
  }

  val validBusinessRegistrationResponse = BusinessProfile(
    "12345",
    "ENG"
  )

  "retrieveCurrentProfile" should {
    "return a a CurrentProfile response if one is found in business registration micro-service" in new Setup {
      mockHttpGET[BusinessProfile]("testUrl", validBusinessRegistrationResponse)

      await(connector.retrieveBusinessProfile) mustBe validBusinessRegistrationResponse
    }

    "return a Not Found response when a CurrentProfile record can not be found" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("not found")))

      intercept[NotFoundException](await(connector.retrieveBusinessProfile))
    }

    "return a Bad Request response when a bad request is send while getting CurrentProfile " in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new BadRequestException("Bad request")))

      intercept[BadRequestException](await(connector.retrieveBusinessProfile))
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("Forbidden", 403, 403)))

      intercept[Upstream4xxResponse](await(connector.retrieveBusinessProfile))
    }

    "return a 4xx response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("Forbidden", 405, 405)))

      intercept[Upstream4xxResponse](await(connector.retrieveBusinessProfile))
    }

    "return a 5xx response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(new Upstream5xxResponse("Forbidden", 500, 500)))

      intercept[Upstream5xxResponse](await(connector.retrieveBusinessProfile))
      }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Runtime Exception")))

      intercept[RuntimeException](await(connector.retrieveBusinessProfile))
    }
  }
}
