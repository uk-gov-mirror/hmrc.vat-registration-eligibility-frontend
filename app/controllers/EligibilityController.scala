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

package controllers

import javax.inject.Inject

import cats.data.OptionT
import common.enums.CacheKeys.IneligibilityReason
import connectors.KeystoreConnector
import forms.ServiceCriteriaFormFactory
import models.YesOrNoQuestion
import models.api.VatServiceEligibility
import models.view.EligibilityQuestion
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{CurrentProfileService, S4LService, VatRegistrationService}
import utils.SessionProfile

import scala.concurrent.Future

class EligibilityController @Inject()(val keystoreConnector: KeystoreConnector,
                                      val currentProfileService: CurrentProfileService,
                                      implicit val messagesApi: MessagesApi,
                                      implicit val vrs: VatRegistrationService,
                                      implicit val s4l: S4LService)
  extends VatRegistrationController with SessionProfile{

  val criteriaName = "applyingForVatExemption"
  def nextPage = controllers.routes.ServiceCriteriaQuestionsController.show("companyWillDoAnyOf")

  def showExemptionCriteria : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          val question = EligibilityQuestion(criteriaName)
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(question.name)
          viewModel[VatServiceEligibility]()
            .flatMap(e => OptionT.fromOption(e.getAnswer(question)))
            .fold(form)(answer => form.fill(YesOrNoQuestion(question.name, answer)))
            .map(f => Ok(views.html.pages.applying_for_vat_exemption(f)))
      }
  }

  def submitExemptionCriteria: Action[AnyContent] = authorised.async {
    implicit  user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          import common.ConditionalFlatMap._
          val question = EligibilityQuestion(criteriaName)
          ServiceCriteriaFormFactory.form(criteriaName).bindFromRequest().fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.applying_for_vat_exemption(hasErrors))),
            data => for {
              vatEligibility  <- viewModel[VatServiceEligibility]().getOrElse(VatServiceEligibility())
              _               <- save(vatEligibility.setAnswer(question, data.answer))
              exit            = data.answer == question.exitAnswer
              _               <- keystoreConnector.cache(IneligibilityReason.toString, question.name) onlyIf exit
            } yield Redirect(if(exit) controllers.routes.ServiceCriteriaQuestionsController.ineligible() else nextPage)
          )
      }
  }
}
