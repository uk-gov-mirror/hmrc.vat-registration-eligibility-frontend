/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.mappings.Mappings
import javax.inject.Inject
import models.{TurnoverEstimate, TurnoverEstimateFormElement}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.hmrc.play.mappers.StopOnFirstFail
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

class TurnoverEstimateFormProvider @Inject() extends FormErrorHelper with Mappings {

  val turnoverEstimateSelection = s"turnoverEstimateSelection"
  val turnoverEstimateAmount = s"turnoverEstimateAmount"
  val errorKeyRoot = s"turnoverEstimate.error"
  val valueRequiredKey = s"$errorKeyRoot.required"
  val amountLessThan = s"$errorKeyRoot.amount.giveLessThan"
  val amountMoreThan = s"$errorKeyRoot.amount.giveMoreThan"
  val amountNumbers = s"$errorKeyRoot.amount.numbers"

  def apply(): Form[TurnoverEstimateFormElement] = Form(
    mapping(
      turnoverEstimateSelection -> text(valueRequiredKey).verifying(matchesRadioSeq(TurnoverEstimate.options, valueRequiredKey)),
      turnoverEstimateAmount -> mandatoryIf(
        isEqual(turnoverEstimateSelection, TurnoverEstimate.TenThousand.toString),
        text(valueRequiredKey)
          .verifying(StopOnFirstFail(
              validBigIntConversion(amountNumbers),
              bigIntRange(amountLessThan, amountMoreThan, BigInt("10000"), BigInt("999999999999999"))
            )
          )
      )
    )(TurnoverEstimateFormElement.apply)(TurnoverEstimateFormElement.unapply)
  )
}
