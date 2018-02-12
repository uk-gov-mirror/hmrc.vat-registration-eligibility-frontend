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
import fixtures.VatRegistrationFixture
import mocks.VatMocks
import models.CurrentProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class CancellationServiceSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAwaits with DefaultAwaitTimeout with VatRegistrationFixture {
  val incorpDate = LocalDate.of(2016, 12, 21)
  implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1", VatRegStatus.draft, Some(incorpDate))

  implicit val hc = HeaderCarrier()

  class Setup {
    val service = new CancellationService {
      override val keystoreConnector = mockKeystoreConnector
      override val s4LConnector = mockS4LConnector
      override val currentProfileService = mockCurrentProfileService
    }

    def mockGetCurrentProfile(regId: String) = when(mockCurrentProfileService.getCurrentProfile()(ArgumentMatchers.any()))
      .thenReturn(Future.successful(currentProfile.copy(registrationId = regId)))

    def removeFromKeystore = when(mockKeystoreConnector.remove()(ArgumentMatchers.any()))
      .thenReturn(Future.successful(HttpResponse(200)))

    def failedRemoveFromKeystore = when(mockKeystoreConnector.remove()(ArgumentMatchers.any()))
      .thenReturn(Future.failed(new Exception("ThrownError")))

    def clearFromS4Later = when(mockS4LConnector.clear(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(HttpResponse(200)))

    def failedClearFromS4Later = when(mockS4LConnector.clear(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.failed(new Exception("ThrownError")))
  }

  "deleteEligibilityData" should {
    val regId = "reg-123"

    "return true if the delete is successful" in new Setup {
      mockGetCurrentProfile(regId)
      removeFromKeystore
      clearFromS4Later
      await(service.deleteEligibilityData(regId)) mustBe true
    }

    "return false if the regId doesnt match the current profile ID" in new Setup {
      mockGetCurrentProfile("DifferentId")
      await(service.deleteEligibilityData(regId)) mustBe false
    }

    "throw an error if keystore remove failed with an error" in new Setup {
      mockGetCurrentProfile(regId)
      failedRemoveFromKeystore
      intercept[Exception](await(service.deleteEligibilityData(regId))).getMessage mustBe "ThrownError"
    }

    "throw an error if s4l clear failed with an error" in new Setup {
      mockGetCurrentProfile(regId)
      removeFromKeystore
      failedClearFromS4Later
      intercept[Exception](await(service.deleteEligibilityData(regId))).getMessage mustBe "ThrownError"
    }
  }
}
