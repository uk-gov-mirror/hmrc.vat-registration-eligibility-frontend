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
import controllers.actions.{CacheIdentifierAction, DataRequiredAction, DataRetrievalAction}
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.VatRegistrationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.eligible

import scala.concurrent.ExecutionContext

class EligibleController @Inject()(override val messagesApi: MessagesApi,
                                   identify: CacheIdentifierAction,
                                   getData: DataRetrievalAction,
                                   requireData: DataRequiredAction,
                                   vatRegistrationService: VatRegistrationService
                                  )(implicit appConfig: FrontendAppConfig) extends FrontendController with I18nSupport {

  val frontendUrl = s"${appConfig.vatRegFEURL}${appConfig.vatRegFEURI}${appConfig.vatRegFEFirstPage}"

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Ok(eligible())
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    vatRegistrationService.submitEligibility(request.internalId)(hc, implicitly[ExecutionContext], request) map { _ =>
      Redirect(frontendUrl)
    }
  }

}
