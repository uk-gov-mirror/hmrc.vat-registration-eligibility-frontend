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
import java.time.format.DateTimeFormatter

import forms.behaviours.BooleanFieldBehaviours
import models.ConditionalDateFormElement
import play.api.data.FormError

class ThresholdPreviousThirtyDaysFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "thresholdPreviousThirtyDays.error.required"

  val incorpDate = LocalDate.now().minusYears(2)
  val dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy")
  val optionalDateForm = new ThresholdPreviousThirtyDaysFormProvider()(incorpDate)

  "bind" should {
    val selectionFieldName = s"thresholdPreviousThirtyDaysSelection"
    val dateFieldName = s"thresholdPreviousThirtyDaysDate"
    val dateRequiredKey = "thresholdPreviousThirtyDays.error.date.required"
    val dateInFutureKey = "thresholdPreviousThirtyDays.error.date.inFuture"
    val dateBeforeIncorpKey = "thresholdPreviousThirtyDays.error.date.beforeIncorp"
    val dateInvalidKey = "thresholdPreviousThirtyDays.error.date.invalid"

    "return errors" when {
      "nothing is selected" in {
        optionalDateForm.bind(Map("" -> "")).errors shouldBe Seq(FormError(selectionFieldName, requiredKey, Seq()))
      }

      "yes is selected but no date is provided" in {
        optionalDateForm.bind(Map(selectionFieldName -> "true")).errors shouldBe Seq(FormError(dateFieldName, dateRequiredKey, Seq()))
      }

      "yes is selected but an invalid date is provided" in {
        val date = LocalDate.now().plusMonths(3)
        optionalDateForm.bind(
          Map(
            selectionFieldName -> "true",
            s"${dateFieldName}.day" -> s"${date.getDayOfMonth}",
            s"${dateFieldName}.month" -> s"sdsdf",
            s"${dateFieldName}.year" -> s"${date.getYear}")
        ).errors shouldBe Seq(FormError(dateFieldName, dateInvalidKey))
      }

      "yes is selected but a date in the future is provided" in {
        val date = LocalDate.now().plusMonths(3)
        optionalDateForm.bind(
          Map(
            selectionFieldName -> "true",
            s"${dateFieldName}.day" -> s"${date.getDayOfMonth}",
            s"${dateFieldName}.month" -> s"${date.getMonthValue}",
            s"${dateFieldName}.year" -> s"${date.getYear}")
        ).errors shouldBe Seq(FormError(dateFieldName, dateInFutureKey))
      }

      "yes is selected but a date before incorp is provided" in {
        val date = incorpDate.minusMonths(3)
        optionalDateForm.bind(
          Map(
            selectionFieldName -> "true",
            s"${dateFieldName}.day" -> s"${date.getDayOfMonth}",
            s"${dateFieldName}.month" -> s"${date.getMonthValue}",
            s"${dateFieldName}.year" -> s"${date.getYear}")
        ).errors shouldBe Seq(FormError(dateFieldName, dateBeforeIncorpKey, Seq(incorpDate.format(dateFormat))))
      }
    }

    "return a ConditionalFromElement" when {
      "yes is selected and a month and year is passed in" in {
        val date = incorpDate.plusMonths(5).withDayOfMonth(1)
        optionalDateForm.bind(
          Map(
            selectionFieldName -> "true",
            s"${dateFieldName}.day" -> s"${date.getDayOfMonth}",
            s"${dateFieldName}.month" -> s"${date.getMonthValue}",
            s"${dateFieldName}.year" -> s"${date.getYear}")
        ).value shouldBe Some(ConditionalDateFormElement(true, Some(date)))
      }
      "no is selected" in {
        optionalDateForm.bind(Map(selectionFieldName -> "false")).value shouldBe Some(ConditionalDateFormElement(false, None))
      }
    }
  }
}
