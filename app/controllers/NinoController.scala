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
import connectors.{Allocated, DataCacheConnector, QuotaReached}
import controllers.actions._
import featureswitch.core.config.{FeatureSwitching, TrafficManagement}
import forms.NinoFormProvider
import identifiers.NinoId
import javax.inject.Inject
import models.{Draft, NormalMode, RegistrationInformation, VatReg}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.TrafficManagementService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import views.html.nino

import scala.concurrent.{ExecutionContext, Future}

class NinoController @Inject()(mcc: MessagesControllerComponents,
                               dataCacheConnector: DataCacheConnector,
                               navigator: Navigator,
                               identify: CacheIdentifierAction,
                               getData: DataRetrievalAction,
                               requireData: DataRequiredAction,
                               formProvider: NinoFormProvider,
                               trafficManagementService: TrafficManagementService
                              )(implicit appConfig: FrontendAppConfig,
                                executionContext: ExecutionContext) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.nino match {
        case None => form
        case Some(value) => form.fill(value)
      }
      Ok(nino(preparedForm, NormalMode))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(nino(formWithErrors, NormalMode))),
        (value) =>
          dataCacheConnector.save[Boolean](request.internalId, NinoId.toString, value).flatMap(cacheMap =>
            if (!value) {
              Future.successful(Redirect(controllers.routes.VATExceptionKickoutController.onPageLoad()))
            }
            else {
              if (isEnabled(TrafficManagement)) {
                trafficManagementService.getRegistrationInformation flatMap {
                  case Some(RegistrationInformation(_, _, Draft, Some(date), VatReg)) if date == LocalDate.now =>
                    Future.successful(Redirect(navigator.nextPage(NinoId, NormalMode)(new UserAnswers(cacheMap))))
                  case _ =>
                    trafficManagementService.allocate(request.currentProfile.registrationID) map {
                      case Allocated =>
                        Redirect(navigator.nextPage(NinoId, NormalMode)(new UserAnswers(cacheMap)))
                      case QuotaReached =>
                        Redirect(controllers.routes.VATExceptionKickoutController.onPageLoad())
                    }
                }
              }
              else {
                Future.successful(Redirect(navigator.nextPage(NinoId, NormalMode)(new UserAnswers(cacheMap))))
              }
            }
          )
      )
  }

}
