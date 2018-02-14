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

package controllers.callbacks

import helpers.ControllerSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest

class SignInOutControllerSpec extends ControllerSpec with GuiceOneAppPerTest {
  class Setup {
    val controller = new SignInOutController {
      override val compRegFEURL: String = "testUrl"
      override val compRegFEURI: String = "/testUri"

      val authConnector = mockAuthClientConnector
      val messagesApi: MessagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
      val currentProfileService = mockCurrentProfileService
    }
  }

  "Calling the postSignIn action" should {
    "return 303 for an unauthorised user" in new Setup {
      mockNoActiveSession()

      val result = controller.postSignIn()(FakeRequest())
      status(result) mustBe 303
    }

    "redirect the user to the Company Registration post-sign-in action" in new Setup {
      callAuthenticated(controller.postSignIn()) { res =>
        status(res) mustBe 303
        redirectLocation(res) mustBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/post-sign-in")
      }
    }
  }

  "signOut" should {
    "redirect to the exit questionnaire and clear the session" in new Setup {
      callAuthenticated(controller.signOut()) { res =>
        status(res) mustBe 303
        redirectLocation(res) mustBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/questionnaire")
      }
    }
  }
}
