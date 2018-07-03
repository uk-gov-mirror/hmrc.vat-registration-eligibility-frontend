/*
 * Copyright 2018 HM Revenue & Customs
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
import forms.CompletionCapacityFormProvider
import identifiers.CompletionCapacityFillingInForId
import javax.inject.Inject
import models.{CompletionCapacity, NormalMode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import services.IncorporationInformationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.{completionCapacityFillingInFor, confirmDoingOnBehalfOfOfficer}

import scala.concurrent.Future

class CompletionCapacityFillingInForController @Inject()(
                                        appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        dataCacheConnector: DataCacheConnector,
                                        navigator: Navigator,
                                        identify: CacheIdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: CompletionCapacityFormProvider,
                                        iiService: IncorporationInformationService) extends FrontendController with I18nSupport with Enumerable.Implicits {

  val form = formProvider(CompletionCapacityFillingInForId) _

  def onPageLoad() = (identify andThen getData andThen requireData).async {
    implicit request =>
      iiService.getOfficerList(request.currentProfile.transactionID) map {
        case officer :: Nil =>
          Ok(confirmDoingOnBehalfOfOfficer(appConfig, officer.shortName))
        case officers =>
          val preparedForm = request.userAnswers.completionCapacityFillingInFor match {
            case None => form(officers)
            case Some(value) => form(officers).fill(value)
          }
          Ok(completionCapacityFillingInFor(appConfig, preparedForm, NormalMode, CompletionCapacity.multipleOfficers(officers, false)))
      }
  }

  def onSubmit() = (identify andThen getData andThen requireData).async {
    implicit request =>
      iiService.getOfficerList(request.currentProfile.transactionID) flatMap {
        case officer :: Nil =>
          dataCacheConnector.save[String](request.internalId, CompletionCapacityFillingInForId.toString, officer.generateId).map(cacheMap =>
            Redirect(navigator.nextPage(CompletionCapacityFillingInForId, NormalMode)(new UserAnswers(cacheMap))))
        case officers =>
          form(officers).bindFromRequest().fold(
            (formWithErrors: Form[_]) =>
              Future.successful(BadRequest(completionCapacityFillingInFor(appConfig, formWithErrors, NormalMode, CompletionCapacity.multipleOfficers(officers, false)))),
            (value) =>
              dataCacheConnector.save[String](request.internalId, CompletionCapacityFillingInForId.toString, value).map(cacheMap =>
                Redirect(navigator.nextPage(CompletionCapacityFillingInForId, NormalMode)(new UserAnswers(cacheMap))))
          )
      }
  }
}
