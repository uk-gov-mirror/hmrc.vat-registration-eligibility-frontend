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

package forms
import java.time.LocalDate

import models.view.ThresholdView
import uk.gov.hmrc.play.test.UnitSpec

class ExpectationThresholdFormSpec extends UnitSpec {
  val tForm = PastThirtyDayPeriodThresholdForm.form(LocalDate.of(1995,1,1))

  "Binding form no incorporation date checks" should {
    "return an invalid date error when true and invalid date" in {
      val data = Map(PastThirtyDayPeriodThresholdForm.RADIO_YES_NO -> "true",
        "pastThirtyDayPeriod.day" -> "11",
        "pastThirtyDayPeriod.month" -> "11",
        "pastThirtyDayPeriod.year" -> "11")
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("pastThirtyDayPeriod","validation.pastThirtyDayPeriod.date.invalid"))
    }
      "return no model when false but date is invalid" in {
        val data = Map(PastThirtyDayPeriodThresholdForm.RADIO_YES_NO -> "false",
          "pastThirtyDayPeriod.day" -> "foo",
          "pastThirtyDayPeriod.month" -> "bar",
          "pastThirtyDayPeriod.year" -> "fizz")
        val boundModel = tForm.bind(data)
        boundModel.value shouldBe Some(ThresholdView(false,None))
      }
    "return model if true and date is valid" in {
      val data = Map(PastThirtyDayPeriodThresholdForm.RADIO_YES_NO -> "true",
        "pastThirtyDayPeriod.day" -> "01",
        "pastThirtyDayPeriod.month" -> "01",
        "pastThirtyDayPeriod.year" -> "2017")
      val boundModel = tForm.bind(data)
      boundModel.value shouldBe Some(ThresholdView(true,Some(LocalDate.of(2017,1,1))))
    }
    "return error with empty Form" in {
      val data = Map(PastThirtyDayPeriodThresholdForm.RADIO_YES_NO -> "",
        "pastThirtyDayPeriod.day" -> "",
        "pastThirtyDayPeriod.month" -> "",
        "pastThirtyDayPeriod.year" -> "")
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("pastThirtyDayPeriodRadio","validation.pastThirtyDayPeriod.selection.missing"))
    }
  }
  "Binding form incorporation checks" should {
    "return validation error if date > today" in {
      val date = LocalDate.now().plusDays(1)
      val data = Map(
        PastThirtyDayPeriodThresholdForm.RADIO_YES_NO -> "true",
        "pastThirtyDayPeriod.day" -> s"${date.getDayOfMonth}",
        "pastThirtyDayPeriod.month" -> s"${date.getMonthValue}",
        "pastThirtyDayPeriod.year" -> s"${date.getYear}"
      )
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("pastThirtyDayPeriod","validation.pastThirtyDayPeriod.date.range.above"))
  }
    "return validation error if date < incorp date" in {
      val data = Map(PastThirtyDayPeriodThresholdForm.RADIO_YES_NO -> "true",
        "pastThirtyDayPeriod.day" -> "01",
        "pastThirtyDayPeriod.month" -> "01",
        "pastThirtyDayPeriod.year" -> "1980")
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("pastThirtyDayPeriod","validation.pastThirtyDayPeriod.date.range.below"))
    }
  }
}
