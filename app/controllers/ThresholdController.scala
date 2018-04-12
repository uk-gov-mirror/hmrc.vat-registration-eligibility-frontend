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

import config.AuthClientConnector
import forms.{ExpectationThresholdForm, OverThresholdFormFactory}
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.view.{ExpectationOverThresholdView, OverThresholdView}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services._
import utils.SessionProfile

class ThresholdControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val authConnector: AuthClientConnector,
                                        val currentProfileService: CurrentProfileService,
                                        val thresholdService: ThresholdService) extends ThresholdController

trait ThresholdController extends VatRegistrationController with SessionProfile {
  val thresholdService: ThresholdService

  def goneOverShow: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDate { date =>
          for {
            view         <- thresholdService.getThresholdViewModel[OverThresholdView]
            vatThreshold <- thresholdService.fetchCurrentVatThreshold
          } yield {
            val incorpDate = date.format(FORMAT_DD_MMMM_Y)
            val form = OverThresholdFormFactory.form(date, vatThreshold)
            Ok(views.html.pages.over_threshold(view.fold(form)(form.fill), incorpDate, vatThreshold))
          }
        }
  }

  def goneOverSubmit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDate { date =>
          thresholdService.fetchCurrentVatThreshold.flatMap { threshold =>
            OverThresholdFormFactory.form(date, threshold).bindFromRequest().fold(
              badForm => thresholdService.fetchCurrentVatThreshold.map { threshold =>
                BadRequest(views.html.pages.over_threshold(badForm, date.format(FORMAT_DD_MMMM_Y), threshold))
              },
              data => thresholdService.saveThreshold(data) map {
                _ => Redirect(controllers.routes.ThresholdController.expectationOverShow())
              }
            )
          }
        }
  }

  def expectationOverShow: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDate { date =>
          for {
            view               <- thresholdService.getThresholdViewModel[ExpectationOverThresholdView]
            currentThreshold   <- thresholdService.fetchCurrentVatThreshold
          } yield {
            val form = ExpectationThresholdForm.form(date)
            Ok(views.html.pages.expectation_over_threshold(view.fold(form)(form.fill), currentThreshold))
          }
        }
  }

  def expectationOverSubmit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      hasIncorpDate { date =>
        ExpectationThresholdForm.form(date).bindFromRequest().fold(
          badForm =>
            for {
              currentThreshold   <- thresholdService.fetchCurrentVatThreshold
            } yield {
              BadRequest(views.html.pages.expectation_over_threshold(badForm, currentThreshold))
            },
          data => thresholdService.saveThreshold(data) map {
            _ => Redirect(controllers.routes.ThresholdSummaryController.show())
          }
        )
      }
  }
}
