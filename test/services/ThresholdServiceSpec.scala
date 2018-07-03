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

import base.{CommonSpecBase, VATEligiblityMocks}
import connectors.DataCacheConnector
import identifiers.VoluntaryRegistrationId
import models.CurrentProfile
import models.requests.DataRequest
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json.JsBoolean
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.UserAnswers

import scala.concurrent.Future
class ThresholdServiceSpec extends CommonSpecBase with VATEligiblityMocks {

  val mockCache = CacheMap("testInternalId", Map(VoluntaryRegistrationId.toString -> JsBoolean(true)))
  implicit val dr = DataRequest(FakeRequest(), "testInternalId", CurrentProfile("testRegId", "testTxId", Some(LocalDate.now().minusYears(2))), new UserAnswers(mockCache))

  class Setup {
    val service = new ThresholdService {
      override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    }
  }
  "removeVoluntaryRegistration" should {
    "call dataCacheConnector and remove VoluntaryRegistrationId when boolean is true" in new Setup {
      when(mockDataCacheConnector.remove(any(),any())) thenReturn Future.successful(true)
      await(service.removeVoluntaryRegistration(true)) mustBe true
      verify(mockDataCacheConnector, times(1)).remove(any(),any())
    }
    "don't call dataCacheConnector and return false when false is passed in" in new Setup {
      await(service.removeVoluntaryRegistration(false)) mustBe false
      verify(mockDataCacheConnector, times(0)).remove(any(),any())
    }
    "throw exception if dataCache throws an exception" in new Setup {
      when(mockDataCacheConnector.remove(any(),any())) thenReturn Future.failed(new Exception("foo bar"))
      intercept[Exception](await(service.removeVoluntaryRegistration(true)))
      verify(mockDataCacheConnector, times(1)).remove(any(),any())
    }
  }
}