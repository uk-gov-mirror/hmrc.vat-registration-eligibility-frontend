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

import common.enums.{CacheKeys, VatRegStatus}
import fixtures.VatRegistrationFixture
import helpers.FutureAssertions
import mocks.VatMocks
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.InternalExceptions.LockedStatus

import scala.concurrent.Future
import scala.language.postfixOps

class VatRegistrationServiceSpec extends PlaySpec with BeforeAndAfter with MockitoSugar with VatMocks with FutureAssertions with VatRegistrationFixture {
  val incorpDate = LocalDate.of(2016, 12, 21)
  implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1", VatRegStatus.draft, Some(incorpDate))

  implicit val hc = HeaderCarrier()

  class Setup {
    val service = new VatRegistrationService {
      override val vatRegConnector = mockRegConnector
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  def beforeEach() {
    resetMocks()
    mockFetchRegId(testRegId)
    when(mockRegConnector.getIncorporationInfo(any(),any())(any()))
      .thenReturn(Future.successful(None))
  }

  "Calling getIncorporationInfo" should {
    "successfully returns an incorporation information" in new Setup {
      when(mockRegConnector.getIncorporationInfo(any(),any())(any()))
        .thenReturn(Future.successful(Some(testIncorporationInfo)))

      service.getIncorporationInfo(testRegId,"txId") returns Some(testIncorporationInfo)
    }
  }

  "Calling getIncorporationDate" should {
    "successfully returns an incorporation date from keystore" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(currentProfile))

      service.getIncorporationDate returns currentProfile.incorporationDate
    }

    "successfully returns an incorporation date from microservice and save to keystore" in new Setup {
      when(mockRegConnector.getIncorporationInfo(any(),any())(any()))
        .thenReturn(Future.successful(Some(testIncorporationInfo)))

      mockKeystoreCache[String](CacheKeys.CurrentProfile.toString, CacheMap("", Map.empty))

      service.getIncorporationDate(currentProfile.copy(incorporationDate = None), hc) returns testIncorporationInfo.statusEvent.incorporationDate
    }
  }

  "Calling getStatus" should {
    "successfully returns a Registration Status" in new Setup {
      when(mockRegConnector.getStatus(any())(any()))
        .thenReturn(Future(VatRegStatus.draft))

      service.getStatus("regId") returns VatRegStatus.draft
    }

    "return a LockedStatus exception if the status is locked" in new Setup {
      when(mockRegConnector.getStatus(any())(any())) thenReturn Future.successful(VatRegStatus.locked)

      intercept[LockedStatus](await(service.getStatus("regId")))
    }

    "return an Exception if fail to get the status" in new Setup {
      when(mockRegConnector.getStatus(any())(any()))
        .thenReturn(Future(throw new BadRequestException("test")))

      service.getStatus("regId") failedWith classOf[BadRequestException]
    }
  }
}
