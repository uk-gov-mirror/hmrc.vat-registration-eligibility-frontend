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

import helpers.VatRegSpec
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest

class SignInOutControllerSpec extends VatRegSpec {
  class Setup {
    val controller = new SignInOutController {
      override val authConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = mockMessagesApi
      override val compRegFEURL: String = "testUrl"
      override val compRegFEURI: String = "/testUri"
    }
  }

  "Calling the postSignIn action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.postSignIn()(FakeRequest())
      status(result) shouldBe 303
    }

    "redirect the user to the Company Registration post-sign-in action" in new Setup {
      callAuthorised(controller.postSignIn()) { res =>
        res isA 303
        res redirectsTo s"${controller.compRegFEURL}${controller.compRegFEURI}/post-sign-in"
      }
    }
  }

  "signOut" should {
    "redirect to the exit questionnaire and clear the session" in new Setup {
      callAuthorised(controller.signOut()) { res =>
        res isA 303
        res redirectsTo s"${controller.compRegFEURL}${controller.compRegFEURI}/questionnaire"
      }
    }
  }
}
