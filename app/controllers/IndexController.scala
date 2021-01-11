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
import controllers.actions.{CacheIdentifierAction, DataRetrievalAction}
import identifiers.Identifier
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator

import scala.concurrent.ExecutionContext

@Singleton
class IndexController @Inject()(mcc: MessagesControllerComponents,
                                navigator: Navigator,
                                dataCacheConnector: DataCacheConnector,
                                identify: CacheIdentifierAction,
                                getData: DataRetrievalAction
                               )(implicit appConfig: FrontendAppConfig,
                                 executionContext: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData) { implicit request =>
    dataCacheConnector.delete(request.internalId) //TODO Remove as part of SAR-6520

    Redirect(routes.IntroductionController.onPageLoad())
  }

  def navigateToPageId(pageId: String): Action[AnyContent] = Action { implicit request =>
    Redirect(navigator.pageIdToPageLoad(new Identifier {
      override def toString: String = pageId
    }))
  }

}
