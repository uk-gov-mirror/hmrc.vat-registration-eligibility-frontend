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

import java.time.LocalDate

import config.FrontendAppConfig
import controllers.actions._
import identifiers.{AgriculturalFlatRateSchemeId, InternationalActivitiesId}
import mocks.TrafficManagementServiceMock
import models.{Draft, RegistrationInformation, VatReg}
import play.api.test.Helpers._
import views.html.{agriculturalDropout, eligibilityDropout, internationalActivityDropout}

import scala.concurrent.Future

class EligibilityDropoutControllerSpec extends ControllerSpecBase with TrafficManagementServiceMock {

  def onwardRoute = routes.EligibilityDropoutController.onPageLoad("")

  implicit val appConfig = app.injector.instanceOf[FrontendAppConfig]

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new EligibilityDropoutController(
      controllerComponents,
      FakeCacheIdentifierAction,
      dataRetrievalAction,
      new DataRequiredAction,
      mockTrafficManagementService)

  val testInternalId = "testInternalId"
  val testRegId = "testRegId"
  val testDate = LocalDate.now

  def viewAsString(default: Boolean) = eligibilityDropout(default)(fakeCacheDataRequestIncorped, messages, appConfig).toString

  def internationalView() = internationalActivityDropout()(fakeCacheDataRequestIncorped, messages, frontendAppConfig).toString

  def agricultureView() = agriculturalDropout()(fakeCacheDataRequestIncorped, messages, appConfig).toString

  "EligibilityDropout Controller" must {

    "return OK and the default view for a GET" in {
      val result = controller().onPageLoad("default")(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(default = true)
    }

    "return OK and the International Dropout view for a GET" in {
      val result = controller().onPageLoad(InternationalActivitiesId.toString)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe internationalView()
    }

    "return OK and the Agriculture Dropout view for a GET" in {
      val result = controller().onPageLoad(AgriculturalFlatRateSchemeId.toString)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe agricultureView()
    }

    "redirect to the next page when valid data is submitted" in {
      mockUpsertRegistrationInformation(testInternalId, testRegId, false, false)(Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, Some(testDate), VatReg)))

      val postRequest = fakeRequest.withFormUrlEncodedBody()

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

  }
}




