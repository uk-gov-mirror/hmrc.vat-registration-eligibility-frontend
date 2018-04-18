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
import forms.VoluntaryRegistrationReasonForm._
import helpers.FutureAssertions
import mocks.VatMocks
import models.CurrentProfile
import models.view._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class ThresholdServiceSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAwaits
                           with DefaultAwaitTimeout with FutureAssertions with VatRegistrationFixture {
  implicit val hc = HeaderCarrier()
  implicit val currentProfilePreIncorp = CurrentProfile("Test Me", testRegId, "000-434-1", VatRegStatus.draft, None)
  val currentProfilePostIncorp = CurrentProfile("Test Me", testRegId, "000-434-1", VatRegStatus.draft, Some(LocalDate.of(2016, 12, 21)))
  val thresholdPreIncorpComplete = Threshold(Some(false), Some(true), Some(SELLS), None, None)
  val overThresholdFalse = OverThresholdView(false, None)
  val overThresholdTrue = OverThresholdView(true, testDate)
  val expectOverThresholdFalse = ExpectationOverThresholdView(false, None)
  val expectOverThresholdTrue = ExpectationOverThresholdView(true, testDate)
  val thresholdPostIncorpComplete = Threshold(None, Some(true), Some(SELLS), Some(overThresholdFalse), Some(expectOverThresholdFalse))
  val thresholdPostIncorpCompleteOver1 = Threshold(None, None, None, Some(overThresholdTrue), Some(expectOverThresholdFalse))
  val thresholdPostIncorpCompleteOver2 = Threshold(None, None, None, Some(overThresholdFalse), Some(expectOverThresholdTrue))

  val date = LocalDate.of(1990, 12, 12)

  val incompleteThreshold = Threshold(Some(false), Some(true))

  class Setup {
    val service = new ThresholdService {
      override val s4LConnector = mockS4LConnector
      override val vatRegistrationConnector = mockRegConnector
      override val now = date
    }

    def mockAllGetThreshold(s4l: Option[Threshold] = None, backend: Option[Threshold] = None): OngoingStubbing[Future[Option[Threshold]]] = {
      when(mockS4LConnector.fetchAndGet[Threshold](any(), any())(any(), any())) thenReturn Future.successful(s4l)
      when(mockRegConnector.getThreshold(any(), any())) thenReturn Future.successful(backend)
    }

    def mockSaveComplete() = {
      when(mockRegConnector.patchThreshold(any())(any(), any())) thenReturn Future.successful(Json.obj())
      when(mockS4LConnector.clear(any())(any())) thenReturn Future.successful(HttpResponse(200))
    }

    def mockSaveIncomplete() = {
      when(mockS4LConnector.save[Threshold](any(),any(), any())(any(), any())) thenReturn Future.successful(CacheMap("test", Map.empty))
    }

    resetMocks()
  }
  

  "Calling getThreshold" must {
    "retrieve a Threshold from S4L if it is present in S4L" in new Setup {
      when(mockS4LConnector.fetchAndGet[Threshold](any(), any())(any(), any())) thenReturn Future.successful(Some(incompleteThreshold))

      await(service.getThreshold) mustBe incompleteThreshold
    }

    "retrieve a Threshold from the backend if not in S4L" in new Setup {
      when(mockS4LConnector.fetchAndGet[Threshold](any(), any())(any(), any())) thenReturn Future.successful(None)
      when(mockRegConnector.getThreshold(any(), any())) thenReturn Future.successful(Some(incompleteThreshold))

      await(service.getThreshold) mustBe incompleteThreshold
    }

    "return back an empty Threshold model if not present in S4L or backend" in new Setup {
      when(mockS4LConnector.fetchAndGet[Threshold](any(), any())(any(), any())) thenReturn Future.successful(None)
      when(mockRegConnector.getThreshold(any(), any())) thenReturn Future.successful(None)

      await(service.getThreshold) mustBe emptyThreshold
    }
  }

  "saveTaxableTurnover" must {
    "save a complete model" in new Setup {
      mockAllGetThreshold()
      mockSaveComplete()
      await(service.saveTaxableTurnover(taxableTurnover = true)) mustBe Threshold(Some(true))
    }
    "save an incomplete model" in new Setup {
      mockAllGetThreshold()
      mockSaveIncomplete()
      await(service.saveTaxableTurnover(taxableTurnover = false)) mustBe Threshold(Some(false))
    }
  }

  "saveVoluntaryRegistration" must {
    "save a complete model" in new Setup {
      val incomplete = Threshold(Some(false), None, Some("test"))
      mockAllGetThreshold(Some(incomplete))
      mockSaveComplete()
      await(service.saveVoluntaryRegistration(voluntaryRegistration = true)) mustBe incomplete.copy(voluntaryRegistration = Some(true))
    }

    "save a complete model where they choose not to register voluntary but there is already a reason" in new Setup {
      val incomplete = Threshold(Some(false), None, Some("test"))
      val expected = Threshold(Some(false), Some(false))
      mockAllGetThreshold(Some(incomplete))
      mockSaveComplete()
      await(service.saveVoluntaryRegistration(voluntaryRegistration = false)) mustBe expected
    }

    "save an incomplete model" in new Setup {
      mockAllGetThreshold()
      mockSaveIncomplete()
      await(service.saveVoluntaryRegistration(voluntaryRegistration = false)) mustBe Threshold(voluntaryRegistration = Some(false))
    }
  }

  "saveVoluntaryRegistrationReason" must {
    "save a complete model" in new Setup {
      val incomplete = Threshold(Some(false), Some(true))
      mockAllGetThreshold(Some(incomplete))
      mockSaveComplete()
      await(service.saveVoluntaryRegistrationReason(reason = "testReason")) mustBe incomplete.copy(voluntaryRegistrationReason = Some("testReason"))
    }
    "save an incomplete model" in new Setup {
      mockAllGetThreshold()
      mockSaveIncomplete()
      await(service.saveVoluntaryRegistrationReason(reason = "testReason")) mustBe Threshold(voluntaryRegistrationReason = Some("testReason"))
    }
  }

  "saveOverThreshold" must {
    val overThreshold = OverThresholdView(selection = true, Some(LocalDate.now()))

    "save a complete model" in new Setup {
      val incomplete = Threshold(None, None, None, None, Some(ExpectationOverThresholdView(selection = false, None)))
      mockAllGetThreshold(Some(incomplete))
      mockSaveComplete()
      await(service.saveOverThreshold(overThreshold)) mustBe incomplete.copy(overThreshold = Some(overThreshold))
    }
    "save an incomplete model" in new Setup {
      mockAllGetThreshold()
      mockSaveIncomplete()
      await(service.saveOverThreshold(overThreshold)) mustBe Threshold(None, None, None, Some(overThreshold))
    }
  }

  "saveExpectationOverThreshold" must {
    val expectationOverThreshold = ExpectationOverThresholdView(selection = true, Some(LocalDate.now()))

    "save a complete model" in new Setup {
      val incomplete = Threshold(None, None, None, Some(OverThresholdView(selection = false, None)))
      mockAllGetThreshold(Some(incomplete))
      mockSaveComplete()
      await(service.saveExpectationOverThreshold(expectationOverThreshold)) mustBe incomplete.copy(expectationOverThreshold = Some(expectationOverThreshold))
    }
    "save an incomplete model" in new Setup {
      mockAllGetThreshold()
      mockSaveIncomplete()
      await(service.saveExpectationOverThreshold(expectationOverThreshold)) mustBe Threshold(None, None, None, None, Some(expectationOverThreshold))
    }
  }

  "fetchCurrentVatThreshold" should {

    val vatThreshold = "12345"
    val formattedThreshold = "12,345"

    "fetch the current vat threshold and format it to have thousand separator" in new Setup {
      when(mockRegConnector.getVATThreshold(any())(any()))
        .thenReturn(Future.successful(vatThreshold))

      await(service.fetchCurrentVatThreshold) mustBe formattedThreshold
    }
  }
}
