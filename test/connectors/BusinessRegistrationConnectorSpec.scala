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

package connectors

import helpers.VatRegSpec
import models.external.BusinessProfile
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

class BusinessRegistrationConnectorSpec extends VatRegSpec {

  val mockBusRegConnector = mock[BusinessRegistrationConnector]

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

      await(connector.retrieveBusinessProfile) shouldBe validBusinessRegistrationResponse
    }

    "return a Not Found response when a CurrentProfile record can not be found" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](Matchers.contains("/business-registration/business-tax-registration"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new NotFoundException("not found")))

      intercept[NotFoundException](await(connector.retrieveBusinessProfile))
    }

    "return a Bad Request response when a bad request is send while getting CurrentProfile " in new Setup {
      when(mockWSHttp.GET[BusinessProfile](Matchers.contains("/business-registration/business-tax-registration"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("Bad request")))

      intercept[BadRequestException](await(connector.retrieveBusinessProfile))
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](Matchers.contains("/business-registration/business-tax-registration"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("Forbidden", 403, 403)))

      intercept[Upstream4xxResponse](await(connector.retrieveBusinessProfile))
    }

    "return a 4xx response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](Matchers.contains("/business-registration/business-tax-registration"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("Forbidden", 405, 405)))

      intercept[Upstream4xxResponse](await(connector.retrieveBusinessProfile))
    }

    "return a 5xx response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](Matchers.contains("/business-registration/business-tax-registration"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.failed(new Upstream5xxResponse("Forbidden", 500, 500)))

      intercept[Upstream5xxResponse](await(connector.retrieveBusinessProfile))
      }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](Matchers.contains("/business-registration/business-tax-registration"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Runtime Exception")))

      intercept[RuntimeException](await(connector.retrieveBusinessProfile))
    }
  }
}
