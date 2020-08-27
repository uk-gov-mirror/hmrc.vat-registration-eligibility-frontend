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
import java.time.format.DateTimeFormatterBuilder

import connectors.FakeDataCacheConnector
import controllers.actions._
import forms.ThresholdInTwelveMonthsFormProvider
import identifiers.ThresholdInTwelveMonthsId
import models.{ConditionalDateFormElement, NormalMode}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.FakeNavigator
import views.html.thresholdInTwelveMonths

import scala.concurrent.Future


class ThresholdInTwelveMonthsControllerSpec extends ControllerSpecBase {

  def onwardRoute = routes.IndexController.onPageLoad()

  val formProvider = new ThresholdInTwelveMonthsFormProvider()
  val form = formProvider(LocalDate.now().minusYears(2))
  implicit val appConfig = frontendAppConfig

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new ThresholdInTwelveMonthsController(messagesApi, FakeDataCacheConnector, new FakeNavigator(desiredRoute = onwardRoute), FakeCacheIdentifierAction,
      dataRetrievalAction, new DataRequiredActionImpl, mockThresholdService, formProvider)

  def viewAsString(form: Form[_] = form) = thresholdInTwelveMonths(form, NormalMode, mockThresholdService)(fakeDataRequestIncorpedOver12m, messages, frontendAppConfig).toString

  "ThresholdInTwelveMonths Controller" must {
    when(mockThresholdService.returnThresholdDateResult[String](any())(any())).thenReturn("foo")
    "return OK and the correct view for a GET" in {

      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val jsValue = Json.toJson(ConditionalDateFormElement(true, Some(LocalDate.now)))
      val validData = Map(ThresholdInTwelveMonthsId.toString -> jsValue)
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(ConditionalDateFormElement(true, Some(LocalDate.now))))
    }

    "redirect to the next page when valid data is submitted and also remove Voluntary registration because answer to question is true" in {
      when(mockThresholdService.removeVoluntaryAndNextThirtyDays(any())) thenReturn Future.successful(emptyCacheMap)

      val postRequest = fakeRequest.withFormUrlEncodedBody("thresholdInTwelveMonthsSelection" -> "true",
        "thresholdInTwelveMonthsDate.year" -> LocalDate.now().getYear.toString,
        "thresholdInTwelveMonthsDate.month" -> LocalDate.now().getMonthValue.toString
      )

      val result = controller().onSubmit()(postRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
      verify(mockThresholdService, times(1)).removeVoluntaryAndNextThirtyDays(any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
      verify(mockThresholdService, times(0)).removeVoluntaryAndNextThirtyDays(any())
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
