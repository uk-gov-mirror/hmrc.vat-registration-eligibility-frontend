/*
 * Copyright 2019 HM Revenue & Customs
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

import base.{CommonSpecBase, MockMessages, SpecBase, VATEligiblityMocks}
import connectors.{DataCacheConnector, VatRegistrationConnector}
import identifiers._
import models.{Name, Officer}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.JsonSummaryRow

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatRegistrationServiceSpec extends SpecBase with VATEligiblityMocks  {

  class Setup {
    val service = new VatRegistrationService {
      override val vrConnector: VatRegistrationConnector = mockVatRegConnector
      override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
      override val messagesApi: MessagesApi = mockMessagesAPI
      override val iiService: IncorporationInformationService = mockIIService
      override val thresholdService: ThresholdService = mockThresholdService
    }
    def mockAllMessages: OngoingStubbing[String] = {
      when(mockMessagesAPI.preferred(Matchers.any[RequestHeader]()))
        .thenReturn(messages)

      when(mockMessagesAPI.apply(Matchers.any[String](), Matchers.any())(Matchers.any()))
        .thenReturn("mocked message")
    }
    mockAllMessages
    when(mockThresholdService.returnThresholdDateResult[String](any())(any())).thenReturn("foo")
  }

  implicit val r = fakeDataRequestIncorped

  val officersList: Seq[Officer] = Seq(
    Officer(Name(Some("First"), Some("Middle"), "Last",Some("Mrs")),"director", None, Some("some-url")),
    Officer(Name(Some("Second"), None, "VeryLast",Some("Mr")), "secretary", None, Some("some-url"))
  )
  val internalId = "internalID"

  "prepareQuestionData" should {
    "prepare simple boolean data" in new Setup {
      val key = "thresholdNextThirtyDays"

      service.prepareQuestionData(key, false) mustBe
        List(Json.obj(
          "questionId"  -> key,
          "question"    -> "mocked message",
          "answer"      -> "mocked message",
          "answerValue" -> false
        ))
    }

    "prepare simple string data" in new Setup {
      val key = "completionCapacity"

      service.prepareQuestionData(key, "officer") mustBe
        List(Json.obj(
          "questionId"  -> key,
          "question"    -> "mocked message",
          "answer"      -> "officer",
          "answerValue" -> "officer"
        ))
    }
    "return valid JsonSummaryRow for NOT acting on behalf for ID InvolvedInOtherBusinessId" in new Setup {
      val res = service.prepareQuestionData("involvedInOtherBusiness",true, officersList, None)
      res mustBe JsonSummaryRow("involvedInOtherBusiness", "mocked message","mocked message", JsBoolean(true))
    }
    "return valid JsonSummaryRow for ACTING on behalf for ID InvolvedInOtherBusinessId" in new Setup {
      val res = service.prepareQuestionData("involvedInOtherBusiness",false, officersList,Some(officersList.head.generateId))
      res mustBe JsonSummaryRow("involvedInOtherBusiness", "mocked message","mocked message", JsBoolean(false))
    }
  }
  ""
  "submitEligibility" should {
    val fullListMapHappyPathTwelveMonthsFalse:ListMap[String,JsValue] = ListMap[String, JsValue](
      ""                             -> JsString(""),
      s"$ThresholdInTwelveMonthsId"         -> Json.obj("value" -> JsBoolean(false)),
      s"$ThresholdNextThirtyDaysId"         -> JsBoolean(false),
      s"$ThresholdPreviousThirtyDaysId"     -> Json.obj("value" -> JsBoolean(false)),
      s"$VoluntaryRegistrationId"           -> JsBoolean(true),
      s"$TurnoverEstimateId"                -> Json.obj("selection" -> JsString("oneandtenthousand")),
      s"$CompletionCapacityId"              -> JsString("noneOfThese"),
      s"$CompletionCapacityFillingInForId"  -> JsString("Mrwellfoo"),
      s"$InternationalActivitiesId"         -> JsBoolean(false),
      s"$InvolvedInOtherBusinessId"         -> JsBoolean(false),
      s"$AnnualAccountingSchemeId"          -> JsBoolean(false),
      s"$VoluntaryRegistrationId"           -> JsBoolean(true),
      s"$VATExemptionId"                    -> JsBoolean(false),
      s"$ZeroRatedSalesId"                  -> JsBoolean(true),
      s"$AgriculturalFlatRateSchemeId"      -> JsBoolean(false),
      s"$RacehorsesId"                      -> JsBoolean(false),
      s"$ApplicantUKNinoId"                 -> Json.obj(
        "value" -> JsBoolean(true),
        "optionalData" -> JsString("nino-fake-not-real")
      )
    )
    "return the JsObject submitted to Vat registration" in new Setup {
      when(mockDataCacheConnector.fetch(any())).thenReturn(Future.successful(Some(new CacheMap("foo",fullListMapHappyPathTwelveMonthsFalse))))
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future(Seq(Officer(Name(None,None,"well",Some("Mr")),"foo",None,None))))
      when(mockVatRegConnector.saveEligibility(any(),any())(any(),any())).thenReturn(Future.successful(Json.obj("wizz" -> "woo")))

      await(service.submitEligibility("foo")) mustBe Json.parse("""
          |{"sections":[
          |{"title":"VAT-taxable sales",
          | "data":[{"questionId":"thresholdInTwelveMonths-value","question":"foo","answer":"mocked message","answerValue":false}, {"questionId":"thresholdNextThirtyDays","question":"mocked message","answer":"mocked message","answerValue":false},
          | {"questionId":"thresholdPreviousThirtyDays-value","question":"foo","answer":"mocked message","answerValue":false},{"questionId":"voluntaryRegistration","question":"mocked message","answer":"mocked message","answerValue":true},
          | {"questionId":"turnoverEstimate-value","question":"mocked message","answer":"mocked message","answerValue":"oneandtenthousand"}]},
          | {"title":"Who is doing the application?",
          | "data":[{"questionId":"completionCapacity","question":"mocked message","answer":"mocked message","answerValue":"noneOfThese"},
          | {"questionId":"completionCapacityFillingInFor","question":"mocked message","answer":"well","answerValue":{"name":{"surname":"well","title":"Mr"},"role":"foo"}}]},
          | {"title":"Special situations",
          | "data":[{"questionId":"internationalActivities","question":"mocked message","answer":"mocked message","answerValue":false},
          | {"questionId":"involvedInOtherBusiness","question":"mocked message","answer":"mocked message","answerValue":false},
          | {"questionId":"annualAccountingScheme","question":"mocked message","answer":"mocked message","answerValue":false},
          | {"questionId":"zeroRatedSales","question":"mocked message","answer":"mocked message","answerValue":true},
          | {"questionId":"vatExemption","question":"mocked message","answer":"mocked message","answerValue":false},
          | {"questionId":"agriculturalFlatRateScheme","question":"mocked message","answer":"mocked message","answerValue":false},
          | {"questionId":"racehorses","question":"mocked message","answer":"mocked message","answerValue":false},
          | {"questionId":"applicantUKNino-value","question":"mocked message","answer":"mocked message","answerValue":true},
          | {"questionId":"applicantUKNino-optionalData","question":"mocked message","answer":"nino-fake-not-real","answerValue":"nino-fake-not-real"}]}]}""".stripMargin)
    }
  }
}