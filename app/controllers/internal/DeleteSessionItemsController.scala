/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.internal

import javax.inject.Inject

import config.FrontendAuthConnector
import connectors.{KeystoreConnector, S4LConnector}
import controllers.VatRegistrationController
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{CancellationService, VatRegistrationService}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

class DeleteSessionItemsController @Inject()(val messagesApi: MessagesApi,
                                             val cancelService: CancellationService) extends VatRegistrationController {

  def deleteSessionRelatedData(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
        implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
        FrontendAuthConnector.currentAuthority flatMap (_.fold {
          Logger.warn(s"[DeleteSessionItemsController][deleteSessionRelatedData] - cant get Authority")
          Future.successful(Unauthorized)
        }{ _ =>
          cancelService.deleteEligibilityData(regId) map (result => if(result) Ok else BadRequest)
        }) recover {
          case ex: Exception =>
            Logger.error(s"[DeleteSessionItemsController][deleteSessionRelatedData] - recieved and error on recieving the authority - error: ${ex.getMessage}")
            InternalServerError
        }
  }
}
