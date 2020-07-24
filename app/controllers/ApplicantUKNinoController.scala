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
import forms.ApplicantUKNinoFormProvider
import identifiers.ApplicantUKNinoId
import javax.inject.Inject
import models.{ConditionalNinoFormElement, NormalMode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import services.{IncorporationInformationService, VatRegistrationService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import views.html.applicantUKNino

import scala.concurrent.{ExecutionContext, Future}

class ApplicantUKNinoController @Inject()(appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          dataCacheConnector: DataCacheConnector,
                                          identify: CacheIdentifierAction,
                                          getData: DataRetrievalAction,
                                          navigator: Navigator,
                                          requireData: DataRequiredAction,
                                          iiService: IncorporationInformationService,
                                          formProvider: ApplicantUKNinoFormProvider,
                                          vatRegistrationService: VatRegistrationService) extends FrontendController with I18nSupport {
  val frontendUrl       = s"${appConfig.vatRegFEURL}${appConfig.vatRegFEURI}${appConfig.vatRegFEFirstPage}"

  def onPageLoad() = (identify andThen getData andThen requireData).async {
    implicit request =>
      iiService.getOfficerList(request.currentProfile.transactionID).map { officers =>
        val shortName = request.userAnswers.completionCapacityFillingInFor.flatMap( id => officers.find(_.generateId == id).map(_.shortName))
        val preparedForm = request.userAnswers.applicantUKNino match {
          case None => formProvider(shortName)
          case Some(value) => formProvider(shortName).fill(value)
        }
        Ok(applicantUKNino(appConfig, preparedForm, NormalMode, shortName))
      }
  }

  def onSubmit() = (identify andThen getData andThen requireData).async {
    implicit request =>
      iiService.getOfficerList(request.currentProfile.transactionID).flatMap { officers =>
        val shortName = request.userAnswers.completionCapacityFillingInFor.flatMap(id => officers.find(_.generateId == id).map(_.shortName))
        formProvider(shortName).bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            Future.successful(BadRequest(applicantUKNino(appConfig, formWithErrors, NormalMode, shortName))),
          (value) => dataCacheConnector.save[ConditionalNinoFormElement](request.internalId, ApplicantUKNinoId.toString, value).flatMap { cacheMap =>
            if (value.value) {
              vatRegistrationService.submitEligibility(request.internalId)(hc, implicitly[ExecutionContext], request)
                .map(_ => Redirect(frontendUrl))
            } else {
              Future.successful(Redirect(navigator.nextPage(ApplicantUKNinoId, NormalMode)(new UserAnswers(cacheMap))))
            }
          }
        )
      }
  }
}