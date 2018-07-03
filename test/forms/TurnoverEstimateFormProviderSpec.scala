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

import forms.behaviours.BooleanFieldBehaviours
import models.{TurnoverEstimate, TurnoverEstimateFormElement}
import play.api.data.FormError

class TurnoverEstimateFormProviderSpec extends BooleanFieldBehaviours {

  val form = new TurnoverEstimateFormProvider().apply()

  "bind" should {
    val selectionFieldName = s"turnoverEstimateSelection"
    val amountFieldName = s"turnoverEstimateAmount"
    val errorKeyRoot = s"turnoverEstimate.error"
    val turnoverEstimateRequired = s"$errorKeyRoot.required"
    val turnoverEstimateAmountLessThan = s"$errorKeyRoot.amount.giveLessThan"
    val turnoverEstimateAmountMoreThan = s"$errorKeyRoot.amount.giveMoreThan"
    val turnoverEstimateNeedsNumbers = s"$errorKeyRoot.amount.numbers"

    "return errors" when {
      "nothing is selected" in {
        form.bind(Map("" -> "")).errors shouldBe Seq(FormError(selectionFieldName, turnoverEstimateRequired, Seq()))
      }

      "over ten thousand is selected but no amount is provided" in {
        form.bind(Map(selectionFieldName -> TurnoverEstimate.TenThousand.toString))
          .errors shouldBe Seq(FormError(amountFieldName, turnoverEstimateRequired, Seq()))
      }

      "over ten thousand is selected but an invalid amount is provided" in {
        form.bind(
          Map(
            selectionFieldName -> TurnoverEstimate.TenThousand.toString,
            amountFieldName -> "abcd"
          )
        ).errors shouldBe Seq(FormError(amountFieldName, turnoverEstimateNeedsNumbers))
      }

      "over ten thousand is selected but the amount is too high" in {
        val wrappedArray = Seq(BigInt("999999999999999"))
        form.bind(
          Map(
            selectionFieldName -> TurnoverEstimate.TenThousand.toString,
            amountFieldName -> "99999999999999999999999999"
          )
        ).errors shouldBe Seq(FormError(amountFieldName, turnoverEstimateAmountLessThan, wrappedArray))
      }

      "over ten thousand is selected but the amount is too low" in {
        val wrappedArray = Seq(BigInt("10000"))
        form.bind(
          Map(
            selectionFieldName -> TurnoverEstimate.TenThousand.toString,
            amountFieldName -> "10000"
          )
        ).errors shouldBe Seq(FormError(amountFieldName, turnoverEstimateAmountMoreThan, wrappedArray))
      }
    }

    "return a ConditionalFromElement" when {
      "over ten thousand is selected" in {
        val bind = form.bind(
          Map(
            selectionFieldName -> TurnoverEstimate.TenThousand.toString,
            amountFieldName -> "10001"
          )
        )
        bind.value shouldBe Some(TurnoverEstimateFormElement(TurnoverEstimate.TenThousand.toString, Some("10001")))
      }
      "between one and ten thousand is selected" in {
        form.bind(
          Map(
            selectionFieldName -> TurnoverEstimate.Oneandtenthousand.toString
          )
        ).value shouldBe Some(TurnoverEstimateFormElement(TurnoverEstimate.Oneandtenthousand.toString, None))
      }
      "zero pounds" in {
        form.bind(
          Map(
            selectionFieldName -> TurnoverEstimate.Zeropounds.toString
          )
        ).value shouldBe Some(TurnoverEstimateFormElement(TurnoverEstimate.Zeropounds.toString, None))
      }
    }
  }
}
