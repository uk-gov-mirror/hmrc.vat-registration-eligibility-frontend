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

package services

import java.time.LocalDate

import connectors.{IncorporationInformationConnector, KeystoreConnector}
import helpers.FutureAssertions
import mocks.VatMocks
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class IncorporationInformationServiceSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAssertions {
  class Setup {
    val service = new IncorporationInformationService {
      override val incorpInfoConnector: IncorporationInformationConnector = mockIIConnector
      override val keystoreConnector: KeystoreConnector = mockKeystoreConnector
    }
  }

  implicit val hc = HeaderCarrier()

  "getCompanyName" should {
    "successfully return a company name" in new Setup {
      when(mockIIConnector.getCompanyName(any(), any())(any())).thenReturn(Future(Json.obj("company_name" -> "TEST NAME")))

      service.getCompanyName("regId", "txId") returns "TEST NAME"
    }

    "return an exception if the company_name field is missing" in new Setup {
      when(mockIIConnector.getCompanyName(any(), any())(any())).thenReturn(Future(Json.obj("foo" -> 12)))

      a[JsResultException] mustBe thrownBy(await(service.getCompanyName("regId", "txId")))
    }
  }

  "getIncorpDate" must {
    "return an incorp date" in new Setup {
      when(mockIIConnector.getIncorpUpdate(any(), any())(any()))
        .thenReturn(Future.successful(Some(
          Json.obj("incorporationDate" -> LocalDate.of(2018, 5, 11))
        )))

      service.getIncorpDate("regId", "txID") returns Some(LocalDate.of(2018, 5, 11))
    }
  }
}
