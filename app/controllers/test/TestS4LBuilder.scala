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

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

import models.S4LVatEligibilityChoice
import models.test.TestSetup
import models.view.{OverThresholdView, TaxableTurnover, VoluntaryRegistration, VoluntaryRegistrationReason}

class TestS4LBuilder {

  def eligiblityChoiceFromData(data: TestSetup): S4LVatEligibilityChoice = {
    val taxableTurnover: Option[String] = data.vatEligibilityChoice.taxableTurnoverChoice

    val overThresholdView: Option[OverThresholdView] = data.vatEligibilityChoice.overThresholdSelection match {
      case Some("true") => Some(OverThresholdView(selection = true, Some(LocalDate.of(
        data.vatEligibilityChoice.overThresholdYear.map(_.toInt).get,
        data.vatEligibilityChoice.overThresholdMonth.map(_.toInt).get,
        1
      ).`with`(TemporalAdjusters.lastDayOfMonth()))))
      case Some("false") => Some(OverThresholdView(selection = false, None))
      case _ => None
    }

    val voluntaryRegistration: Option[String] = data.vatEligibilityChoice.voluntaryChoice
    val voluntaryRegistrationReason: Option[String] = data.vatEligibilityChoice.voluntaryRegistrationReason

    S4LVatEligibilityChoice(
      taxableTurnover = taxableTurnover.map(TaxableTurnover(_)),
      voluntaryRegistration = voluntaryRegistration.map(VoluntaryRegistration(_)),
      voluntaryRegistrationReason = voluntaryRegistrationReason.map(VoluntaryRegistrationReason(_)),
      overThreshold = overThresholdView
    )
  }
}
