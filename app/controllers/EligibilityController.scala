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

import javax.inject.Inject

import common.enums.CacheKeys.IneligibilityReason
import common.enums.EligibilityQuestions
import config.AuthClientConnector
import connectors.KeystoreConnector
import forms.ServiceCriteriaFormFactory
import models.CurrentProfile
import models.view.YesOrNoQuestion
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.{CurrentProfileService, EligibilityService, ThresholdService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SessionProfile

import scala.concurrent.Future


class EligibilityControllerImpl @Inject()(val keystoreConnector: KeystoreConnector,
                                          val currentProfileService: CurrentProfileService,
                                          val eligibilityService: EligibilityService,
                                          val thresholdService: ThresholdService,
                                          val messagesApi: MessagesApi,
                                          val authConnector: AuthClientConnector) extends EligibilityController{}

trait EligibilityController extends VatRegistrationController with SessionProfile {
  val keystoreConnector: KeystoreConnector
  val eligibilityService: EligibilityService
  val thresholdService: ThresholdService

  private def submitQuestion(question: EligibilityQuestions.Value, newValue: Boolean, exitCondition: Boolean)(success: => Result, fail: => Result)
                            (implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[Result] = {
    eligibilityService.saveEligibility(YesOrNoQuestion(question, newValue)) flatMap { _ =>
      if(exitCondition) {
        keystoreConnector.cache(IneligibilityReason.toString, question.toString) map (_ => fail)
      } else {
        Future.successful(success)
      }
    }
  }

  private def fillYesNoQuestionForm(question: EligibilityQuestions.Value, optBoolean: Option[Boolean]): Form[YesOrNoQuestion] = {
    val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(question)
    optBoolean.fold(form)(x => form.fill(YesOrNoQuestion(question, x)))
  }

  def ineligible(): Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      keystoreConnector.fetchAndGet[String](IneligibilityReason.toString) flatMap {
        case None         => Future.successful(InternalServerError)
        case Some(v)      => thresholdService.fetchCurrentVatThreshold.map{
          case threshold if v == EligibilityQuestions.applyingForVatExemption.toString =>
            Ok(views.html.pages.ineligible.exemption_ineligible(threshold))
          case threshold =>
            Ok(views.html.pages.ineligible.ineligible(v.toString, threshold))
        }
      }
  }

  def showHaveNino : Action[AnyContent] = isAuthenticatedWithProfile{
    implicit request => implicit profile =>
      for {
        eligibility <- eligibilityService.getEligibility
        formFilled  =  fillYesNoQuestionForm(EligibilityQuestions.haveNino, eligibility.haveNino)
      } yield Ok(views.html.pages.have_nino(formFilled))
  }

  def submitHaveNino: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      ServiceCriteriaFormFactory.form(EligibilityQuestions.haveNino).bindFromRequest.fold(
        hasErrors => Future.successful(BadRequest(views.html.pages.have_nino(hasErrors))),
        data => submitQuestion(EligibilityQuestions.haveNino, data.answer, !data.answer)(
          success = Redirect(controllers.routes.EligibilityController.showDoingBusinessAbroad()),
          fail    = Redirect(controllers.routes.EligibilityController.ineligible())
        )
      )
  }

  def showDoingBusinessAbroad : Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for {
        eligibility <- eligibilityService.getEligibility
        formFilled = fillYesNoQuestionForm(EligibilityQuestions.doingBusinessAbroad, eligibility.doingBusinessAbroad)
      } yield Ok(views.html.pages.doing_business_abroad(formFilled))
  }

  def submitDoingBusinessAbroad: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      ServiceCriteriaFormFactory.form(EligibilityQuestions.doingBusinessAbroad).bindFromRequest.fold(
        hasErrors => Future.successful(BadRequest(views.html.pages.doing_business_abroad(hasErrors))),
        data => submitQuestion(EligibilityQuestions.doingBusinessAbroad, data.answer, data.answer)(
          success = Redirect(controllers.routes.EligibilityController.showDoAnyApplyToYou()),
          fail    = Redirect(controllers.routes.EligibilityController.ineligible())
        )
      )
  }

  def showDoAnyApplyToYou : Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for {
        eligibility <- eligibilityService.getEligibility
        formFilled  =  fillYesNoQuestionForm(EligibilityQuestions.doAnyApplyToYou, eligibility.doAnyApplyToYou)
      } yield Ok(views.html.pages.do_any_apply_to_you(formFilled))
  }

  def submitDoAnyApplyToYou: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      ServiceCriteriaFormFactory.form(EligibilityQuestions.doAnyApplyToYou).bindFromRequest.fold(
        hasErrors => Future.successful(BadRequest(views.html.pages.do_any_apply_to_you(hasErrors))),
        data => submitQuestion(EligibilityQuestions.doAnyApplyToYou, data.answer, data.answer)(
          success = Redirect(controllers.routes.EligibilityController.showApplyingForAnyOf()),
          fail    = Redirect(controllers.routes.EligibilityController.ineligible())
        )
      )
  }

  def showApplyingForAnyOf : Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for {
        eligibility <- eligibilityService.getEligibility
        formFilled  =  fillYesNoQuestionForm(EligibilityQuestions.applyingForAnyOf, eligibility.applyingForAnyOf)
      } yield Ok(views.html.pages.applying_for_any_of(formFilled))
  }

  def submitApplyingForAnyOf: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      ServiceCriteriaFormFactory.form(EligibilityQuestions.applyingForAnyOf).bindFromRequest.fold(
        hasErrors => Future.successful(BadRequest(views.html.pages.applying_for_any_of(hasErrors))),
        data => submitQuestion(EligibilityQuestions.applyingForAnyOf, data.answer, data.answer)(
          success = Redirect(controllers.routes.EligibilityController.showExemptionCriteria()),
          fail    = Redirect(controllers.routes.EligibilityController.ineligible())
        )
      )
  }

  def showCompanyWillDoAnyOf : Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for {
        eligibility <- eligibilityService.getEligibility
        formFilled  =  fillYesNoQuestionForm(EligibilityQuestions.companyWillDoAnyOf, eligibility.companyWillDoAnyOf)
      } yield Ok(views.html.pages.company_will_do_any_of(formFilled))
  }

  def submitCompanyWillDoAnyOf: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      ServiceCriteriaFormFactory.form(EligibilityQuestions.companyWillDoAnyOf).bindFromRequest.fold(
        hasErrors => Future.successful(BadRequest(views.html.pages.company_will_do_any_of(hasErrors))),
        data => submitQuestion(EligibilityQuestions.companyWillDoAnyOf, data.answer, data.answer)(
          success = Redirect(controllers.routes.EligibilitySummaryController.show()),
          fail    = Redirect(controllers.routes.EligibilityController.ineligible())
        )
      )
  }

  def showExemptionCriteria : Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for {
        eligibility <- eligibilityService.getEligibility
        formFilled  =  fillYesNoQuestionForm(EligibilityQuestions.applyingForVatExemption, eligibility.applyingForVatExemption)
        vatThreshold <- thresholdService.fetchCurrentVatThreshold
      } yield Ok(views.html.pages.applying_for_vat_exemption(formFilled, vatThreshold))
  }

  def submitExemptionCriteria: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      ServiceCriteriaFormFactory.form(EligibilityQuestions.applyingForVatExemption).bindFromRequest.fold(
        hasErrors => thresholdService.fetchCurrentVatThreshold.map{ threshold =>
          BadRequest(views.html.pages.applying_for_vat_exemption(hasErrors, threshold))
        },
        data => submitQuestion(EligibilityQuestions.applyingForVatExemption, data.answer, data.answer)(
          success = Redirect(controllers.routes.EligibilityController.showCompanyWillDoAnyOf()),
          fail    = Redirect(controllers.routes.EligibilityController.ineligible())
        )
      )
  }
}


