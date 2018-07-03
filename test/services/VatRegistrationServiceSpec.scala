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

import base.{CommonSpecBase, MockMessages, VATEligiblityMocks}
import connectors.{DataCacheConnector, VatRegistrationConnector}
import models.{CurrentProfile, Name, Officer}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatRegistrationServiceSpec extends CommonSpecBase with VATEligiblityMocks with MockMessages {

  class Setup {
    val service = new VatRegistrationService {
      override val vrConnector: VatRegistrationConnector = mockVatRegConnector
      override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
      override val messagesApi: MessagesApi = mockMessagesAPI
      override val iiService: IncorporationInformationService = mockIIService
    }

    mockAllMessages
  }

  val internalId = "internalID"
  val regId = "regId"
  val txId = "txId"

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
  }
}
