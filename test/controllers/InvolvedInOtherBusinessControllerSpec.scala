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
import forms.InvolvedInOtherBusinessFormProvider
import identifiers.{CompletionCapacityFillingInForId, InvolvedInOtherBusinessId}
import models.{Name, NormalMode, Officer}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.data.Form
import play.api.libs.json.{JsBoolean, JsString}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.FakeNavigator
import views.html.involvedInOtherBusiness

import scala.concurrent.Future

class InvolvedInOtherBusinessControllerSpec extends ControllerSpecBase {

  def onwardRoute = routes.IndexController.onPageLoad()

  val formProvider = new InvolvedInOtherBusinessFormProvider()
  val form = formProvider.form()

  val officersList: Seq[Officer] = Seq(
    Officer(Name(Some("First"), Some("Middle"), "Last",Some("Mrs")),"director", None, Some("some-url")),
    Officer(Name(Some("Second"), None, "VeryLast",Some("Mr")), "secretary", None, Some("some-url"))
  )

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new InvolvedInOtherBusinessController(frontendAppConfig, messagesApi, FakeDataCacheConnector, new FakeNavigator(desiredRoute = onwardRoute), FakeCacheIdentifierAction,
      dataRetrievalAction, new DataRequiredActionImpl, formProvider, mockIIService)

  def viewAsString(form: Form[_] = form, officer: Option[String] = None) = involvedInOtherBusiness(frontendAppConfig, form, NormalMode, officer)(fakeRequest, messages).toString

  "InvolvedInOtherBusiness Controller" must {
    "return OK and the correct view for a GET" in {
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future.successful(Seq.empty))
      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future.successful(officersList))
      val validData = Map(InvolvedInOtherBusinessId.toString -> JsBoolean(true),
        CompletionCapacityFillingInForId.toString -> JsString(officersList.head.generateId))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(true), Some("First Last"))
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future.successful(Seq.empty))
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a Bad Request and errors when invalid data is submitted passing the officer name back into view and not acting on behalf of" in {
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future.successful(Seq.empty))
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }
    "return a Bad Request and errors when invalid data is submitted passing the officer name back into view and acting on half of" in {
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future.successful(officersList))
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))
      val validData = Map(InvolvedInOtherBusinessId.toString -> JsBoolean(true),
        CompletionCapacityFillingInForId.toString -> JsString(officersList.head.generateId))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))
      val result = controller(getRelevantData).onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm, Some("First Last"))
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future.successful(Seq.empty))
      val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }

    "redirect to Session Expired for a POST if no existing data is found" in {
      when(mockIIService.getOfficerList(any())(any())).thenReturn(Future.successful(Seq.empty))
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      val result = controller(dontGetAnyData).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }
  }
}