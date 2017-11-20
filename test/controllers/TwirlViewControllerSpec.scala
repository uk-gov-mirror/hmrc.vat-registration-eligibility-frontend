/*
 * Copyright 2017 HM Revenue & Customs
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

import helpers.VatRegSpec
import models.CurrentProfile
import play.api.http.Status
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class TwirlViewControllerSpec extends VatRegSpec {

  object TestController extends TwirlViewController() {
    override val authConnector: AuthConnector = mockAuthConnector

    override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
      f(currentProfile)
    }
  }

  "GET" should {
    "return HTML when user is authorized to access" in {
      val page = "use-this-service"
      val expectedTitle = "Can you use this service?"

      callAuthorised(TestController.renderViewAuthorised(page)) {
        _ includesText expectedTitle
      }
    }

    "return 404" when {
      "requested twirl template does not exist" in {

        callAuthorised(TestController.renderViewAuthorised("fake")) { result =>
          result isA Status.NOT_FOUND
        }
      }
    }
  }
}
