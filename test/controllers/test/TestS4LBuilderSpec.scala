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

import models.test.ThresholdTestSetup
import models.view.TaxableTurnover._
import models.view.{ExpectationOverThresholdView, OverThresholdView, TaxableTurnover, Threshold, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.VoluntaryRegistration._
import models.view.VoluntaryRegistrationReason._
import uk.gov.hmrc.play.test.UnitSpec

class TestS4LBuilderSpec extends UnitSpec {
  object TestBuilder extends TestS4LBuilder

  "thresholdFromData" should {
    "return a full valid Threshold view model" in {
      val data = ThresholdTestSetup(
        taxableTurnoverChoice = Some(TAXABLE_NO),
        voluntaryChoice = Some(REGISTER_YES),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdSelection = Some("true"),
        overThresholdMonth = Some("9"),
        overThresholdYear = Some("2017"),
        expectationOverThresholdSelection = Some("true"),
        expectationOverThresholdDay = Some("6"),
        expectationOverThresholdMonth = Some("8"),
        expectationOverThresholdYear = Some("2016")
      )

      val expected = Threshold(
        taxableTurnover = Some(TaxableTurnover(TAXABLE_NO)),
        voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
        voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(SELLS)),
        overThreshold = Some(OverThresholdView(true, Some(LocalDate.of(2017, 9, 30)))),
        expectationOverThreshold = Some(ExpectationOverThresholdView(true, Some(LocalDate.of(2016, 8, 6))))
      )

      TestBuilder.thresholdFromData(data) shouldBe expected
    }

    "return a partial valid Threshold view model" in {
      val data = ThresholdTestSetup(
        taxableTurnoverChoice = Some(TAXABLE_NO),
        voluntaryChoice = Some(REGISTER_YES),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdSelection = Some("false"),
        overThresholdMonth = Some("9"),
        overThresholdYear = Some("2017"),
        expectationOverThresholdSelection = Some("false"),
        expectationOverThresholdDay = Some("6"),
        expectationOverThresholdMonth = Some("8"),
        expectationOverThresholdYear = Some("2016")
      )

      val expected = Threshold(
        taxableTurnover = Some(TaxableTurnover(TAXABLE_NO)),
        voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
        voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(SELLS)),
        overThreshold = Some(OverThresholdView(false, None)),
        expectationOverThreshold = Some(ExpectationOverThresholdView(false, None))
      )

      TestBuilder.thresholdFromData(data) shouldBe expected
    }

    "return a partial valid Threshold view model without Threshold dates" in {
      val data = ThresholdTestSetup(
        taxableTurnoverChoice = Some(TAXABLE_NO),
        voluntaryChoice = Some(REGISTER_YES),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdSelection = Some("fadfgdfgse"),
        overThresholdMonth = Some("9"),
        overThresholdYear = Some("2017"),
        expectationOverThresholdSelection = Some("gdfg"),
        expectationOverThresholdDay = Some("6"),
        expectationOverThresholdMonth = Some("8"),
        expectationOverThresholdYear = Some("2016")
      )

      val expected = Threshold(
        taxableTurnover = Some(TaxableTurnover(TAXABLE_NO)),
        voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
        voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(SELLS)),
        overThreshold = None,
        expectationOverThreshold = None
      )

      TestBuilder.thresholdFromData(data) shouldBe expected
    }
  }
}
