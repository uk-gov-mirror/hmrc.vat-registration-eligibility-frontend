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

package services

import java.time.LocalDate

import common.enums.VatRegStatus
import helpers.FutureAssertions
import mocks.VatMocks
import models.CurrentProfile
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

class S4LServiceSpec extends UnitSpec with MockitoSugar with VatMocks with FutureAssertions {
  val service = new S4LService(mockS4LConnector)
  val cacheMap = CacheMap("s-date", Map.empty)

  val testRegId = "id"
  val cacheKey = "test"
  val cacheData = "data"

  implicit val hc = HeaderCarrier()
  implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1",
    VatRegStatus.draft,Some(LocalDate.of(2016, 12, 21)))

  "S4L Service" should {
    "save a form with the correct key" in {
      mockKeystoreFetchAndGet[String]("RegistrationId", Some(testRegId))
      mockS4LSaveForm[String](cacheMap)
      service.save(cacheKey, cacheData) returns cacheMap
    }

    "fetch a form with the correct key" in {
      mockKeystoreFetchAndGet[String]("RegistrationId", Some(testRegId))
      mockS4LFetchAndGet("test", Some(cacheData))
      service.fetchAndGet[String](cacheKey) returns Some(cacheData)
    }

    "clear down S4L data" in {
      mockKeystoreFetchAndGet[String]("RegistrationId", Some(testRegId))
      mockS4LClear()
      service.clear().map(_.status) returns 200
    }

    "fetch all data" in {
      mockKeystoreFetchAndGet[String]("RegistrationId", Some(testRegId))
      mockS4LFetchAll(Some(cacheMap))
      service.fetchAll() returns Some(cacheMap)
    }
  }
}
