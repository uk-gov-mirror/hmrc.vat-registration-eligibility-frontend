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
import controllers.actions._
import identifiers._
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.{agriculturalDropout, eligibilityDropout, internationalActivityDropout}

class EligibilityDropoutController @Inject()(mcc: MessagesControllerComponents,
                                             identify: CacheIdentifierAction
                                            )(implicit appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport {

  def onPageLoad(mode: String) = identify {
    implicit request =>
      mode match {
        case InternationalActivitiesId.toString => Ok(internationalActivityDropout())
        case AgriculturalFlatRateSchemeId.toString => Ok(agriculturalDropout())
        case _ => Ok(eligibilityDropout())
      }
  }

  def onSubmit: Action[AnyContent] = Action { implicit request =>
    Redirect(controllers.routes.EligibilityDropoutController.onPageLoad(""))
  }
}
