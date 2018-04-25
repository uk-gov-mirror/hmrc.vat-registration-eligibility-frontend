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

package controllers

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import helpers.{ControllerSpec, FutureAssertions}
import models.CurrentProfile
import models.external.IncorporationInfo
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EligibilitySummaryControllerSpec extends ControllerSpec with GuiceOneAppPerTest with VatRegistrationFixture with FutureAssertions {

  class SetupOverYear {

    val testController = new EligibilitySummaryController {
      override val authConnector = mockAuthClientConnector
      override val summaryService = mockSummaryService
      override val vatRegistrationService = mockVatRegistrationService
      override val currentProfileService = mockCurrentProfileService
      override val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }

    val INCORPORATION_STATUS = "incorporationStatus"
  }

  class SetupWithinYear {
    val testController = new EligibilitySummaryController {
      override val authConnector = mockAuthClientConnector
      override val summaryService = mockSummaryService
      override val vatRegistrationService = mockVatRegistrationService
      override val currentProfileService = mockCurrentProfileService
      override val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile.copy(incorporationDate = Some(incorpDateWithinYear)))
      }
    }

    val INCORPORATION_STATUS = "incorporationStatus"
  }

  class SetupNoIncorp {
    val testController = new EligibilitySummaryController {
      override val authConnector = mockAuthClientConnector
      override val summaryService = mockSummaryService
      override val vatRegistrationService = mockVatRegistrationService
      override val currentProfileService = mockCurrentProfileService
      override val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile.copy(incorporationDate = None))
      }
    }

    val INCORPORATION_STATUS = "incorporationStatus"
  }

  val fakeRequest = FakeRequest(controllers.routes.EligibilitySummaryController.show())

  "Calling eligibility summary to show the Eligibility summary page" should {
    "return HTML with a valid threshold summary view" in new SetupOverYear {
      when(mockSummaryService.getEligibilitySummary(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEligibilitySummary))

      callAuthenticated(testController.show)(_ includesText "Check and confirm your answers")
    }
  }

  s"POST ${controllers.routes.EligibilitySummaryController.submit()}" should {
    "return 303 and redirect to overThresholdThirtyShow when the company is not incorporated" in new SetupNoIncorp {
      mockKeystoreFetchAndGet[IncorporationInfo](INCORPORATION_STATUS, None)

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody()) {
        _ redirectsTo controllers.routes.ThresholdController.overThresholdThirtyShow().url
      }
    }

    "return 303 and redirect to goneOverSinceIncorpShow when the company was incorporated less than a year ago" in new SetupWithinYear {
      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody()) {
        _ redirectsTo controllers.routes.ThresholdController.goneOverSinceIncorpShow().url
      }
    }

    "return 303 and redirect to overThresholdThirtyShow when the company has been incorporated for over a year" in new SetupOverYear {
      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody()) {
        _ redirectsTo controllers.routes.ThresholdController.overThresholdThirtyShow().url
      }
    }
  }
}
