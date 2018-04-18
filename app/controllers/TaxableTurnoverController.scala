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
import forms.TaxableTurnoverForm
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services._
import utils.SessionProfile

import scala.concurrent.Future

class TaxableTurnoverControllerImpl @Inject()(val authConnector: AuthClientConnector,
                                              val messagesApi: MessagesApi,
                                              val vrs: VatRegistrationService,
                                              val currentProfileService: CurrentProfileService,
                                              val vatRegFrontendService: VatRegFrontendService,
                                              val thresholdService: ThresholdService) extends TaxableTurnoverController

trait TaxableTurnoverController extends VatRegistrationController with SessionProfile {
  val thresholdService: ThresholdService
  val vatRegFrontendService: VatRegFrontendService

  def form(vatThreshold: String): Form[Boolean] = TaxableTurnoverForm.form(vatThreshold)

  def show: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for{
        threshold      <- thresholdService.getThreshold
        currentThreshold <- thresholdService.fetchCurrentVatThreshold
        form       = TaxableTurnoverForm.form(currentThreshold)
      } yield {
        Ok(views.html.pages.taxable_turnover(threshold.taxableTurnover.fold(form)(form.fill), currentThreshold))
      }
  }

  def submit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      thresholdService.fetchCurrentVatThreshold.flatMap{ threshold =>
        form(threshold).bindFromRequest().fold(
          badForm => Future.successful(BadRequest(views.html.pages.taxable_turnover(badForm, threshold))),
          taxableTurnover    => thresholdService.saveTaxableTurnover(taxableTurnover) map { _ =>
            if (taxableTurnover) {
              Redirect(vatRegFrontendService.buildVatRegFrontendUrlEntry)
            } else {
              Redirect(controllers.routes.VoluntaryRegistrationController.show())
            }
          }
        )
    }
  }
}
