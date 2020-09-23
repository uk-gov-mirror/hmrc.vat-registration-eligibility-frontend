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
import forms.BusinessEntityFormProvider
import identifiers.BusinessEntityId
import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import views.html.businessEntity

import scala.concurrent.Future

class BusinessEntityController @Inject()(override val messagesApi: MessagesApi,
                                         dataCacheConnector: DataCacheConnector,
                                         navigator: Navigator,
                                         identify: CacheIdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: BusinessEntityFormProvider
                                        )(implicit appConfig: FrontendAppConfig) extends FrontendController with I18nSupport {


  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.businessEntity match {
        case None => formProvider()
        case Some(businessEntity) => formProvider().fill(businessEntity)
      }
      Ok(businessEntity(preparedForm, controllers.routes.BusinessEntityController.onSubmit()))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    formProvider().bindFromRequest.fold(
      formWithErrors =>
        Future.successful(
          BadRequest(businessEntity(formWithErrors, routes.BusinessEntityController.onSubmit()))
        ),
      entityType => {
        dataCacheConnector.save[BusinessEntity](request.internalId, BusinessEntityId.toString, entityType) map { cacheMap =>
          Redirect(navigator.nextPage(BusinessEntityId, NormalMode)(new UserAnswers(cacheMap)))
        }
      }
    )
  }
}