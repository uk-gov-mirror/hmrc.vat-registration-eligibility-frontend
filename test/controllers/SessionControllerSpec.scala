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

import akka.util.Timeout
import helpers.ControllerSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest

import scala.concurrent.duration._

class SessionControllerSpec extends ControllerSpec with GuiceOneAppPerTest {

  val testController = new SessionController {
    val authConnector = mockAuthClientConnector
    val messagesApi = fakeApplication.injector.instanceOf(classOf[MessagesApi])
    val currentProfileService = mockCurrentProfileService
  }

  implicit val duration: Timeout = 5.seconds
  implicit lazy val mat = fakeApplication.materializer

  "renewSession" should {
    "return 200 when hit with Authorised User" in {
      callAuthenticated(testController.renewSession()){ res =>
        status(res) mustBe 200
        contentType(res) mustBe Some("image/jpeg")
        await(res).body.toString.contains("""renewSession.jpg""") mustBe true
      }
    }
  }

  "destroySession" should {
    "return redirect to timeout show and get rid of headers" in {

      val fr = FakeRequest().withHeaders(("playFoo","no more"))

      val res = testController.destroySession()(fr)
      status(res) mustBe 303
      headers(res).contains("playFoo") mustBe false

      redirectLocation(res) mustBe Some(controllers.routes.SessionController.timeoutShow().url)
    }
  }

  "timeoutShow" should {
    "return 200" in {
      val res = testController.timeoutShow()(FakeRequest())
      status(res) mustBe 200
    }
  }
}
