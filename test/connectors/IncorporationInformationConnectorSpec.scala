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

import java.time.LocalDate

import base.ConnectorSpecBase
import config.WSHttp
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{CoreGet, InternalServerException, NotFoundException}
import utils.{BooleanFeatureSwitch, VATFeatureSwitch}

class IncorporationInformationConnectorSpec extends ConnectorSpecBase {

  val txId      = "someTxId"
  val companyName = "MyTestCompany"

  class Setup {
    val fakeUrl = "testUrl"
    val stubFakeUrl = "stubUrl"
    val connector = new IncorporationInformationConnector {
      override val http: CoreGet with WSHttp = mockWSHttp
      override val incorpInfoUrl: String = fakeUrl
      override val incorpInfoUri: String = ""
      override val stubUrl: String = stubFakeUrl
      override val featureSwitch: VATFeatureSwitch = mockVATFeatureSwitch
    }

    val fetchIncorpDataUrl = s"$fakeUrl/$txId/incorporation-update"
    def fetchOfficerUrl(url: String = fakeUrl, uri: String = "") = s"$url$uri/$txId/officer-list"
    val fetchCohoDetailsUrl = s"$fakeUrl/$txId/company-profile"
  }

  val validIncorpData = Json.parse(
    s"""
      |{
      | "transaction_id" : "$txId",
      | "status" : "accepted",
      | "crn" : "OC12374C",
      | "incorporationDate" : "${LocalDate.now}",
      | "timepoint" : "12334545553"
      |}
    """.stripMargin)

  val validCOHODetails = Json.parse(
    s"""
          |{
          |  "company_name":"$companyName",
          |  "registered_office_address":{
          |    "premises":"1",
          |    "address_line_1":"test street",
          |    "locality":"Testford",
          |    "country":"UK",
          |    "postal_code":"TE2 2ST"
          |  }
          |}
        """.stripMargin)

  val tstOfficerListJson = Json.parse(
    """
      |{
      |  "officers": [
      |    {
      |      "name" : "test",
      |      "name_elements" : {
      |        "forename" : "test1",
      |        "other_forenames" : "test11",
      |        "surname" : "testa",
      |        "title" : "Mr"
      |      },
      |      "officer_role" : "cic-manager"
      |    }, {
      |      "name" : "test",
      |      "name_elements" : {
      |        "forename" : "test2",
      |        "other_forenames" : "test22",
      |        "surname" : "testb",
      |        "title" : "Mr"
      |      },
      |      "officer_role" : "corporate-director"
      |    }
      |  ]
      |}""".stripMargin)

  "getIncorpData" should {
    "return some data" in new Setup {
      mockGet(fetchIncorpDataUrl, OK, Some(validIncorpData))
      await(connector.getIncorpData(txId)) mustBe Some(validIncorpData)
      verifyGetCalled(fetchIncorpDataUrl)
    }

    "return none if not data present" in new Setup {
      mockGet(fetchIncorpDataUrl, NO_CONTENT, Some(validIncorpData))
      await(connector.getIncorpData(txId)) mustBe None
      verifyGetCalled(fetchIncorpDataUrl)
    }

    "throw and exception if occurs" in new Setup {
      mockFailedGet(fetchIncorpDataUrl, new InternalServerException("internal server error"))
      intercept[InternalServerException](await(connector.getIncorpData(txId)))
      verifyGetCalled(fetchIncorpDataUrl)
    }
  }

  "getOfficerList" should {
    "return some data" in new Setup {
      when(mockVATFeatureSwitch.useIiStubbed).thenReturn(BooleanFeatureSwitch("test", false))
      mockGet(fetchOfficerUrl(), OK, Some(tstOfficerListJson))
      await(connector.getOfficerList(txId)) mustBe tstOfficerListJson
      verifyGetCalled(fetchOfficerUrl())
    }

    "return some data using the stubbed url if stub is active" in new Setup {
      when(mockVATFeatureSwitch.useIiStubbed).thenReturn(BooleanFeatureSwitch("test", true))
      mockGet(fetchOfficerUrl(stubFakeUrl, "/incorporation-frontend-stubs"), OK, Some(tstOfficerListJson))
      await(connector.getOfficerList(txId)) mustBe tstOfficerListJson
      verifyGetCalled(fetchOfficerUrl(stubFakeUrl, "/incorporation-frontend-stubs"))
    }

    "throw exception if not data return form ii" in new Setup {
      when(mockVATFeatureSwitch.useIiStubbed).thenReturn(BooleanFeatureSwitch("test", false))
      mockFailedGet(fetchOfficerUrl(), new NotFoundException("internal server error"))
      intercept[NotFoundException](await(connector.getOfficerList(txId)))
      verifyGetCalled(fetchOfficerUrl())
    }

    "throw and exception if occurs" in new Setup {
      when(mockVATFeatureSwitch.useIiStubbed).thenReturn(BooleanFeatureSwitch("test", false))
      mockFailedGet(fetchOfficerUrl(), new InternalServerException("internal server error"))
      intercept[InternalServerException](await(connector.getOfficerList(txId)))
      verifyGetCalled(fetchOfficerUrl())
    }
  }

  "getCompanyDetails" should {
    "return some data" in new Setup {
      mockGet(fetchCohoDetailsUrl, OK, Some(validCOHODetails))
      await(connector.getCOHOCompanyDetails(txId)) mustBe validCOHODetails
      verifyGetCalled(fetchCohoDetailsUrl)
    }

    "throw exception if not data return form ii" in new Setup {
      mockFailedGet(fetchCohoDetailsUrl, new NotFoundException("not found error"))
      intercept[NotFoundException](await(connector.getCOHOCompanyDetails(txId)))
      verifyGetCalled(fetchCohoDetailsUrl)
    }

    "throw and exception if occurs" in new Setup {
      mockFailedGet(fetchCohoDetailsUrl, new InternalServerException("internal server error"))
      intercept[InternalServerException](await(connector.getCOHOCompanyDetails(txId)))
      verifyGetCalled(fetchCohoDetailsUrl)
    }
  }
}
