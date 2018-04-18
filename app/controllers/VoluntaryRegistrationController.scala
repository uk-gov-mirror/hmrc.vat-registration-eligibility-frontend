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
import connectors.S4LConnector
import forms.VoluntaryRegistrationForm
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CurrentProfileService, ThresholdService, VatRegFrontendService}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import utils.SessionProfile

import scala.concurrent.Future

class VoluntaryRegistrationControllerImpl @Inject()(val messagesApi: MessagesApi,
                                                    val authConnector: AuthClientConnector,
                                                    val currentProfileService: CurrentProfileService,
                                                    val vatRegFrontendService: VatRegFrontendService,
                                                    val thresholdService: ThresholdService,
                                                    val s4LConnector : S4LConnector,
                                                    config: ServicesConfig) extends VoluntaryRegistrationController{
  lazy val compRegFEURL = config.getConfString("company-registration-frontend.www.url", "")
  lazy val compRegFEURI = config.getConfString("company-registration-frontend.www.uri", "")
  lazy val compRegFECompanyRegistrationOverview = config.getConfString("company-registration-frontend.www.company-registration-overview", "")}


trait VoluntaryRegistrationController extends VatRegistrationController with SessionProfile {
  val compRegFEURL: String
  val compRegFEURI: String
  val compRegFECompanyRegistrationOverview: String
  val thresholdService: ThresholdService
  val vatRegFrontendService: VatRegFrontendService
  val s4LConnector : S4LConnector

  val form = VoluntaryRegistrationForm.form

  def show: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      thresholdService.getThreshold map { threshold =>
        Ok(views.html.pages.voluntary_registration(threshold.voluntaryRegistration.fold(form)(form.fill)))
      }
  }

  def submit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      form.bindFromRequest().fold(
        badForm => Future.successful(BadRequest(views.html.pages.voluntary_registration(badForm))),
        voluntary    => thresholdService.saveVoluntaryRegistration(voluntary) map { _ =>
          if (voluntary) {
            Redirect(controllers.routes.VoluntaryRegistrationReasonController.show())
          } else {
            Redirect(controllers.routes.VoluntaryRegistrationController.showChoseNoToVoluntary())
          }
        }
      )
  }

  def showChoseNoToVoluntary: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
        Future.successful(Ok(views.html.pages.chose_no_to_voluntary_registration()))
    }


  def showClearS4lRedirectDashboard: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      s4LConnector.clear(profile.registrationId).map(_ => Redirect(s"$compRegFEURL$compRegFEURI$compRegFECompanyRegistrationOverview"))
  }
}

