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

import cats.data.OptionT
import common.enums.CacheKeys
import fixtures.VatRegistrationFixture
import helpers.{S4LMockSugar, VatRegSpec}
import models._
import models.external.IncorporationInfo
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import scala.language.postfixOps

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {
    val service = new VatRegistrationService(mockS4LService, mockKeystoreConnector, mockRegConnector)
  }

  override def beforeEach() {
    super.beforeEach()
    mockFetchRegId(testRegId)
    when(mockRegConnector.getIncorporationInfo(any())(any())).thenReturn(OptionT.none[Future, IncorporationInfo])
  }

  "Calling submitEligibility" should {
    "return a success response when VatEligibility is submitted" in new Setup {
      save4laterReturns(S4LVatEligibility(Some(validServiceEligibility)))

      when(mockRegConnector.getRegistration(Matchers.eq(testRegId))(any(), any())).thenReturn(validVatScheme.pure)
      when(mockRegConnector.upsertVatEligibility(any(), any())(any(), any())).thenReturn(validServiceEligibility.pure)

      service.submitVatEligibility() returns validServiceEligibility
    }
  }

  "Calling deleteVatScheme" should {
    "return a success response when the delete VatScheme is successful" in new Setup {
      mockKeystoreCache[String]("RegistrationId", CacheMap("", Map.empty))
      when(mockRegConnector.deleteVatScheme(any())(any(), any())).thenReturn(().pure)

      service.deleteVatScheme() completedSuccessfully
    }
  }

  "When this is the first time the user starts a journey and we're persisting to the backend" should {
    "submitVatEligibility should process the submission even if VatScheme does not contain a VatEligibility object" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(testRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      when(mockRegConnector.upsertVatEligibility(any(), any())(any(), any())).thenReturn(validServiceEligibility.pure)
      save4laterReturns(S4LVatEligibility(Some(validServiceEligibility)))
      service.submitVatEligibility() returns validServiceEligibility
    }

    "submitVatEligibility should fail if there's not trace of VatEligibility in neither backend nor S4L" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(testRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      save4laterReturnsNothing[S4LVatEligibility]()

      service.submitVatEligibility() failedWith classOf[IllegalStateException]
    }
  }

  "Calling getIncorporationInfo" should {
    "successfully returns an incorporation information" in new Setup {
      when(mockRegConnector.getIncorporationInfo(Matchers.any())(Matchers.any())).thenReturn(OptionT.fromOption(Some(testIncorporationInfo)))

      service.getIncorporationInfo("txId") returns Some(testIncorporationInfo)
    }
  }

  "Calling getIncorporationDate" should {
    "successfully returns an incorporation date from keystore" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(currentProfile))

      service.getIncorporationDate("txId") returns currentProfile.incorporationDate
    }

    "successfully returns an incorporation date from microservice and save to keystore" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(currentProfile.copy(incorporationDate = None)))
      when(mockRegConnector.getIncorporationInfo(Matchers.any())(Matchers.any())).thenReturn(OptionT.fromOption(Some(testIncorporationInfo)))
      mockKeystoreCache[String](CacheKeys.CurrentProfile.toString, CacheMap("", Map.empty))

      service.getIncorporationDate("txId") returns testIncorporationInfo.statusEvent.incorporationDate
    }

    "throw an exception if no Current Profile is returned from keystore" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)
      when(mockRegConnector.getIncorporationInfo(Matchers.any())(Matchers.any())).thenReturn(OptionT.fromOption(Some(testIncorporationInfo)))
      mockKeystoreCache[String](CacheKeys.CurrentProfile.toString, CacheMap("", Map.empty))

      an[IllegalStateException] shouldBe thrownBy(await(service.getIncorporationDate("txId")))
    }
  }
}
