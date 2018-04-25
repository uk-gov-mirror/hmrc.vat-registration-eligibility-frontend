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

import java.time.LocalDate
import javax.inject.Inject

import config.AuthClientConnector
import forms._
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.view.Threshold
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import services._
import utils.SessionProfile

import scala.concurrent.Future

class ThresholdControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val authConnector: AuthClientConnector,
                                        val currentProfileService: CurrentProfileService,
                                        val thresholdService: ThresholdService,
                                        val vatRegFrontendService: VatRegFrontendService) extends ThresholdController

trait ThresholdController extends VatRegistrationController with SessionProfile {
  val thresholdService: ThresholdService
  val vatRegFrontendService : VatRegFrontendService

  def goneOverTwelveShow: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDate { date =>
          for {
            threshold    <- thresholdService.getThreshold
            vatThreshold <- thresholdService.fetchCurrentVatThreshold
          } yield {
            val incorpDate = date.format(FORMAT_DD_MMMM_Y)
            val form = OverThresholdTwelveMonthsForm.form(date, vatThreshold)
            Ok(views.html.pages.over_threshold_twelve_month(threshold.overThresholdOccuredTwelveMonth.fold(form)(form.fill), incorpDate, vatThreshold))
          }
        }
  }

  def goneOverTwelveSubmit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDate { date =>
          thresholdService.fetchCurrentVatThreshold.flatMap { threshold =>
            OverThresholdTwelveMonthsForm.form(date, threshold).bindFromRequest().fold(
              badForm => thresholdService.fetchCurrentVatThreshold.map { threshold =>
                BadRequest(views.html.pages.over_threshold_twelve_month(badForm, date.format(FORMAT_DD_MMMM_Y), threshold))
              },
              data => thresholdService.saveOverThresholdTwelveMonths(data) map {
                _ => Redirect(controllers.routes.ThresholdSummaryController.show())
              }
            )
          }
        }
  }

  def goneOverSinceIncorpShow: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDateWithinTwelveMonths { date =>
          for {
            threshold    <- thresholdService.getThreshold
            vatThreshold <- thresholdService.fetchCurrentVatThreshold
          } yield {
            val incorpDate = date.format(FORMAT_DD_MMMM_Y)
            val form = OverThresholdSinceIncorpForm.form(incorpDate, vatThreshold)
            Ok(views.html.pages.over_threshold_since_incorp(
              threshold.overThresholdOccuredTwelveMonth.fold(form)(tv => form.fill(tv.selection)), incorpDate, vatThreshold
            ))
          }
        }
  }

  def goneOverSinceIncorpSubmit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDateWithinTwelveMonths { date =>
          thresholdService.fetchCurrentVatThreshold.flatMap { threshold =>
            val incorpDate = date.format(FORMAT_DD_MMMM_Y)
            OverThresholdSinceIncorpForm.form(incorpDate, threshold).bindFromRequest().fold(
              badForm => thresholdService.fetchCurrentVatThreshold.map { threshold =>
                BadRequest(views.html.pages.over_threshold_since_incorp(badForm, date.format(FORMAT_DD_MMMM_Y), threshold))
              },
              data => thresholdService.saveOverThresholdSinceIncorp(data) map {
                _ => Redirect(controllers.routes.ThresholdController.overThresholdThirtyShow())
              }
            )
          }
        }
  }

  def pastThirtyDaysShow: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        hasIncorpDate { date =>
          for {
            threshold          <- thresholdService.getThreshold
            currentThreshold   <- thresholdService.fetchCurrentVatThreshold
          } yield {
            val form = PastThirtyDayPeriodThresholdForm.form(date)
            Ok(views.html.pages.past_thirty_day_period_threshold(threshold.pastOverThresholdThirtyDays.fold(form)(form.fill), currentThreshold))
          }
        }
  }

  def pastThirtyDaysSubmit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      hasIncorpDate { date =>
        PastThirtyDayPeriodThresholdForm.form(date).bindFromRequest().fold(
          badForm =>
            for {
              currentThreshold   <- thresholdService.fetchCurrentVatThreshold
            } yield {
              BadRequest(views.html.pages.past_thirty_day_period_threshold(badForm, currentThreshold))
            },
          data => thresholdService.saveOverThresholdPastThirtyDays(data) map {
            _ => Redirect(controllers.routes.ThresholdController.goneOverTwelveShow())
          }
        )
      }
  }

  def overThresholdThirtyShow: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for {
        threshold <- thresholdService.getThreshold
        vatThreshold <- thresholdService.fetchCurrentVatThreshold
      } yield {
        Ok(
          profile.incorporationDate match {
            case Some(_) =>
              val form = OverThresholdThirtyDaysForm.form(vatThreshold)
              views.html.pages.over_threshold_thirty(threshold.overThresholdThirtyDays.fold(form)(tv => form.fill(tv.selection)), vatThreshold)
            case None       =>
              val form = OverThresholdThirtyDaysPreIncForm.form(vatThreshold)
              views.html.pages.over_threshold_thirty_preincorp(threshold.overThresholdThirtyDaysPreIncorp.fold(form)(tt => form.fill(tt)), vatThreshold)
          }
        )
      }
  }

  private def incorpDateToRedirectLocation(incorpDate : Option[LocalDate], threshold : Threshold) : Result = incorpDate match {
    case Some(id) if id.isBefore(LocalDate.now().minusYears(1)) => Redirect(routes.ThresholdController.pastThirtyDaysShow())
    case _ =>
      (
        threshold.overThresholdOccuredTwelveMonth.exists(_.selection),
        threshold.overThresholdThirtyDays.exists(_.selection),
        threshold.overThresholdThirtyDaysPreIncorp.contains(true)
      ) match {
        case (false, false, false) => Redirect(controllers.routes.VoluntaryRegistrationController.show())
        case _                     => Redirect(vatRegFrontendService.buildVatRegFrontendUrlEntry)
      }
  }

  def overThresholdThirtySubmit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        thresholdService.fetchCurrentVatThreshold.flatMap { threshold =>
          val (form, badRequest, saveMethod) = profile.incorporationDate match {
            case Some(_) => (
              OverThresholdThirtyDaysForm.form(threshold),
              (badForm : Form[Boolean]) => views.html.pages.over_threshold_thirty(badForm, threshold),
              thresholdService.saveOverThresholdThirtyDays _
            )
            case _       =>  (
              OverThresholdThirtyDaysPreIncForm.form(threshold),
              (badForm : Form[Boolean]) => views.html.pages.over_threshold_thirty_preincorp(badForm, threshold),
              thresholdService.saveOverThresholdThirtyDaysPreIncorp _
            )
          }
          form.bindFromRequest().fold(
            badForm => Future.successful(BadRequest(badRequest(badForm))),
            data => saveMethod(data) map {
              th => incorpDateToRedirectLocation(profile.incorporationDate, th)
            }
          )
        }
  }
}
