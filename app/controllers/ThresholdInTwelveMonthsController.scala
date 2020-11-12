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

import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions._
import forms.ThresholdInTwelveMonthsFormProvider
import identifiers.ThresholdInTwelveMonthsId
import javax.inject.Inject
import models.{ConditionalDateFormElement, NormalMode, RegistrationInformation}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import services.{ThresholdService, TrafficManagementService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import views.html.thresholdInTwelveMonths

import scala.concurrent.{ExecutionContext, Future}

class ThresholdInTwelveMonthsController @Inject()(mcc: MessagesControllerComponents,
                                                  dataCacheConnector: DataCacheConnector,
                                                  navigator: Navigator,
                                                  identify: CacheIdentifierAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  thresholdService: ThresholdService,
                                                  formProvider: ThresholdInTwelveMonthsFormProvider,
                                                  trafficManagementService: TrafficManagementService
                                                 )(implicit appConfig: FrontendAppConfig,
                                                   executionContext: ExecutionContext) extends FrontendController(mcc) with I18nSupport {


  def onPageLoad() = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.thresholdInTwelveMonths match {
        case None => formProvider()
        case Some(value) => formProvider().fill(value)
      }
      Ok(thresholdInTwelveMonths(preparedForm, NormalMode, thresholdService))
  }

  def onSubmit() = (identify andThen getData andThen requireData).async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(thresholdInTwelveMonths(formWithErrors, NormalMode, thresholdService))),
        (formValue) =>
          dataCacheConnector.save[ConditionalDateFormElement](request.internalId, ThresholdInTwelveMonthsId.toString, formValue).flatMap { cacheMap =>
            if (formValue.value) thresholdService.removeVoluntaryAndNextThirtyDays else thresholdService.removeException
          }.flatMap(cMap =>
            trafficManagementService.upsertRegistrationInformation(request.internalId, request.currentProfile.registrationID, isOtrs = false, isSubmitted = false).map {
              case RegistrationInformation(_, _, _, _, _) =>
                Redirect(navigator.nextPage(ThresholdInTwelveMonthsId, NormalMode)(new UserAnswers(cMap)))
            }
          ))
  }
}
