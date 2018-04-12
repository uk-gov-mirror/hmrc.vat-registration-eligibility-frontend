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

import forms.TaxableTurnoverForm.RADIO_YES_NO
import helpers.FormInspectors._
import models.view.TaxableTurnover
import models.view.TaxableTurnover.TAXABLE_YES
import uk.gov.hmrc.play.test.UnitSpec

class TaxableTurnoverFormSpec extends UnitSpec {

  val vatThreshold = "12345"
  val testForm = TaxableTurnoverForm.form(vatThreshold)

  "Binding TaxableTurnoverForm to a model" should {
    "bind successfully with full data" in {
      val data = Map(RADIO_YES_NO -> TAXABLE_YES)
      val model = TaxableTurnover(TAXABLE_YES)

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "have the correct error if no data is provided" in {
      val data: Map[String,String] = Map()
      val boundForm = testForm.bind(data)

      boundForm shouldHaveErrors Seq(RADIO_YES_NO -> "validation.taxable.turnover.missing")
    }

    "have the correct error if wrong data is provided" in {
      val data = Map(RADIO_YES_NO -> "wrong data")
      val boundForm = testForm.bind(data)

      boundForm shouldHaveErrors Seq(RADIO_YES_NO -> "error.unknown")
    }
  }

  "Unbinding TaxableTurnoverForm to a model" should {
    "Unbind successfully with full data" in {
      val data = Map(RADIO_YES_NO -> TAXABLE_YES)
      val model = TaxableTurnover(TAXABLE_YES)

      testForm.fill(model).data shouldBe data
    }
  }
}
