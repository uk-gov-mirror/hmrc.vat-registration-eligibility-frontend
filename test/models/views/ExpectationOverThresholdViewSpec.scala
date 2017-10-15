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

package models.views

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import models.api.VatExpectedThresholdPostIncorp
import models.view.ExpectationOverThresholdView
import models.{ApiModelTransformer, DayMonthYearModel, S4LVatEligibilityChoice}
import org.scalatest.Inside
import uk.gov.hmrc.play.test.UnitSpec


class ExpectationOverThresholdViewSpec  extends UnitSpec with VatRegistrationFixture with Inside {

  val date = LocalDate.of(2017, 3, 31)
  val yesView = ExpectationOverThresholdView(true, Some(date))
  val noView = ExpectationOverThresholdView(false, None)

  "unbind" should {
    "decompose an over threshold view with a date" in {
      inside(ExpectationOverThresholdView.unbind(yesView)) {
        case Some((selection, otDate)) =>
          selection shouldBe true
          otDate shouldBe Some(DayMonthYearModel.fromLocalDate(date))
      }
    }

    "decompose an over threshold view without a date" in {
      inside(ExpectationOverThresholdView.unbind(noView)) {
        case Some((selection, otDate)) =>
          selection shouldBe false
          otDate shouldBe None
      }
    }
  }

  "bind" should {
    "create OverThresholdView when MonthYearModel is present" in {
      ExpectationOverThresholdView.bind(true, Some(DayMonthYearModel.fromLocalDate(date))) shouldBe ExpectationOverThresholdView(true, Some(date))
    }
    "create OverThresholdView when MonthYearModel is NOT present" in {
      ExpectationOverThresholdView.bind(false, None) shouldBe ExpectationOverThresholdView(false, None)
    }
  }

  "ViewModelFormat" should {
    val validExpectationOverThresholdView = ExpectationOverThresholdView(false, None)
    val s4LVatChoice: S4LVatEligibilityChoice = S4LVatEligibilityChoice(expectationOverThreshold = Some(validExpectationOverThresholdView))

    "extract over threshold from vatChoice" in {
      ExpectationOverThresholdView.viewModelFormat.read(s4LVatChoice) shouldBe Some(validExpectationOverThresholdView)
    }

    "update empty vatChoice with expectation over threshold" in {
      ExpectationOverThresholdView.viewModelFormat.update(validExpectationOverThresholdView, Option.empty[S4LVatEligibilityChoice]).expectationOverThreshold shouldBe Some(validExpectationOverThresholdView)
    }

    "update non-empty vatChoice with expectation over threshold" in {
      ExpectationOverThresholdView.viewModelFormat.update(validExpectationOverThresholdView, Some(s4LVatChoice)).expectationOverThreshold shouldBe Some(validExpectationOverThresholdView)
    }

    "ApiModelTransformer" should {

      "produce empty view model from an empty frs start date" in {
        val vm = ApiModelTransformer[ExpectationOverThresholdView]
          .toViewModel(vatScheme(vatServiceEligibility = None))
        vm shouldBe None
      }

      "produce a view model from a vatScheme with an over threshold date set" in {
        val vm = ApiModelTransformer[ExpectationOverThresholdView]
          .toViewModel(vatScheme(vatServiceEligibility = Some(validServiceEligibility.copy(
            vatEligibilityChoice = Some(validVatChoice.copy(vatExpectedThresholdPostIncorp =
              Some(VatExpectedThresholdPostIncorp(expectedOverThresholdSelection = true, expectedOverThresholdDate = Some(date)))))
          ))))
        vm shouldBe Some(ExpectationOverThresholdView(true, Some(date)))
      }

      "produce a view model from a vatScheme with no over threshold date" in {
        val vm = ApiModelTransformer[ExpectationOverThresholdView]
          .toViewModel(vatScheme(vatServiceEligibility = Some(validServiceEligibility.copy(
            vatEligibilityChoice = Some(validVatChoice.copy(vatExpectedThresholdPostIncorp =
              Some(VatExpectedThresholdPostIncorp(expectedOverThresholdSelection = false, None))))
          ))))
        vm shouldBe Some(ExpectationOverThresholdView(false, None))
      }

    }
  }

}
