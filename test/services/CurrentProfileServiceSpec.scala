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

import common.enums.VatRegStatus
import connectors.BusinessRegistrationConnector
import fixtures.VatRegistrationFixture
import helpers.FutureAssertions
import mocks.VatMocks
import models.CurrentProfile
import models.external.{BusinessProfile, CompanyRegistrationProfile}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Format
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}

import scala.concurrent.Future

class CurrentProfileServiceSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAwaits with DefaultAwaitTimeout
                                with FutureAssertions with VatRegistrationFixture {
  val mockBusinessRegistrationConnector = mock[BusinessRegistrationConnector]

  val testService = new CurrentProfileService {
    override val keystoreConnector = mockKeystoreConnector
    override val businessRegistrationConnector = mockBusinessRegistrationConnector
    override val compRegConnector = mockCompanyRegConnector
    override val incorpInfoService = mockIncorpInfoService
    override val vatRegistrationService = mockVatRegistrationService
  }

  val now = LocalDate.now()

  val testCompanyName = "testCompanyName"
  val regId = "12345"
  val txId = "000-12345"

  def testCurrentProfile(incorpDate: Option[LocalDate] = Some(now)) = CurrentProfile(
    companyName           = testCompanyName,
    registrationId        = regId,
    transactionId         = txId,
    vatRegistrationStatus = VatRegStatus.draft,
    incorporationDate     = incorpDate
  )

  "getCurrentProfile" should {
    implicit val hc = HeaderCarrier()

    "return a CurrentProfile" when {
      "fetched from Keystore" in {
        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](any())(any[HeaderCarrier](), any[Format[CurrentProfile]]()))
          .thenReturn(Future.successful(Some(testCurrentProfile())))

        val result = await(testService.getCurrentProfile())
        result mustBe testCurrentProfile()
      }

      "build and store in Keystore" in {
        val businessProfile = BusinessProfile(regId, "EN")
        val compRegDetails = CompanyRegistrationProfile("accepted", txId)

        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](any())(any[HeaderCarrier](), any[Format[CurrentProfile]]()))
          .thenReturn(Future.successful(None))

        when(mockBusinessRegistrationConnector.retrieveBusinessProfile(any[HeaderCarrier](), any[HttpReads[BusinessProfile]]()))
          .thenReturn(Future.successful(businessProfile))

        when(mockCompanyRegConnector.getCompanyRegistrationDetails(any())(any[HeaderCarrier]()))
          .thenReturn(Future.successful(compRegDetails))

        when(mockIncorpInfoService.getCompanyName(any(), any())(any[HeaderCarrier]()))
          .thenReturn(Future.successful(testCompanyName))

        when(mockVatRegistrationService.getStatus(any())(any[HeaderCarrier]()))
          .thenReturn(Future.successful(VatRegStatus.draft))

        when(mockIncorpInfoService.getIncorpDate(any(), any())(any[HeaderCarrier]()))
          .thenReturn(Future.successful(testIncorporationInfo.statusEvent.incorporationDate))

        when(mockKeystoreConnector.cache[CurrentProfile](any(), any())(any(), any()))
          .thenReturn(Future.successful(CacheMap("", Map())))

        val result = await(testService.getCurrentProfile())
        result mustBe testCurrentProfile(testIncorporationInfo.statusEvent.incorporationDate)
      }
    }
  }
}
