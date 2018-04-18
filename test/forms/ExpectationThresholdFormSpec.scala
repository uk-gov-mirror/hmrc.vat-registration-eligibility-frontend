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

import models.view.ExpectationOverThresholdView
import uk.gov.hmrc.play.test.UnitSpec

class ExpectationThresholdFormSpec extends UnitSpec {
  val tForm = ExpectationThresholdForm.form(LocalDate.of(1995,1,1))

  "Binding form no incorporation date checks" should {
    "return an invalid date error when true and invalid date" in {
      val data = Map(ExpectationThresholdForm.RADIO_YES_NO -> "true",
        "expectationOverThreshold.day" -> "11",
        "expectationOverThreshold.month" -> "11",
        "expectationOverThreshold.year" -> "11")
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("expectationOverThreshold","validation.expectationOverThreshold.date.invalid"))
    }
      "return no model when false but date is invalid" in {
        val data = Map(ExpectationThresholdForm.RADIO_YES_NO -> "false",
          "expectationOverThreshold.day" -> "foo",
          "expectationOverThreshold.month" -> "bar",
          "expectationOverThreshold.year" -> "fizz")
        val boundModel = tForm.bind(data)
        boundModel.value shouldBe Some(ExpectationOverThresholdView(false,None))
      }
    "return model if true and date is valid" in {
      val data = Map(ExpectationThresholdForm.RADIO_YES_NO -> "true",
        "expectationOverThreshold.day" -> "01",
        "expectationOverThreshold.month" -> "01",
        "expectationOverThreshold.year" -> "2017")
      val boundModel = tForm.bind(data)
      boundModel.value shouldBe Some(ExpectationOverThresholdView(true,Some(LocalDate.of(2017,1,1))))
    }
    "return error with empty Form" in {
      val data = Map(ExpectationThresholdForm.RADIO_YES_NO -> "",
        "expectationOverThreshold.day" -> "",
        "expectationOverThreshold.month" -> "",
        "expectationOverThreshold.year" -> "")
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("expectationOverThresholdRadio","validation.expectationOverThreshold.selection.missing"))
    }
  }
  "Binding form incorporation checks" should {
    "return validation error if date > today" in {
      val date = LocalDate.now().plusDays(1)
      val data = Map(
        ExpectationThresholdForm.RADIO_YES_NO -> "true",
        "expectationOverThreshold.day" -> s"${date.getDayOfMonth}",
        "expectationOverThreshold.month" -> s"${date.getMonthValue}",
        "expectationOverThreshold.year" -> s"${date.getYear}"
      )
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("expectationOverThreshold","validation.expectationOverThreshold.date.range.above"))
  }
    "return validation error if date < incorp date" in {
      val data = Map(ExpectationThresholdForm.RADIO_YES_NO -> "true",
        "expectationOverThreshold.day" -> "01",
        "expectationOverThreshold.month" -> "01",
        "expectationOverThreshold.year" -> "1980")
      val boundModel = tForm.bind(data)

      boundModel.errors map { formError =>
        (formError.key, formError.message)
      } shouldBe Seq(("expectationOverThreshold","validation.expectationOverThreshold.date.range.below"))
    }
  }
}
