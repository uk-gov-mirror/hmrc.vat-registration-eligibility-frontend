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

import java.io.File
import javax.inject.Inject

import config.AuthClientConnector
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.CurrentProfileService
import utils.SessionProfile
import views.html.pages.error.TimeoutView

import scala.concurrent.Future

class SessionControllerImpl @Inject()(val messagesApi: MessagesApi,
                                      val authConnector: AuthClientConnector,
                                      val currentProfileService: CurrentProfileService) extends SessionController

trait SessionController extends VatRegistrationController with SessionProfile {

  def renewSession: Action[AnyContent] = isAuthenticated {
    implicit request =>
      Future.successful(Ok.sendFile(new File("conf/renewSession.jpg")).as("image/jpeg"))
  }

  def destroySession: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Redirect(routes.SessionController.timeoutShow()).withNewSession)
  }

  def timeoutShow = Action.async {
    implicit request =>
      Future.successful(Ok(TimeoutView()))
  }
}
