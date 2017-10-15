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

package fixtures

import java.time.LocalDate

import models.api._
import models.view.TaxableTurnover

trait TradingDetailsFixture {
  val testDate = LocalDate.of(2017, 3, 21)

  //View models
  val validTaxableTurnover = TaxableTurnover("TAXABLE_YES")

  // Api models
  val validVatChoice = VatEligibilityChoice(
    VatEligibilityChoice.NECESSITY_VOLUNTARY,
    vatThresholdPostIncorp = Some(VatThresholdPostIncorp(true, Some(testDate))),
    vatExpectedThresholdPostIncorp = Some(VatExpectedThresholdPostIncorp(true, Some(testDate))))

  val validVatServiceEligibility = VatServiceEligibility(vatEligibilityChoice = Some(validVatChoice))

  def vatServiceEligibility(necessity: String = VatEligibilityChoice.NECESSITY_VOLUNTARY, reason: Option[String] = None): VatServiceEligibility =
    VatServiceEligibility(
      vatEligibilityChoice = Some(VatEligibilityChoice(
        necessity = necessity,
        reason = reason
      ))
    )
}