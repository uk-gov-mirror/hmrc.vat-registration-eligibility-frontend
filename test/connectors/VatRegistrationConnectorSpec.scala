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

import common.enums.VatRegStatus
import config.WSHttp
import fixtures.VatRegistrationFixture
import helpers.FutureAssertions
import mocks.VatMocks
import models.CurrentProfile
import models.external.IncorporationInfo
import models.view.VoluntaryRegistrationReason.SELLS
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}

class VatRegistrationConnectorSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAwaits with DefaultAwaitTimeout
                                   with VatRegistrationFixture with FutureAssertions {
  class Setup {
    val connector = new VatRegistrationConnector {
      override val vatRegUrl: String = "tst-url"
      override val http: WSHttp = mockWSHttp
      override lazy val config = mockAppConfig
    }
    when(mockAppConfig.whitelistedRegIds).thenReturn(Seq("foo"))
  }

  val incorpDate = LocalDate.of(2016, 12, 21)
  implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1", VatRegStatus.draft, Some(incorpDate))

  implicit val hc = HeaderCarrier()

  val forbidden = Upstream4xxResponse(FORBIDDEN.toString, FORBIDDEN, FORBIDDEN)
  val internalServerError = Upstream5xxResponse(INTERNAL_SERVER_ERROR.toString, INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
  val notFound = new NotFoundException(NOT_FOUND.toString)
  val exception = new Exception(BAD_GATEWAY.toString)

  "Calling getIncorporationInfo" should {

    "return a IncorporationInfo when it can be retrieved from the microservice" in new Setup {
      mockHttpGET[IncorporationInfo]("tst-url", testIncorporationInfo)
      await(connector.getIncorporationInfo(testRegId,"tstID")) mustBe Some(testIncorporationInfo)
    }

    "fail when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[IncorporationInfo]("test-url", notFound)
      await(connector.getIncorporationInfo(testRegId,"tstID")) mustBe None
    }
  }

  "Calling getStatus" should {
    "return a valid status" in new Setup {
      mockHttpGET[JsObject]("tst-url", Json.obj("status" -> VatRegStatus.draft))
      await(connector.getStatus("testID")) mustBe VatRegStatus.draft
    }

    "fail when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[JsObject]("test-url", notFound)
      an[Exception] mustBe thrownBy(await(connector.getStatus("testID")))
    }

    "return an exception when the status is not valid" in new Setup {
      mockHttpGET[JsObject]("tst-url", Json.obj("status" -> "wrongStatus"))
      an[Exception] mustBe thrownBy(await(connector.getStatus("testID")))
    }
  }

  "Calling getEligibility" should {
    val validJson = Json.parse(
      s"""
         |{
         |  "version": 1,
         |  "result": "success"
         |}
       """.stripMargin)
    val httpRespOK = HttpResponse(OK, Some(validJson))
    val httpRespNOCONTENT = HttpResponse(NO_CONTENT, None)

    "return a tuple2 (String, Int)" in new Setup {
      mockHttpGET[HttpResponse]("tst-url", httpRespOK)
      connector.getEligibility returns Some(("success", 1))
    }

    "return None if there is no data for the registration" in new Setup {
      mockHttpGET[HttpResponse]("tst-url", httpRespNOCONTENT)
      connector.getEligibility returns None
    }

    "throw a NotFoundException if the registration does not exist" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", notFound)
      connector.getEligibility failedWith notFound
    }

    "throw an Upstream4xxResponse with Forbidden status" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", forbidden)
      connector.getEligibility failedWith forbidden
    }

    "throw an Upstream5xxResponse with Internal Server Error status" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", internalServerError)
      connector.getEligibility failedWith internalServerError
    }

    "throw an Exception if the call failed" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", exception)
      connector.getEligibility failedWith exception
    }
  }

  "Calling patchEligibility" should {
    val validJson = Json.parse(
      s"""
         |{
         |  "version": 1,
         |  "result": "success"
         |}
       """.stripMargin)

    "return a JsValue" in new Setup {
      mockHttpPATCH[JsValue, JsValue]("tst-url", validJson)
      connector.patchEligibility("success", 1) returns validJson
    }

    "throw a NotFoundException if the registration does not exist" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", notFound)
      connector.patchEligibility("success", 1) failedWith notFound
    }

    "throw an Upstream4xxResponse with Forbidden status" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", forbidden)
      connector.patchEligibility("success", 1) failedWith forbidden
    }

    "throw an Upstream5xxResponse with Internal Server Error status" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", internalServerError)
      connector.patchEligibility("success", 1) failedWith internalServerError
    }

    "throw an Exception if the call failed" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", exception)
      connector.patchEligibility("success", 1) failedWith exception
    }
  }

  "Calling getThreshold" should {
    val validJson = Json.parse(
      s"""
         |{
         |  "mandatoryRegistration": true
         |}
       """.stripMargin)
    val httpRespOK = HttpResponse(OK, Some(validJson))
    val httpRespNOCONTENT = HttpResponse(NO_CONTENT, None)

    "return a JsValue" in new Setup {
      mockHttpGET[HttpResponse]("tst-url", httpRespOK)
      connector.getThreshold returns Some(validJson)
    }

    "return None if there is no data for the registration" in new Setup {
      mockHttpGET[HttpResponse]("tst-url", httpRespNOCONTENT)
      connector.getThreshold returns None
    }

    "throw a NotFoundException if the registration does not exist" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", notFound)
      connector.getThreshold failedWith notFound
    }

    "throw an Upstream4xxResponse with Forbidden status" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", forbidden)
      connector.getThreshold failedWith forbidden
    }

    "throw an Upstream5xxResponse with Internal Server Error status" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", internalServerError)
      connector.getThreshold failedWith internalServerError
    }

    "throw an Exception if the call failed" in new Setup {
      mockHttpFailedGET[HttpResponse]("tst-url", exception)
      connector.getThreshold failedWith exception
    }
  }

  "Calling patchThreshold" should {
    val validJson = Json.parse(
      s"""
         |{
         |  "mandatoryRegistration": false,
         |  "voluntaryReason": "$SELLS"
         |}
       """.stripMargin)

    "return a JsValue" in new Setup {
      mockHttpPATCH[JsValue, JsValue]("tst-url", validJson)
      connector.patchThreshold(validThresholdPreIncorp) returns validJson
    }

    "throw a NotFoundException if the registration does not exist" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", notFound)
      connector.patchThreshold(validThresholdPreIncorp) failedWith notFound
    }

    "throw an Upstream4xxResponse with Forbidden status" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", forbidden)
      connector.patchThreshold(validThresholdPreIncorp) failedWith forbidden
    }

    "throw an Upstream5xxResponse with Internal Server Error status" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", internalServerError)
      connector.patchThreshold(validThresholdPreIncorp) failedWith internalServerError
    }

    "throw an Exception if the call failed" in new Setup {
      mockHttpFailedPATCH[JsValue, JsValue]("tst-url", exception)
      connector.patchThreshold(validThresholdPreIncorp) failedWith exception
    }
  }
}
