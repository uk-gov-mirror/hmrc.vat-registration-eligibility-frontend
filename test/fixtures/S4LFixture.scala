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

package fixtures

import models.view.{ExpectationOverThresholdView, OverThresholdView, TaxableTurnover, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.VoluntaryRegistration.REGISTER_YES
import models.view.VoluntaryRegistrationReason.SELLS
import models.view.TaxableTurnover.TAXABLE_YES
import models.{S4LVatEligibility, S4LVatEligibilityChoice}

trait S4LFixture {
  val validS4LEligibility = S4LVatEligibility(
    haveNino = Some(true),
    doingBusinessAbroad = Some(false),
    doAnyApplyToYou = Some(false),
    applyingForAnyOf = Some(false),
    applyingForVatExemption = Some(false),
    companyWillDoAnyOf = Some(false)
  )

  val emptyS4LEligibilityChoice = S4LVatEligibilityChoice()

  val validS4LEligibilityChoiceWithTaxableTurnover = S4LVatEligibilityChoice(
    taxableTurnover = Some(TaxableTurnover(TAXABLE_YES)),
    voluntaryRegistration = None,
    voluntaryRegistrationReason = None,
    overThreshold = None,
    expectationOverThreshold = None
  )

  val validS4LEligibilityChoiceWithThreshold = S4LVatEligibilityChoice(
    taxableTurnover = None,
    voluntaryRegistration = None,
    voluntaryRegistrationReason = None,
    overThreshold = Some(OverThresholdView(false)),
    expectationOverThreshold = Some(ExpectationOverThresholdView(false))
  )

  val validS4LEligibilityChoiceWithVoluntarilyData = validS4LEligibilityChoiceWithThreshold.copy(
    voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
    voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(SELLS))
  )
}
