/*
 * Copyright 2021 HM Revenue & Customs
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

import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions._
import forms.ThresholdInTwelveMonthsFormProvider
import identifiers.{ThresholdInTwelveMonthsId, ThresholdNextThirtyDaysId, VATRegistrationExceptionId, VoluntaryRegistrationId}
import models.{ConditionalDateFormElement, NormalMode, RegistrationInformation}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.TrafficManagementService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import views.html.thresholdInTwelveMonths

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ThresholdInTwelveMonthsController @Inject()(mcc: MessagesControllerComponents,
                                                  dataCacheConnector: DataCacheConnector,
                                                  navigator: Navigator,
                                                  identify: CacheIdentifierAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  formProvider: ThresholdInTwelveMonthsFormProvider,
                                                  trafficManagementService: TrafficManagementService,
                                                  view: thresholdInTwelveMonths
                                                 )(implicit appConfig: FrontendAppConfig,
                                                   executionContext: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.thresholdInTwelveMonths match {
        case None => formProvider()
        case Some(value) => formProvider().fill(value)
      }
      Ok(view(preparedForm, NormalMode))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, NormalMode))),
        formValue =>
          dataCacheConnector.save[ConditionalDateFormElement](request.internalId, ThresholdInTwelveMonthsId.toString, formValue).flatMap { _ =>
            if (formValue.value) {
              dataCacheConnector.removeEntry(request.internalId, VoluntaryRegistrationId.toString).flatMap {
                _ => dataCacheConnector.removeEntry(request.internalId, ThresholdNextThirtyDaysId.toString)
              }
            } else {
              dataCacheConnector.removeEntry(request.internalId, VATRegistrationExceptionId.toString)
            }
          }.flatMap(cacheMap =>
            trafficManagementService.upsertRegistrationInformation(request.internalId, request.currentProfile.registrationID, isOtrs = false).map {
              case RegistrationInformation(_, _, _, _, _) =>
                Redirect(navigator.nextPage(ThresholdInTwelveMonthsId, NormalMode)(new UserAnswers(cacheMap)))
            }
          ))
  }
}
