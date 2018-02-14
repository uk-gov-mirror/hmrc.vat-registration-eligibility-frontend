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

import connectors.S4LConnector
import helpers.ControllerSpec
import models.CurrentProfile
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import services.CurrentProfileService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class TestCacheControllerSpec extends ControllerSpec with GuiceOneAppPerTest {

  class Setup {
    val controller = new TestCacheController {
      override val s4lConnector: S4LConnector = mockS4LConnector
      val messagesApi: MessagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
      val currentProfileService: CurrentProfileService = mockCurrentProfileService
      val authConnector: AuthConnector = mockAuthClientConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  "tearDownS4L" should {
    "clear S4L" in new Setup {
      when(mockS4LConnector.clear(any())(any())).thenReturn(Future(HttpResponse(200)))

      callAuthenticated(controller.tearDownS4L)(status(_) mustBe 200)
    }
  }
}
