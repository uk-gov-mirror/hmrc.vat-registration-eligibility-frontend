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

import connectors.FakeDataCacheConnector
import controllers.actions._
import forms.CompletionCapacityFormProvider
import identifiers.CompletionCapacityId
import models.{CompletionCapacity, Name, NormalMode, Officer}
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.data.Form
import play.api.libs.json.JsString
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{FakeNavigator, RadioOption}
import views.html.completionCapacity

import scala.concurrent.Future

class CompletionCapacityControllerSpec extends ControllerSpecBase {

  def onwardRoute = routes.IndexController.onPageLoad()

  val officersList: Seq[Officer] = Seq(
    Officer(Name(Some("First"), Some("Middle"), "Last",Some("Mrs")),"director", None, Some("some-url")),
    Officer(Name(Some("Second"), None, "VeryLast",Some("Mr")), "secretary", None, Some("some-url"))
  )

  val formProvider = new CompletionCapacityFormProvider()
  val form = formProvider(CompletionCapacityId)(officersList)

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new CompletionCapacityController(frontendAppConfig, messagesApi, FakeDataCacheConnector, new FakeNavigator(desiredRoute = onwardRoute), FakeCacheIdentifierAction,
      dataRetrievalAction, new DataRequiredActionImpl, mockIIService, formProvider)

  def viewAsString(form: Form[_] = form, officers: Seq[RadioOption] = CompletionCapacity.multipleOfficers(officersList), shortName: Option[String] = None) =
    completionCapacity(frontendAppConfig, form, NormalMode, officers, shortName)(fakeRequest, messages).toString

  "CompletionCapacity Controller" must {

    "return OK and the correct view for a GET" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any())).thenReturn(Future.successful(officersList))

      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val validData = Map(CompletionCapacityId.toString -> JsString(officersList.head.generateId))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any())).thenReturn(Future.successful(officersList))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(officersList.head.generateId))
    }

    "redirect to the next page when valid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", officersList.head.generateId))

      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any())).thenReturn(Future.successful(officersList))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any())).thenReturn(Future.successful(officersList))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when invalid data is submitted with one officer" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "mrJohnDoeDirector"))
      val boundForm = form.bind(Map("value" -> "mrJohnDoeDirector"))

      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(officersList.head)))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm, CompletionCapacity.singleOfficer(officersList.head), Some(officersList.head.shortName))
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any())).thenReturn(Future.successful(officersList))

      val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }

    "redirect to Session Expired for a POST if no existing data is found" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", officersList.head.generateId))

      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any())).thenReturn(Future.successful(officersList))

      val result = controller(dontGetAnyData).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }
  }
}
