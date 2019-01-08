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

import java.time.LocalDate

import base.CommonSpecBase
import connectors.{BusinessRegistrationConnector, CompanyRegistrationConnector, DataCacheConnector}
import models.CurrentProfile
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class CurrentProfileServiceSpec extends CommonSpecBase {

  class Setup {
    val service = new CurrentProfileService {
      override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
      override val incorporationInformationService: IncorporationInformationService = mockIIService
      override val businessRegistrationConnector: BusinessRegistrationConnector = mockBusRegConnector
      override val companyRegistrationConnector: CompanyRegistrationConnector = mockCompanyRegConnector
    }
  }

  val regID   = "registrationID"
  val txID    = "transactionID"
  val testIntId = "internalId"
  val testCompanyName = "Test Company"

  "buildCurrentProfile" should {
    "build a profile" when {
      "it hasn't been built" in new Setup {
        when(mockDataCacheConnector.getEntry[CurrentProfile](Matchers.any(), Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(None))
        when(mockIIService.getIncorpDate(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(None))
        when(mockIIService.getCompanyName(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(testCompanyName))
        when(mockBusRegConnector.getBusinessRegistrationId(Matchers.any()))
            .thenReturn(Future.successful(regID))
        when(mockCompanyRegConnector.getTransactionId(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(txID))
        when(mockDataCacheConnector.save[CurrentProfile](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(CacheMap("test", Map("test" -> Json.obj()))))

        await(service.fetchOrBuildCurrentProfile(testIntId)) mustBe CurrentProfile(regID, txID, None, testCompanyName)
      }

      "it has been built" in new Setup {
        private val profile = CurrentProfile(regID, txID, Some(LocalDate.now()),testCompanyName)

        when(mockIIService.getIncorpDate(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Some(LocalDate.now())))

        when(mockIIService.getCompanyName(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(testCompanyName))

        when(mockDataCacheConnector.getEntry[CurrentProfile](Matchers.any(), Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(Some(profile)))

        await(service.fetchOrBuildCurrentProfile(testIntId)) mustBe profile
      }
    }
  }
}
