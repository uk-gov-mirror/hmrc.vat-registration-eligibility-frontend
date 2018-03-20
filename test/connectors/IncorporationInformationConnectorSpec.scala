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

import config.WSHttp
import mocks.VatMocks
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class IncorporationInformationConnectorSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAwaits with DefaultAwaitTimeout {
  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new IncorporationInformationConnector {

      override val incorpInfoUrl = "testIIStubUrl"
      override val incorpInfoUri = "testIIUri"
      override val http : WSHttp = mockWSHttp
    }
  }

  val validCoHoCompanyDetailsResponse = Json.parse(
    s"""{
       |  "company_name": "testCompany"
       |}""".stripMargin)

  "getCompanyName" should {
    "return a successful CoHo api response object for valid data" in new Setup {
      mockHttpGET[JsValue](connector.incorpInfoUrl, Future.successful(validCoHoCompanyDetailsResponse))

      await(connector.getCompanyName("testRegID", "testTxID")) mustBe validCoHoCompanyDetailsResponse
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup {
      mockHttpGET[JsValue](connector.incorpInfoUrl, Future.failed(new NotFoundException("tstException")))

      a[NotFoundException] mustBe thrownBy(await(connector.getCompanyName("testRegID", "testTxID")))
    }

    "return a CoHo error api response object for a downstream error" in new Setup {
      val ex = new Exception("tstException")
      mockHttpGET[JsValue](connector.incorpInfoUrl, Future.failed(ex))

      an[Exception] mustBe thrownBy(await(connector.getCompanyName("testRegID", "testTxID")))
    }
  }


}
