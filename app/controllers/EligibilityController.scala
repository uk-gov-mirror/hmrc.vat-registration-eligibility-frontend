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

import javax.inject.{Inject, Singleton}

import common.enums.CacheKeys.IneligibilityReason
import common.enums.{EligibilityQuestions => Questions}
import connectors.KeystoreConnector
import forms.ServiceCriteriaFormFactory
import models.view.YesOrNoQuestion
import models.CurrentProfile
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.{CurrentProfileService, EligibilityService, VatRegistrationService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class EligibilityController @Inject()(val keystoreConnector: KeystoreConnector,
                                      val currentProfileService: CurrentProfileService,
                                      val eligibilityService: EligibilityService,
                                      implicit val messagesApi: MessagesApi,
                                      implicit val vrs: VatRegistrationService)
  extends VatRegistrationController with SessionProfile {

  private def submitQuestion(question: Questions.Value, newValue: Boolean, exitCondition: Boolean)(success: => Result, fail: => Result)
                            (implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[Result] = {
    eligibilityService.saveQuestion(question, newValue) flatMap { _ =>
      if(exitCondition) {
        keystoreConnector.cache(IneligibilityReason.toString, question.toString) map (_ => fail)
      } else {
        Future.successful(success)
      }
    }
  }

  def ineligible(): Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          keystoreConnector.fetchAndGet[String](IneligibilityReason.toString) map {
            case None => InternalServerError
            case Some(v) if v == Questions.applyingForVatExemption.toString => Ok(views.html.pages.ineligible.exemption_ineligible())
            case Some(v) => Ok(views.html.pages.ineligible.ineligible(v.toString))
          }
        }
  }

  def showHaveNino : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(Questions.haveNino)
          eligibilityService.getEligibility map { eligibility =>
            Ok(views.html.pages.have_nino(
              eligibility.haveNino.fold(form) { v =>
                form.fill(YesOrNoQuestion(Questions.haveNino, v))
              }
            ))
          }
      }
  }

  def submitHaveNino: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.haveNino).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.have_nino(hasErrors))),
            data => submitQuestion(Questions.haveNino, data.answer, !data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showDoingBusinessAbroad()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
      }
  }

  def showDoingBusinessAbroad : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(Questions.doingBusinessAbroad)
          eligibilityService.getEligibility map { eligibility =>
            Ok(views.html.pages.doing_business_abroad(
              eligibility.doingBusinessAbroad.fold(form) { v =>
                form.fill(YesOrNoQuestion(Questions.doingBusinessAbroad, v))
              }
            ))
          }
      }
  }

  def submitDoingBusinessAbroad: Action[AnyContent] = authorised.async {
    implicit  user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.doingBusinessAbroad).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.doing_business_abroad(hasErrors))),
            data => submitQuestion(Questions.doingBusinessAbroad, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showDoAnyApplyToYou()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
      }
  }

  def showDoAnyApplyToYou : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(Questions.doAnyApplyToYou)
          eligibilityService.getEligibility map { eligibility =>
            Ok(views.html.pages.do_any_apply_to_you(
              eligibility.doAnyApplyToYou.fold(form) { v =>
                form.fill(YesOrNoQuestion(Questions.doAnyApplyToYou, v))
              }
            ))
          }
      }
  }

  def submitDoAnyApplyToYou: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.doAnyApplyToYou).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.do_any_apply_to_you(hasErrors))),
            data => submitQuestion(Questions.doAnyApplyToYou, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showApplyingForAnyOf()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
      }
  }

  def showApplyingForAnyOf : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(Questions.applyingForAnyOf)
          eligibilityService.getEligibility map { eligibility =>
            Ok(views.html.pages.applying_for_any_of(
              eligibility.applyingForAnyOf.fold(form) { v =>
                form.fill(YesOrNoQuestion(Questions.applyingForAnyOf, v))
              }
            ))
          }
      }
  }

  def submitApplyingForAnyOf: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.applyingForAnyOf).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.applying_for_any_of(hasErrors))),
            data => submitQuestion(Questions.applyingForAnyOf, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showExemptionCriteria()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
      }
  }

  def showCompanyWillDoAnyOf : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(Questions.companyWillDoAnyOf)
          eligibilityService.getEligibility map { eligibility =>
            Ok(views.html.pages.company_will_do_any_of(
              eligibility.companyWillDoAnyOf.fold(form) { v =>
                form.fill(YesOrNoQuestion(Questions.companyWillDoAnyOf, v))
              }
            ))
          }
      }
  }

  def submitCompanyWillDoAnyOf: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.companyWillDoAnyOf).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.company_will_do_any_of(hasErrors))),
            data => submitQuestion(Questions.companyWillDoAnyOf, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilitySummaryController.show()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
      }
  }

  def showExemptionCriteria : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(Questions.applyingForVatExemption)
          eligibilityService.getEligibility map { eligibility =>
            Ok(views.html.pages.applying_for_vat_exemption(
              eligibility.applyingForVatExemption.fold(form) { v =>
                form.fill(YesOrNoQuestion(Questions.applyingForVatExemption, v))
              }
            ))
          }
      }
  }

  def submitExemptionCriteria: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => withCurrentProfile{
        implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.applyingForVatExemption).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.applying_for_vat_exemption(hasErrors))),
            data => submitQuestion(Questions.applyingForVatExemption, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showCompanyWillDoAnyOf),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
      }
  }
}
