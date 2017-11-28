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
import forms.ExpectationThresholdForm
import models.DayMonthYearModel
import models.view.ExpectationOverThresholdView
import org.scalatest.Inside
import uk.gov.hmrc.play.test.UnitSpec


class ExpectationOverThresholdViewSpec  extends UnitSpec with VatRegistrationFixture with Inside {

  val date = LocalDate.of(2017, 3, 31)
  val yesView = ExpectationOverThresholdView(true, Some(date))
  val noView = ExpectationOverThresholdView(false, None)

  "unbind" should {
    "decompose an over threshold view with a date" in {
      inside(ExpectationThresholdForm.unbind(yesView)) {
        case Some((selection, otDate)) =>
          selection shouldBe true
          otDate shouldBe Some(DayMonthYearModel.fromLocalDate(date))
      }
    }

    "decompose an over threshold view without a date" in {
      inside(ExpectationThresholdForm.unbind(noView)) {
        case Some((selection, otDate)) =>
          selection shouldBe false
          otDate shouldBe None
      }
    }
  }

  "bind" should {
    "create OverThresholdView when MonthYearModel is present" in {
      ExpectationThresholdForm.bind(true, Some(DayMonthYearModel.fromLocalDate(date))) shouldBe ExpectationOverThresholdView(true, Some(date))
    }
    "create OverThresholdView when MonthYearModel is NOT present" in {
      ExpectationThresholdForm.bind(false, None) shouldBe ExpectationOverThresholdView(false, None)
    }
  }
}
