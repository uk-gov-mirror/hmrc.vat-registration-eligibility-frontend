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
import identifiers.{CompletionCapacityFillingInForId, CompletionCapacityId}
import javax.inject.Inject
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import services.IncorporationInformationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.completionCapacity

import scala.concurrent.Future

class CompletionCapacityController @Inject()(
                                              appConfig: FrontendAppConfig,
                                              override val messagesApi: MessagesApi,
                                              dataCacheConnector: DataCacheConnector,
                                              navigator: Navigator,
                                              identify: CacheIdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              iiService : IncorporationInformationService,
                                              formProvider: CompletionCapacityFormProvider) extends FrontendController with I18nSupport with Enumerable.Implicits {

  val form = formProvider(CompletionCapacityId)_

  def onPageLoad() = (identify andThen getData andThen requireData).async {
    implicit request =>
      def prepareForm(officers: Seq[Officer]): Form[String] = {
        request.userAnswers.completionCapacity match {
          case None => form(officers)
          case Some(value) => form(officers).fill(value)
        }
      }

      iiService.getOfficerList(request.currentProfile.transactionID) map {
        case officer :: Nil =>
          Ok(completionCapacity(appConfig, prepareForm(List(officer)), NormalMode, CompletionCapacity.singleOfficer(officer), Some(officer.shortName)))
        case officers =>
          Ok(completionCapacity(appConfig, prepareForm(officers), NormalMode, CompletionCapacity.multipleOfficers(officers)))
      }
  }

  def onSubmit() = (identify andThen getData andThen requireData).async {
    implicit request =>
      iiService.getOfficerList(request.currentProfile.transactionID) flatMap { officers =>
        def errForm(form : Form[_]) = officers match {
          case officer :: Nil     => completionCapacity(appConfig, form, NormalMode, CompletionCapacity.singleOfficer(officer), Some(officer.shortName))
          case multipleOfficers   => completionCapacity(appConfig, form, NormalMode, CompletionCapacity.multipleOfficers(multipleOfficers))
        }

        form(officers).bindFromRequest().fold(
          (formWithErrors: Form[_]) => Future.successful(BadRequest(errForm(formWithErrors))),
          value =>
            dataCacheConnector.save[String](request.internalId, CompletionCapacityId.toString, value).flatMap{cacheMap =>
              def redirectToLocation = Redirect(navigator.nextPage(CompletionCapacityId, NormalMode)(new UserAnswers(cacheMap)))
              value match {
                case ("noneOfThese" | "no") => Future.successful(redirectToLocation)
                case _ => dataCacheConnector.removeEntry(request.internalId, CompletionCapacityFillingInForId.toString).map(_ => redirectToLocation)
              }
        }
        )
      }
  }

}
