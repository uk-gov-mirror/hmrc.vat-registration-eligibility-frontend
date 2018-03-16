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

import auth.VatExternalUrls
import config.Logging
import models.CurrentProfile
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.CompositePredicate
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.InternalExceptions.{BRDocumentNotFound, VatFootprintNotFound}
import utils.SessionProfile

import scala.concurrent.Future

trait VatRegistrationController extends FrontendController with I18nSupport with Logging with AuthorisedFunctions {
  self: SessionProfile =>

  private val authPredicate = CompositePredicate(AuthProviders(GovernmentGateway), ConfidenceLevel.L50)

  private def handleErrorResult: PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession =>
      Future.successful(Redirect(VatExternalUrls.loginUrl, Map(
        "continue" -> Seq(VatExternalUrls.continueUrl),
        "origin" -> Seq(VatExternalUrls.defaultOrigin)
      )))
    case ae: AuthorisationException =>
      logger.info(s"User is not authorised - reason: ${ae.reason}")
      Future.successful(InternalServerError)
    case e =>
      logger.warn(s"An exception occurred - err: ${e.getMessage}")
      throw e
  }

  def isAuthenticated(f: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(authPredicate)(f(request)) recoverWith handleErrorResult
  }

  def isAuthenticatedWithProfile(f: Request[AnyContent] => CurrentProfile => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(authPredicate) {
        withCurrentProfile { profile =>
          f(request)(profile)
        } recover {
          case _: VatFootprintNotFound | _: BRDocumentNotFound =>
            Redirect(callbacks.routes.SignInOutController.startVat())
          case e => throw e
        }
      } recoverWith handleErrorResult
  }
}
