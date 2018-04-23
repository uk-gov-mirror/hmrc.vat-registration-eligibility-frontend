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

import forms.VoluntaryRegistrationReasonForm._
import models.test.ThresholdTestSetup
import models.view.{Threshold, ThresholdView}
import uk.gov.hmrc.play.test.UnitSpec

class TestS4LBuilderSpec extends UnitSpec {
  object TestBuilder extends TestS4LBuilder

  "thresholdFromData" should {
    "return a full valid Threshold view model" in {
      val data = ThresholdTestSetup(
        taxableTurnoverChoice = Some(false),
        voluntaryChoice = Some(true),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdTwelveSelection = Some(true),
        overThresholdTwelveMonth = Some("9"),
        overThresholdTwelveYear = Some("2017"),
        pastOverThresholdThirtySelection = Some(true),
        pastOverThresholdThirtyDay = Some("6"),
        pastOverThresholdThirtyMonth = Some("8"),
        pastOverThresholdThirtyYear = Some("2016"),
        overThresholdThirtySelection = Some(true),
        overThresholdThirtyDay = Some("6"),
        overThresholdThirtyMonth = Some("8"),
        overThresholdThirtyYear = Some("2016")
      )

      val expected = Threshold(
        overThresholdThirtyDaysPreIncorp = Some(false),
        voluntaryRegistration = Some(true),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdThirtyDays = Some(ThresholdView(true, Some(LocalDate.of(2016, 8, 6)))),
        pastOverThresholdThirtyDays = Some(ThresholdView(true, Some(LocalDate.of(2016, 8, 6)))),
        overThresholdOccuredTwelveMonth = Some(ThresholdView(true, Some(LocalDate.of(2017, 9, 30))))
      )

      TestBuilder.thresholdFromData(data) shouldBe expected
    }

    "return a partial valid Threshold view model" in {
      val data = ThresholdTestSetup(
        taxableTurnoverChoice = Some(false),
        voluntaryChoice = Some(true),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdTwelveSelection = Some(false),
        overThresholdTwelveMonth = Some("9"),
        overThresholdTwelveYear = Some("2017"),
        pastOverThresholdThirtySelection = Some(false),
        pastOverThresholdThirtyDay = Some("6"),
        pastOverThresholdThirtyMonth = Some("8"),
        pastOverThresholdThirtyYear = Some("2016"),
        overThresholdThirtySelection = Some(false),
        overThresholdThirtyDay = Some("6"),
        overThresholdThirtyMonth = Some("8"),
        overThresholdThirtyYear = Some("2016")
      )

      val expected = Threshold(
        overThresholdThirtyDaysPreIncorp = Some(false),
        voluntaryRegistration = Some(true),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdThirtyDays = Some(ThresholdView(false, None)),
        pastOverThresholdThirtyDays = Some(ThresholdView(false, None)),
        overThresholdOccuredTwelveMonth = Some(ThresholdView(false, None))
      )

      TestBuilder.thresholdFromData(data) shouldBe expected
    }

    "return a partial valid Threshold view model without Threshold dates" in {
      val data = ThresholdTestSetup(
        taxableTurnoverChoice = Some(false),
        voluntaryChoice = Some(true),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdTwelveSelection = None,
        overThresholdTwelveMonth = Some("9"),
        overThresholdTwelveYear = Some("2017"),
        pastOverThresholdThirtySelection = None,
        pastOverThresholdThirtyDay = Some("6"),
        pastOverThresholdThirtyMonth = Some("8"),
        pastOverThresholdThirtyYear = Some("2016"),
        overThresholdThirtySelection = None,
        overThresholdThirtyDay = Some("6"),
        overThresholdThirtyMonth = Some("8"),
        overThresholdThirtyYear = Some("2016")
      )

      val expected = Threshold(
        overThresholdThirtyDaysPreIncorp = Some(false),
        voluntaryRegistration = Some(true),
        voluntaryRegistrationReason = Some(SELLS),
        overThresholdThirtyDays = None,
        pastOverThresholdThirtyDays = None,
        overThresholdOccuredTwelveMonth = None
      )

      TestBuilder.thresholdFromData(data) shouldBe expected
    }
  }
}
