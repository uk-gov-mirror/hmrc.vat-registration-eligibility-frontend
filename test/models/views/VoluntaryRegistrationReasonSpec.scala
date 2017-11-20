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

import fixtures.VatRegistrationFixture
import models.view.VoluntaryRegistrationReason
import models.view.VoluntaryRegistrationReason._
import org.scalatest.{Inspectors, Matchers}
import uk.gov.hmrc.play.test.UnitSpec

class VoluntaryRegistrationReasonSpec extends UnitSpec with Matchers with Inspectors with VatRegistrationFixture {

  private val validationFunction = VoluntaryRegistrationReason.valid

  "VoluntaryRegistrationReason is valid" when {
    "selected reason is one of the allowed values" in {
      forAll(Seq(SELLS, INTENDS_TO_SELL, NEITHER)) {
        validationFunction(_) shouldBe true
      }
    }
  }

  "VoluntaryRegistrationReason is NOT valid" when {
    "selected reason is not of the allowed values" in {
      forAll(Seq("", "not an allowed value")) {
        validationFunction(_) shouldBe false
      }
    }
  }
}
