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

import javax.inject.Inject

import controllers.builders.SummaryVatThresholdBuilder
import models.api.{VatExpectedThresholdPostIncorp, VatThresholdPostIncorp}
import models.view.{ExpectationOverThresholdView, OverThresholdView, Summary, VoluntaryRegistration}
import models.view.VoluntaryRegistration.REGISTER_NO
import models.{CurrentProfile, MonthYearModel, S4LVatEligibilityChoice}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CurrentProfileService, S4LService, VatRegFrontendService, VatRegistrationService}
import utils.SessionProfile

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ThresholdSummaryController @Inject()(implicit val messagesApi: MessagesApi,
                                           implicit val s4LService: S4LService,
                                           implicit val vrs: VatRegistrationService,
                                           val currentProfileService: CurrentProfileService,
                                           val vatRegFrontendService: VatRegFrontendService)
  extends VatRegistrationController with SessionProfile {

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          val dateOfIncorporation = profile.incorporationDate
            .getOrElse(throw new IllegalStateException("Date of Incorporation data expected to be found in Incorporation"))

          getThresholdSummary() map {
            thresholdSummary =>
              Ok(views.html.pages.threshold_summary(
                thresholdSummary,
                MonthYearModel.FORMAT_DD_MMMM_Y.format(dateOfIncorporation))
              )
          }
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => {
        withCurrentProfile { implicit profile =>
          getVatThresholdAndExpectedThreshold().flatMap {
            case a if (a._1.overThresholdSelection || a._2.expectedOverThresholdSelection) =>
              for {
                _ <- save(VoluntaryRegistration(REGISTER_NO))
                _ <- vrs.submitVatEligibility()
              } yield Redirect(vatRegFrontendService.buildVatRegFrontendUrlEntry)
            case _ => Future.successful(Redirect(controllers.routes.VoluntaryRegistrationController.show()))
          }
        }
      }
  }

  def getThresholdSummary()(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[Summary] = {
    for {
      (threshold,expectedThreshold) <- getVatThresholdAndExpectedThreshold()
    } yield thresholdToSummary(threshold, expectedThreshold)
  }

  def thresholdToSummary(vatThresholdPostIncorp: VatThresholdPostIncorp, expectedThresholdPostIncorp: VatExpectedThresholdPostIncorp): Summary = {
    Summary(Seq(
      SummaryVatThresholdBuilder(Some(vatThresholdPostIncorp), Some(expectedThresholdPostIncorp)).section
    ))
  }

  def getVatThresholdAndExpectedThreshold()(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[(VatThresholdPostIncorp,VatExpectedThresholdPostIncorp)] = {
    for {
      vatChoice <- s4LService.fetchAndGet[S4LVatEligibilityChoice]()
      overThreshold <- vatChoice.flatMap(_.overThreshold).pure
      expected <- vatChoice.flatMap(_.expectationOverThreshold).pure
    } yield mapToModels(overThreshold, expected)
  }

  def mapToModels(thresholdView: Option[OverThresholdView],
                  expectedThresholdView: Option[ExpectationOverThresholdView])
  : (VatThresholdPostIncorp, VatExpectedThresholdPostIncorp) = {
    (thresholdView.map(a => VatThresholdPostIncorp(a.selection, a.date)).get,
      expectedThresholdView.map(a => VatExpectedThresholdPostIncorp(a.selection, a.date)).get)
  }
}

