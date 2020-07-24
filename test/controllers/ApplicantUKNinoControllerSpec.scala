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

import base.VATEligiblityMocks
import connectors.FakeDataCacheConnector
import controllers.actions._
import forms.ApplicantUKNinoFormProvider
import identifiers.{ApplicantUKNinoId, CompletionCapacityFillingInForId}
import models.{ConditionalNinoFormElement, Name, NormalMode, Officer}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.FakeNavigator
import views.html.applicantUKNino

import scala.concurrent.Future

class ApplicantUKNinoControllerSpec extends ControllerSpecBase with VATEligiblityMocks {

  def onwardRouteNo = routes.IndexController.onPageLoad()
  val formProvider = new ApplicantUKNinoFormProvider()
  def ninoForm(selectedOfficer : Option[String] = None) = formProvider(selectedOfficer)

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new ApplicantUKNinoController(frontendAppConfig, messagesApi, FakeDataCacheConnector,FakeCacheIdentifierAction,
      dataRetrievalAction, new FakeNavigator(desiredRoute = onwardRouteNo), new DataRequiredActionImpl, mockIIService, formProvider, mockVRService)

  def frontendUrl = s"${frontendAppConfig.vatRegFEURL}${frontendAppConfig.vatRegFEURI}${frontendAppConfig.vatRegFEFirstPage}"

  def viewAsString(form: Form[_] = ninoForm(), shortName: Option[String] = None): String = applicantUKNino(frontendAppConfig, form, NormalMode, shortName)(fakeRequest, messages).toString

  "ApplicationUKNino Controller" must {

    val randomOfficer = Officer(Name(None, None, "hymn", None), "test", None, None)

    "return OK and the correct view for a GET" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return OK and the correct view when someone is registering on behalf of someone" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val validData = Map(CompletionCapacityFillingInForId.toString -> Json.toJson(randomOfficer.generateId))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(ninoForm(Some(randomOfficer.shortName)), Some(randomOfficer.shortName))
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val validData = Map(ApplicantUKNinoId.toString -> Json.toJson(ConditionalNinoFormElement(true, Some("nino"))))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      contentAsString(result) mustBe viewAsString(ninoForm().fill(ConditionalNinoFormElement(true, Some("nino"))))
    }

    "redirect to the next page when valid data is submitted when answer is true" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val postRequest = fakeRequest.withFormUrlEncodedBody(
        ("applicantUKNinoSelection", "true"),
        ("applicantUKNinoEntry", "AB123456A")
      )

      when(mockVRService.submitEligibility(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Json.obj()))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(frontendUrl)

      verify(mockVRService, times(1)).submitEligibility(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "redirect to the next navigator page when valid data is submitted when answer is false" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val postRequest = fakeRequest.withFormUrlEncodedBody(
        ("applicantUKNinoSelection", "false")
      )

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardRouteNo.url)

      verify(mockVRService, times(0)).submitEligibility(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = ninoForm().bind(Map("value" -> "invalid value"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when invalid data is submitted on behalf of an officer" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val validData = Map(CompletionCapacityFillingInForId.toString -> Json.toJson(randomOfficer.generateId))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = ninoForm(Some(randomOfficer.shortName)).bind(Map("value" -> "invalid value"))

      val result = controller(getRelevantData).onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm, Some(randomOfficer.shortName))
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }

    "redirect to Session Expired for a POST if no existing data is found" in {
      when(mockIIService.getOfficerList(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(randomOfficer)))

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      val result = controller(dontGetAnyData).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }
  }
}
