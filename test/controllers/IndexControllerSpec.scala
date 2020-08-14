/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.test.Helpers._
import utils.FakeNavigator

class IndexControllerSpec extends ControllerSpecBase {

  class Setup {
    val controller = new IndexController(frontendAppConfig, messagesApi, new FakeNavigator(desiredRoute = routes.ThresholdInTwelveMonthsController.onPageLoad()))
  }
  "Index Controller" must {
    "return 200 for a GET" in new Setup {
      val result = controller.onPageLoad()(fakeRequest)
      redirectLocation(result) mustBe Some(routes.ThresholdInTwelveMonthsController.onPageLoad().url)
    }
    "navigateToPage with a page id takes user to page in navigator" in new Setup {
      val result = controller.navigateToPageId("foo")(fakeRequest)
    }
  }
}
