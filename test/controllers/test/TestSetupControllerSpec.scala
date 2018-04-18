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

import connectors.{S4LConnector, VatRegistrationConnector}
import forms.VoluntaryRegistrationReasonForm._
import helpers.{ControllerSpec, FutureAssertions}
import models.CurrentProfile
import org.mockito.ArgumentMatchers.any
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
    "display the test setup form" in new Setup {
      when(mockS4LConnector.fetchAndGet(any(), any())(any(), any())).thenReturn(Future(None))

      callAuthenticated(testController.show())(_ includesText "Test Setup")
    }
  }

  "Calling submit" should {
    "save Eligibility and Threshold view models into S4L" in new Setup {
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
          "overThresholdSelection" -> "false",
          "overThresholdMonth" -> "",
          "overThresholdYear" -> "",
          "expectationOverThresholdSelection" -> "true",
          "expectationOverThresholdDay" -> "6",
          "expectationOverThresholdMonth" -> "8",
          "expectationOverThresholdYear" -> "2017"
        )
      )(_ isA 200)
    }
  }

  "Calling addThresholdToBackend" should {
    "update patch threshold in backend" in new Setup {
      when(mockRegConnector.patchThreshold(any())(any(), any())).thenReturn(Future(Json.obj()))

      callAuthenticated(testController.addThresholdToBackend(Some("test"), Some("2017-08-17"), Some("2017-09-05")))(_ isA 200)
    }
  }
}
