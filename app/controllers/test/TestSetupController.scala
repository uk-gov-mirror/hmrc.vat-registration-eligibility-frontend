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

package controllers.test

import javax.inject.Inject

import controllers.VatRegistrationController
import forms.test.TestSetupForm
import models.api._
import models.test.{TestSetup, VatEligibilityChoiceTestSetup, VatServiceEligibilityTestSetup}
import models.{S4LKey, _}
import play.api.i18n.MessagesApi
import play.api.libs.json.Format
import play.api.mvc.{Action, AnyContent}
import services.{CurrentProfileService, S4LService, VatRegistrationService}
import utils.SessionProfile

import scala.concurrent.Future

class TestSetupController @Inject()(implicit s4LService: S4LService,
                                    implicit val messagesApi: MessagesApi,
                                    val currentProfileService: CurrentProfileService,
                                    vatRegistrationService: VatRegistrationService,
                                    s4LBuilder: TestS4LBuilder)
  extends VatRegistrationController with SessionProfile {

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          for {
            eligibilityChoice <- s4LService.fetchAndGet[S4LVatEligibilityChoice]()
            eligibility <- s4LService.fetchAndGet[S4LVatEligibility]()

            testSetup = TestSetup(
              VatServiceEligibilityTestSetup(
                haveNino = eligibility.flatMap(_.vatEligibility).map(_.haveNino.getOrElse("").toString),
                doingBusinessAbroad = eligibility.flatMap(_.vatEligibility).map(_.doingBusinessAbroad.getOrElse("").toString),
                doAnyApplyToYou = eligibility.flatMap(_.vatEligibility).map(_.doAnyApplyToYou.getOrElse("").toString),
                applyingForAnyOf = eligibility.flatMap(_.vatEligibility).map(_.applyingForAnyOf.getOrElse("").toString),
                applyingForVatExemption = eligibility.flatMap(_.vatEligibility).map(_.applyingForVatExemption.getOrElse("").toString),
                companyWillDoAnyOf = eligibility.flatMap(_.vatEligibility).map(_.companyWillDoAnyOf.getOrElse("").toString)
              ),
              VatEligibilityChoiceTestSetup(
                taxableTurnoverChoice =   eligibilityChoice.flatMap(_.taxableTurnover.map(_.yesNo)),
                voluntaryChoice = eligibilityChoice.flatMap(_.voluntaryRegistration).map(_.yesNo),
                voluntaryRegistrationReason = eligibilityChoice.flatMap(_.voluntaryRegistrationReason).map(_.reason),
                overThresholdSelection = eligibilityChoice.flatMap(_.overThreshold).map(_.selection.toString),
                overThresholdMonth = eligibilityChoice.flatMap(_.overThreshold).flatMap(_.date).map(_.getMonthValue.toString),
                overThresholdYear = eligibilityChoice.flatMap(_.overThreshold).flatMap(_.date).map(_.getYear.toString)
              )
            )
            form = TestSetupForm.form.fill(testSetup)
          } yield Ok(views.html.pages.test.test_setup(form))
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          def saveToS4Later[T: Format : S4LKey](userEntered: Option[String], data: TestSetup, f: TestSetup => T): Future[Unit] =
            userEntered.map(_ => s4LService.save(f(data)).map(_ => ())).getOrElse(Future.successful(()))

          TestSetupForm.form.bindFromRequest().fold(
            badForm => {
              Future.successful(BadRequest(views.html.pages.test.test_setup(badForm)))
            }, {
              data: TestSetup => {
                for {
                  _ <- saveToS4Later(data.vatServiceEligibility.haveNino, data, { x =>
                         S4LVatEligibility(Some(VatServiceEligibility(x.vatServiceEligibility.haveNino.map(_.toBoolean),
                           x.vatServiceEligibility.doingBusinessAbroad.map(_.toBoolean),
                           x.vatServiceEligibility.doAnyApplyToYou.map(_.toBoolean),
                           x.vatServiceEligibility.applyingForAnyOf.map(_.toBoolean),
                           x.vatServiceEligibility.applyingForVatExemption.map(_.toBoolean),
                           x.vatServiceEligibility.companyWillDoAnyOf.map(_.toBoolean))))
                         })
                  _ <- s4LService.save(s4LBuilder.eligiblityChoiceFromData(data))
                } yield Ok("Test setup complete")
              }
            })
        }
  }

}