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

package controllers.test

import java.time.LocalDate

import common.enums.CacheKeys
import connectors.{S4LConnector, VatRegistrationConnector}
import forms.VoluntaryRegistrationReasonForm._
import helpers.{ControllerSpec, FutureAssertions}
import models.CurrentProfile
import models.view.{Eligibility, Threshold, ThresholdView}
import org.mockito.ArgumentMatchers.{any, matches}
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import services.CurrentProfileService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class TestSetupControllerSpec extends ControllerSpec with GuiceOneAppPerTest with FutureAssertions {
  val mockTestS4LBuilder = mock[TestS4LBuilder]

  trait Setup {
    val testController: TestSetupController = new TestSetupController {
      override val s4LBuilder: TestS4LBuilder = mockTestS4LBuilder
      override val vatRegistrationConnector: VatRegistrationConnector = mockRegConnector
      override val s4LConnector: S4LConnector = mockS4LConnector
      val authConnector: AuthConnector = mockAuthClientConnector
      val currentProfileService: CurrentProfileService = mockCurrentProfileService
      val messagesApi: MessagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  "Calling show" should {
    val eligibility = Eligibility(Some(true), Some(false), Some(false), Some(false), Some(false), Some(false))
    val thresholdPreIncorp = Threshold(Some(false), Some(true), Some("test"), None, None, None)
    val thresholdView = ThresholdView(true, Some(LocalDate.now))
    val thresholdPostIncorp = Threshold(None, None, None, Some(thresholdView), Some(thresholdView), Some(thresholdView))

    "display the test setup form with no data" in new Setup {
      when(mockS4LConnector.fetchAndGet(any(), any())(any(), any())).thenReturn(Future(None))
      callAuthenticated(testController.show())(_ includesText "Test Setup")
    }
    "display the test setup form with data for voluntary" in new Setup {
      when(mockS4LConnector.fetchAndGet[Threshold](any(), matches(CacheKeys.Threshold))(any(), any())) thenReturn Future.successful(Some(thresholdPreIncorp))
      when(mockS4LConnector.fetchAndGet[Eligibility](any(), matches(CacheKeys.Eligibility))(any(), any())) thenReturn Future.successful(Some(eligibility))

      callAuthenticated(testController.show())(_ includesText "Test Setup")
    }
    "display the test setup form with data for mandatory post incorp" in new Setup {
      when(mockS4LConnector.fetchAndGet[Threshold](any(), matches(CacheKeys.Threshold))(any(), any())) thenReturn Future.successful(Some(thresholdPostIncorp))
      when(mockS4LConnector.fetchAndGet[Eligibility](any(), matches(CacheKeys.Eligibility))(any(), any())) thenReturn Future.successful(Some(eligibility))

      callAuthenticated(testController.show())(_ includesText "Test Setup")
    }
  }

  "Calling submit" should {
    "save Eligibility and Threshold view models into S4L when voluntary" in new Setup {
      when(mockS4LConnector.save(any(), any(), any())(any(), any())).thenReturn(Future(CacheMap("id", Map())))

      submitAuthorised(testController.submit(),
        FakeRequest().withFormUrlEncodedBody(
          "haveNino" -> "true",
          "doingBusinessAbroad" -> "false",
          "doAnyApplyToYou" -> "false",
          "applyingForAnyOf" -> "false",
          "applyingForVatExemption" -> "false",
          "companyWillDoAnyOf" -> "false",
          "taxableTurnoverChoice" -> "true",
          "voluntaryChoice" -> "true",
          "voluntaryRegistrationReason" -> SELLS,
          "overThresholdTwelveSelection" -> "false",
          "overThresholdTwelveMonth" -> "",
          "overThresholdTwelveYear" -> "",
          "pastOverThresholdThirtySelection" -> "false",
          "pastOverThresholdThirtyDay" -> "",
          "pastOverThresholdThirtyMonth" -> "",
          "pastOverThresholdThirtyYear" -> "",
          "overThresholdThirtySelection" -> "false",
          "overThresholdThirtyDay" -> "",
          "overThresholdThirtyMonth" -> "",
          "overThresholdThirtyYear" -> ""
        )
      )(_ isA 200)
    }

    "save Eligibility and Threshold view models into S4L when mandatory" in new Setup {
      when(mockS4LConnector.save(any(), any(), any())(any(), any())).thenReturn(Future(CacheMap("id", Map())))

      submitAuthorised(testController.submit(),
        FakeRequest().withFormUrlEncodedBody(
          "haveNino" -> "true",
          "doingBusinessAbroad" -> "false",
          "doAnyApplyToYou" -> "false",
          "applyingForAnyOf" -> "false",
          "applyingForVatExemption" -> "false",
          "companyWillDoAnyOf" -> "false",
          "taxableTurnoverChoice" -> "true",
          "voluntaryChoice" -> "false",
          "voluntaryRegistrationReason" -> "",
          "overThresholdTwelveSelection" -> "true",
          "overThresholdTwelveMonth" -> "08",
          "overThresholdTwelveYear" -> "2017",
          "pastOverThresholdThirtySelection" -> "true",
          "pastOverThresholdThirtyDay" -> "15",
          "pastOverThresholdThirtyMonth" -> "08",
          "pastOverThresholdThirtyYear" -> "2017",
          "overThresholdThirtySelection" -> "true",
          "overThresholdThirtyDay" -> "15",
          "overThresholdThirtyMonth" -> "08",
          "overThresholdThirtyYear" -> "2017"
        )
      )(_ isA 200)
    }
  }

  "Calling addThresholdToBackend" should {
    "update patch threshold in backend when voluntary" in new Setup {
      when(mockRegConnector.patchThreshold(any())(any(), any())).thenReturn(Future(Json.obj()))
      callAuthenticated(testController.addThresholdToBackend(Some("test"), None, None, None))(_ isA 200)
    }

    "update patch threshold in backend when mandatory with all dates" in new Setup {
      when(mockRegConnector.patchThreshold(any())(any(), any())).thenReturn(Future(Json.obj()))
      callAuthenticated(testController.addThresholdToBackend(None, Some("2017-08-17"), Some("2017-09-05"), Some("2017-08-17")))(_ isA 200)
    }

  }
}
