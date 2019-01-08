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

import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions._
import forms.InvolvedInOtherBusinessFormProvider
import identifiers.InvolvedInOtherBusinessId
import javax.inject.Inject
import models.NormalMode
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import services.IncorporationInformationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import views.html.involvedInOtherBusiness

import scala.concurrent.Future

class InvolvedInOtherBusinessController @Inject()(appConfig: FrontendAppConfig,
                                                  override val messagesApi: MessagesApi,
                                                  dataCacheConnector: DataCacheConnector,
                                                  navigator: Navigator,
                                                  identify: CacheIdentifierAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  formProvider: InvolvedInOtherBusinessFormProvider,
                                                  iiService: IncorporationInformationService) extends FrontendController with I18nSupport {


  private def retrieveFillingInForName(implicit request: DataRequest[_]): Future[Option[String]] =
    iiService.getOfficerList(request.currentProfile.transactionID).map { officers =>
      request.userAnswers.completionCapacityFillingInFor.flatMap(id => officers.find(_.generateId == id).map(_.shortName))
    }

  def onPageLoad() = (identify andThen getData andThen requireData).async {
    implicit request =>
      retrieveFillingInForName.map { shortName =>
        val preparedForm = request.userAnswers.involvedInOtherBusiness match {
          case None => formProvider.form(shortName)
          case Some(value) => formProvider.form(shortName).fill(value)
        }
        Ok(involvedInOtherBusiness(appConfig, preparedForm, NormalMode, shortName))
      }
  }

  def onSubmit() = (identify andThen getData andThen requireData).async {
    implicit request =>
      retrieveFillingInForName.flatMap { shortName =>
        formProvider.form(shortName).bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            Future.successful(BadRequest(involvedInOtherBusiness(appConfig, formWithErrors, NormalMode, shortName))),
          (value) =>
            dataCacheConnector.save[Boolean](request.internalId, InvolvedInOtherBusinessId.toString, value).map(cacheMap =>
              Redirect(navigator.nextPage(InvolvedInOtherBusinessId, NormalMode)(new UserAnswers(cacheMap))))
        )
      }
  }
}
