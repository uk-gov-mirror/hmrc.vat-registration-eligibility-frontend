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

package controllers.test

import java.time.LocalDate
import javax.inject.Inject

import common.enums.CacheKeys
import common.enums.CacheKeys._
import config.AuthClientConnector
import connectors.{S4LConnector, VatRegistrationConnector}
import controllers.VatRegistrationController
import forms.test.TestSetupForm
import models._
import models.test.{TestSetup, ThresholdTestSetup}
import models.view.{Eligibility, Threshold}
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.CurrentProfileService
import utils.SessionProfile

import scala.concurrent.Future

class TestSetupControllerImpl @Inject()(val s4LConnector: S4LConnector,
                                        val messagesApi: MessagesApi,
                                        val authConnector: AuthClientConnector,
                                        val currentProfileService: CurrentProfileService,
                                        val vatRegistrationConnector: VatRegistrationConnector,
                                        val s4LBuilder: TestS4LBuilder) extends TestSetupController

trait TestSetupController extends VatRegistrationController with SessionProfile {
  val s4LConnector: S4LConnector
  val s4LBuilder: TestS4LBuilder
  val vatRegistrationConnector: VatRegistrationConnector

  def show: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      for {
        threshold <- s4LConnector.fetchAndGet[Threshold](profile.registrationId, CacheKeys.Threshold)
        eligibility <- s4LConnector.fetchAndGet[Eligibility](profile.registrationId, CacheKeys.Eligibility)

        testSetup = TestSetup(
          eligibility.getOrElse(Eligibility(None, None, None, None, None, None)),
          ThresholdTestSetup(
            taxableTurnoverChoice = threshold.flatMap(_.overThresholdThirtyDaysPreIncorp),
            voluntaryChoice = threshold.flatMap(_.voluntaryRegistration),
            voluntaryRegistrationReason = threshold.flatMap(_.voluntaryRegistrationReason),
            overThresholdTwelveSelection = threshold.flatMap(_.overThresholdOccuredTwelveMonth).map(_.selection),
            overThresholdTwelveMonth = threshold.flatMap(_.overThresholdOccuredTwelveMonth).flatMap(_.date).map(_.getMonthValue.toString),
            overThresholdTwelveYear = threshold.flatMap(_.overThresholdOccuredTwelveMonth).flatMap(_.date).map(_.getYear.toString),
            pastOverThresholdThirtySelection = threshold.flatMap(_.pastOverThresholdThirtyDays).map(_.selection),
            pastOverThresholdThirtyDay = threshold.flatMap(_.pastOverThresholdThirtyDays).flatMap(_.date).map(_.getDayOfMonth.toString),
            pastOverThresholdThirtyMonth = threshold.flatMap(_.pastOverThresholdThirtyDays).flatMap(_.date).map(_.getMonthValue.toString),
            pastOverThresholdThirtyYear = threshold.flatMap(_.pastOverThresholdThirtyDays).flatMap(_.date).map(_.getYear.toString),
            overThresholdThirtySelection = threshold.flatMap(_.overThresholdThirtyDays).map(_.selection),
            overThresholdThirtyDay = threshold.flatMap(_.overThresholdThirtyDays).flatMap(_.date).map(_.getDayOfMonth.toString),
            overThresholdThirtyMonth = threshold.flatMap(_.overThresholdThirtyDays).flatMap(_.date).map(_.getMonthValue.toString),
            overThresholdThirtyYear = threshold.flatMap(_.overThresholdThirtyDays).flatMap(_.date).map(_.getYear.toString)
          )
        )
        form = TestSetupForm.form.fill(testSetup)
      } yield Ok(views.html.pages.test.test_setup(form))
  }

  def submit: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile =>
      TestSetupForm.form.bindFromRequest().fold(
        badForm => {
          Future.successful(BadRequest(views.html.pages.test.test_setup(badForm)))
        }, {
          data: TestSetup => {
            for {
              _ <- s4LConnector.save(profile.registrationId, CacheKeys.Eligibility, data.eligibility)
              _ <- s4LConnector.save(profile.registrationId, CacheKeys.Threshold, s4LBuilder.thresholdFromData(data.threshold))
            } yield Ok("Test setup complete")
          }
        })
  }

  def addThresholdToBackend(reason: Option[String],
                            overTwelveDate: Option[String],
                            pastThirtyDate: Option[String],
                            overThirtyDate: Option[String]): Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request => implicit profile => {
      val overTwelve = overTwelveDate.map(LocalDate.parse)
      val pastThirty = pastThirtyDate.map(LocalDate.parse)
      val overThirty = overThirtyDate.map(LocalDate.parse)
      val threshold = getThresholdFromJson(buildThreshold(reason, overTwelve, pastThirty, overThirty))

      vatRegistrationConnector.patchThreshold(threshold) map (_ => Ok("Threshold Inserted"))
    }
  }

  private def getThresholdFromJson(json: JsValue)(implicit profile: CurrentProfile): Threshold = {
    Json.fromJson[Threshold](json)(Threshold.apiReads(profile.incorporationDate)).getOrElse(Threshold())
  }

  private def purgeNull(jsObj : JsObject) : JsObject =
    JsObject(jsObj.value.filterNot {
      case (_, value) => value == JsNull
    })

  private def buildThreshold(reason: Option[String],
                             overThresholdTwelveDate: Option[LocalDate],
                             pastThirtyOverDate: Option[LocalDate],
                             overThresholdThirtyDate: Option[LocalDate]) = {
    (reason, overThresholdTwelveDate, pastThirtyOverDate, overThresholdThirtyDate) match {
      case (Some(r), _, _, _) => Json.obj("mandatoryRegistration" -> false, "voluntaryReason" -> r)
      case _ => purgeNull(Json.obj(
        "mandatoryRegistration" -> true,
        "overThresholdOccuredTwelveMonth" -> overThresholdTwelveDate,
        "pastOverThresholdDateThirtyDays" -> pastThirtyOverDate,
        "overThresholdDateThirtyDays" -> overThresholdThirtyDate
      ))
    }
  }
}
