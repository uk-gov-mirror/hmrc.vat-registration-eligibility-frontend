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

import java.time.LocalDate
import config.FrontendAppConfig
import connectors.{Allocated, DataCacheConnector, QuotaReached}
import controllers.actions._
import featureswitch.core.config.{FeatureSwitching, TrafficManagement}
import forms.NinoFormProvider
import identifiers.NinoId

import javax.inject.{Inject, Singleton}
import models.{Draft, NormalMode, RegistrationInformation, VatReg}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{S4LService, TrafficManagementService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import views.html.nino

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NinoController @Inject()(mcc: MessagesControllerComponents,
                               dataCacheConnector: DataCacheConnector,
                               s4LService: S4LService,
                               navigator: Navigator,
                               identify: CacheIdentifierAction,
                               getData: DataRetrievalAction,
                               requireData: DataRequiredAction,
                               formProvider: NinoFormProvider,
                               trafficManagementService: TrafficManagementService,
                               view: nino
                              )(implicit appConfig: FrontendAppConfig,
                                executionContext: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.nino match {
        case None => formProvider()
        case Some(value) => formProvider().fill(value)
      }
      Ok(view(preparedForm, NormalMode))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      implicit val profile = request.currentProfile
      formProvider().bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, NormalMode))),
        value =>
          dataCacheConnector.save[Boolean](request.internalId, NinoId.toString, value).flatMap { cacheMap =>
            if (!value) {
              Future.successful(Redirect(controllers.routes.VATExceptionKickoutController.onPageLoad()))
            }
            else {
              if (isEnabled(TrafficManagement)) {
                trafficManagementService.getRegistrationInformation flatMap {
                  case Some(RegistrationInformation(_, _, Draft, Some(date), VatReg)) if date == LocalDate.now =>
                    Future.successful(Redirect(navigator.nextPage(NinoId, NormalMode)(new UserAnswers(cacheMap))))
                  case _ =>
                    trafficManagementService.allocate(request.currentProfile.registrationID) flatMap {
                      case Allocated =>
                        s4LService.save("eligibility-data", Json.toJson(cacheMap)) map { _ =>
                          Redirect(navigator.nextPage(NinoId, NormalMode)(new UserAnswers(cacheMap)))
                        }
                      case QuotaReached =>
                        Future.successful(Redirect(controllers.routes.VATExceptionKickoutController.onPageLoad()))
                    }
                }
              }
              else {
                trafficManagementService.upsertRegistrationInformation(
                  internalId = request.internalId,
                  regId = request.currentProfile.registrationID,
                  isOtrs = false
                ) map (_ => Redirect(navigator.nextPage(NinoId, NormalMode)(new UserAnswers(cacheMap))))
              }
            }
          }
      )
  }

}
