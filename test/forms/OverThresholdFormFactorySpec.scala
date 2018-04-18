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

import models.MonthYearModel
import models.view.OverThresholdView
import uk.gov.hmrc.play.test.UnitSpec

class OverThresholdFormFactorySpec extends UnitSpec {

  val vatThreshold = "12345"
  val testForm = OverThresholdFormFactory.form(LocalDate.of(2016, 8, 5), vatThreshold)

  "Binding OverThresholdFormFactory to a model" should {
    "bind successfully with full data" in {
      val data = Map(OverThresholdFormFactory.RADIO_YES_NO -> "true",
                     "overThreshold.month" -> "9",
                     "overThreshold.year" -> "2016")

      val model = OverThresholdView(true, Some(MonthYearModel("9", "2016")).flatMap(_.toLocalDate))

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "bind successfully with partial data" in {
      val data = Map(OverThresholdFormFactory.RADIO_YES_NO -> "false")

      val model = OverThresholdView(false, None)

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "have the correct error if no data are completed" in {
      val data: Map[String,String] = Map()
      val boundForm = testForm.bind(data)

      boundForm.errors map { formErrors =>
        (formErrors.key, formErrors.message)
      } shouldBe Seq(OverThresholdFormFactory.RADIO_YES_NO -> "validation.overThreshold.selection.missing")
    }

    "have the correct error if data is below incorporation date" in {
      val data: Map[String,String] = Map(OverThresholdFormFactory.RADIO_YES_NO -> "true",
                                         "overThreshold.month" -> "7",
                                         "overThreshold.year" -> "2016")
      val boundForm = testForm.bind(data)

      boundForm.errors map { formErrors =>
        (formErrors.key, formErrors.message)
      } shouldBe Seq("overThreshold" -> "validation.overThreshold.date.range.below")

    }

    "have the correct error if data is above current date" in {
      val data: Map[String,String] = Map(OverThresholdFormFactory.RADIO_YES_NO -> "true",
        "overThreshold.month" -> "7",
        "overThreshold.year" -> LocalDate.now().plusYears(1).getYear.toString)
      val boundForm = testForm.bind(data)

      boundForm.errors map { formErrors =>
        (formErrors.key, formErrors.message)
      } shouldBe Seq("overThreshold" -> "validation.overThreshold.date.range.above")
    }

    "have the correct error if data is an incomplete date" in {
      val data: Map[String,String] = Map(OverThresholdFormFactory.RADIO_YES_NO -> "true",
        "overThreshold.month" -> "",
        "overThreshold.year" -> "2016")
      val boundForm = testForm.bind(data)

      boundForm.errors map { formErrors =>
        (formErrors.key, formErrors.message)
      } shouldBe Seq("overThreshold" -> "validation.overThreshold.date.invalid")
    }
  }

  "Unbinding a OverThresholdView model to a form" should {
    "unbind successfully when selection is false" in {
      val data = Map(OverThresholdFormFactory.RADIO_YES_NO -> "false")
      val model = OverThresholdView(false, None)

      testForm.fill(model).data shouldBe data
    }

    "unbind successfully when selection is true with a date" in {
      val data = Map(OverThresholdFormFactory.RADIO_YES_NO -> "true",
                     "overThreshold.month" -> "9",
                     "overThreshold.year" -> "2016")
      val model = OverThresholdView(true, Some(MonthYearModel("9", "2016")).flatMap(_.toLocalDate))

      testForm.fill(model).data shouldBe data
    }

    "unbind successfully when selection is true with no date" in {
      val data = Map(OverThresholdFormFactory.RADIO_YES_NO -> "true")
      val model = OverThresholdView(true, None)

      testForm.fill(model).data shouldBe data
    }
  }
}
