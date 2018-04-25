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
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CurrentProfileService, SummaryService, VatRegistrationService}
import utils.SessionProfile

import scala.concurrent.Future

class EligibilitySummaryControllerImpl @Inject()(val messagesApi: MessagesApi,
                                                 val summaryService: SummaryService,
                                                 val vatRegistrationService: VatRegistrationService,
                                                 val authConnector: AuthClientConnector,
                                                 val currentProfileService: CurrentProfileService) extends EligibilitySummaryController

trait EligibilitySummaryController extends VatRegistrationController with SessionProfile {
  val summaryService: SummaryService
  val vatRegistrationService: VatRegistrationService

  def show: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      summaryService.getEligibilitySummary map (summary => Ok(views.html.pages.summary_eligibility(summary)))
  }

  private def incorpDateToRedirectLocation(incorpDate : Option[LocalDate]) : Call = incorpDate match {
    case Some(id) if id.isAfter(LocalDate.now().minusMonths(12)) => routes.ThresholdController.goneOverSinceIncorpShow()
    case _                                                       => routes.ThresholdController.overThresholdThirtyShow()
  }

  def submit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      Future.successful(Redirect(incorpDateToRedirectLocation(profile.incorporationDate)))
  }
}
