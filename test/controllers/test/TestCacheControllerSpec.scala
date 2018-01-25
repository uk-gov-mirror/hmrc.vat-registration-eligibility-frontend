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
import helpers.VatRegSpec
import models.CurrentProfile
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import services.CurrentProfileService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class TestCacheControllerSpec extends VatRegSpec {

  class Setup {
    val controller = new TestCacheController {
      override val s4lConnector: S4LConnector = mockS4LConnector
      override def messagesApi: MessagesApi = mockMessagesApi
      override val currentProfileService: CurrentProfileService = mockCurrentProfileService
      override protected def authConnector: AuthConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  "tearDownS4L" should {
    "clear S4L" in new Setup {
      when(mockS4LConnector.clear(any())(any())).thenReturn(Future(HttpResponse(200)))

      callAuthorised(controller.tearDownS4L)(_ isA 200)
    }
  }
}
