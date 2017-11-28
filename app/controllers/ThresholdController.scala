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

import cats.syntax.FlatMapSyntax
import forms.{ExpectationThresholdForm, OverThresholdFormFactory}
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.hasIncorpDate
import models.view.{ExpectationOverThresholdView, OverThresholdView}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services._
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class ThresholdController @Inject()(implicit val messagesApi: MessagesApi,
                                    implicit val s4LService: S4LService,
                                    implicit val vrs: VatRegistrationService,
                                    val currentProfileService: CurrentProfileService,
                                    val eligibilityService: EligibilityService)
  extends VatRegistrationController with FlatMapSyntax with SessionProfile {

  def goneOverShow: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => {
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            eligibilityService.getEligibilityChoice map { choice =>
              Ok(views.html.pages.over_threshold(
                choice.overThreshold.fold(OverThresholdFormFactory.form(incorpDate))(OverThresholdFormFactory.form(incorpDate).fill),
                incorpDate.format(FORMAT_DD_MMMM_Y)
              ))
            }
          }
        }
      }
  }

  def goneOverSubmit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            OverThresholdFormFactory.form(incorpDate).bindFromRequest().fold(
              badForm => BadRequest(views.html.pages.over_threshold(badForm, incorpDate.format(FORMAT_DD_MMMM_Y))).pure,
              data => eligibilityService.saveChoiceQuestion(data) map (_ => Redirect(controllers.routes.ThresholdController.expectationOverShow()))
            )
          }
        }
  }

  def expectationOverShow: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            eligibilityService.getEligibilityChoice map { choice =>
              Ok(views.html.pages.expectation_over_threshold(
                choice.expectationOverThreshold.fold(ExpectationThresholdForm.form(incorpDate))(ExpectationThresholdForm.form(incorpDate).fill)
              ))
            }
          }
        }
  }

  def expectationOverSubmit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            ExpectationThresholdForm.form(incorpDate).bindFromRequest().fold(
              badForm => BadRequest(views.html.pages.expectation_over_threshold(badForm)).pure,
              data => eligibilityService.saveChoiceQuestion(data) map (_ => Redirect(controllers.routes.ThresholdSummaryController.show()))
            )
          }
        }
  }
}
