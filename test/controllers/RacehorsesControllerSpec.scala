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

import connectors.FakeDataCacheConnector
import controllers.actions._
import forms.RacehorsesFormProvider
import identifiers.RacehorsesId
import models.NormalMode
import models.requests.DataRequest
import org.mockito.Matchers
import org.scalatest.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.{JsBoolean, JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.FakeNavigator
import views.html.racehorses
import org.mockito.Mockito._

import scala.concurrent.{ExecutionContext, Future}

class RacehorsesControllerSpec extends ControllerSpecBase with MockitoSugar{

  def onwardRoute = routes.IndexController.onPageLoad()

  val formProvider = new RacehorsesFormProvider()
  val form = formProvider()
  implicit val appConfig = frontendAppConfig

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new RacehorsesController(messagesApi, FakeDataCacheConnector, new FakeNavigator(desiredRoute = onwardRoute), FakeCacheIdentifierAction,
      dataRetrievalAction, new DataRequiredActionImpl, formProvider, mockVRService)

  def viewAsString(form: Form[_] = form) = racehorses(form, NormalMode)(fakeDataRequestIncorped, messages, frontendAppConfig).toString

  "Racehorses Controller" must {

    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val validData = Map(RacehorsesId.toString -> JsBoolean(true))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(true))
    }

    "redirect to the next page when valid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      when(mockVRService.submitEligibility(Matchers.any[String])(Matchers.any[HeaderCarrier], Matchers.any[ExecutionContext], Matchers.any[DataRequest[_]]))
        .thenReturn(Future.successful(Json.obj()))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("http://localhost:9895/register-for-vat/changed-name") //TODO - check if this can be retrieved from somewhere
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }

    "redirect to Session Expired for a POST if no existing data is found" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      val result = controller(dontGetAnyData).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }
  }
}
