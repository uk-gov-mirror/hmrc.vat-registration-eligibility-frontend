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

import config.VatShortLivedCache
import helpers.FutureAssertions
import models.view.TaxableTurnover
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class S4LConnectorSpec extends UnitSpec with MockitoSugar with FutureAssertions {

  val mockShortLivedCache = mock[VatShortLivedCache]

  object S4LConnectorTest extends S4LConnector(mockShortLivedCache)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val taxableTurnoverModel = TaxableTurnover(TaxableTurnover.TAXABLE_NO)
  val cacheMap = CacheMap("", Map("" -> Json.toJson(taxableTurnoverModel)))

  "Fetching from save4later" should {
    "return the correct model" in {

      when(mockShortLivedCache.fetchAndGetEntry[TaxableTurnover](Matchers.anyString(), Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(taxableTurnoverModel)))

      S4LConnectorTest.fetchAndGet[TaxableTurnover]("", "") returnsSome taxableTurnoverModel
    }
  }

  "Saving a model into save4later" should {
    "save the model" in {
      val returnCacheMap = CacheMap("", Map("" -> Json.toJson(taxableTurnoverModel)))

      when(mockShortLivedCache.cache[TaxableTurnover](Matchers.anyString(), Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnCacheMap))

      val result = S4LConnectorTest.save[TaxableTurnover]("", "", taxableTurnoverModel)
      await(result) shouldBe returnCacheMap
    }
  }

  "clearing an entry using save4later" should {
    "clear the entry given the user id" in {
      when(mockShortLivedCache.remove(Matchers.anyString())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = S4LConnectorTest.clear("test")
      await(result).status shouldBe HttpResponse(OK).status
    }
  }

  "fetchAll" should {
    "fetch all entries in S4L" in {
      when(mockShortLivedCache.fetch(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      val result = S4LConnectorTest.fetchAll("testUserId")
      await(result) shouldBe Some(cacheMap)
    }
  }
}
