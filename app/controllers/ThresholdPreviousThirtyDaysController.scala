/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.{LocalDate => LocalDateJoda}
import uk.gov.hmrc.time.DateTimeUtils
import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions._
import forms.ThresholdPreviousThirtyDaysFormProvider
import identifiers.ThresholdPreviousThirtyDaysId
import javax.inject.Inject

import models.requests.DataRequest
import models.{ConditionalDateFormElement, NormalMode}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import services.ThresholdService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers, VATDateHelper, ThresholdHelper}
import views.html.thresholdPreviousThirtyDays

import scala.concurrent.Future

class ThresholdPreviousThirtyDaysController @Inject()(appConfig: FrontendAppConfig,
                                         override val messagesApi: MessagesApi,
                                         dataCacheConnector: DataCacheConnector,
                                         navigator: Navigator,
                                         identify: CacheIdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         thresholdService: ThresholdService,
                                         formProvider: ThresholdPreviousThirtyDaysFormProvider) extends FrontendController with I18nSupport {

  def incorpDate(implicit request: DataRequest[_]): LocalDate = request.currentProfile.incorpDate.getOrElse(
    throw new RuntimeException(
      s"Trying to access post incorp page with no incorp date for txId ${request.currentProfile.transactionID} and regId ${request.currentProfile.registrationID}"
    )
  )

  def onPageLoad() = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.thresholdPreviousThirtyDays match {
        case None => formProvider(incorpDate)
        case Some(value) => formProvider(incorpDate).fill(value)
      }
      Ok(thresholdPreviousThirtyDays(appConfig, preparedForm, NormalMode, thresholdService))
  }

  def onSubmit() = (identify andThen getData andThen requireData).async {
    implicit request =>
      formProvider(incorpDate).bindFromRequest().fold(
        (formWithErrors: Form[_]) => {
          Future.successful(BadRequest(thresholdPreviousThirtyDays(appConfig, formWithErrors, NormalMode,thresholdService)))
        },
        (formValue) =>
          dataCacheConnector.save[ConditionalDateFormElement](request.internalId,ThresholdPreviousThirtyDaysId.toString, formValue).flatMap{
            cacheMap =>
              val userAnswers = new UserAnswers(cacheMap)
              if (ThresholdHelper.q1DefinedAndTrue(userAnswers) | formValue.value | userAnswers.thresholdNextThirtyDays.getOrElse(false))  {
                thresholdService.removeVoluntaryRegistration
              } else {
                Future.successful(cacheMap)
              }
          }.map(cMap => Redirect(navigator.nextPage(ThresholdPreviousThirtyDaysId, NormalMode)(new UserAnswers(cMap)))))
  }
}