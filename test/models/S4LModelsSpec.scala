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

package models

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import models.api._
import models.api.VatEligibilityChoice._
import models.view._
import models.view.VoluntaryRegistrationReason._
import models.view.TaxableTurnover._
import models.view.VoluntaryRegistration._
import org.scalatest.Inspectors
import uk.gov.hmrc.play.test.UnitSpec

class S4LModelsSpec  extends UnitSpec with Inspectors with VatRegistrationFixture {

  "S4LTradingDetails.S4LModelTransformer.toS4LModel" should {
    val specificDate = LocalDate.of(2017, 11, 12)
    val tradingName = "name"

    "transform VatScheme to S4L container" in {
      val vs = emptyVatScheme.copy(
        vatServiceEligibility = Some(VatServiceEligibility(
          vatEligibilityChoice = Some(VatEligibilityChoice(
            necessity = NECESSITY_VOLUNTARY,
            reason = Some(INTENDS_TO_SELL),
            vatThresholdPostIncorp = Some(validVatThresholdPostIncorp))
        )))
      )

      val expected = S4LVatEligibilityChoice(
        taxableTurnover = Some(TaxableTurnover(TAXABLE_NO)),
        voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
        voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(INTENDS_TO_SELL)),
        overThreshold = Some(OverThresholdView(false, None))
      )

      S4LVatEligibilityChoice.modelT.toS4LModel(vs) shouldBe expected
    }
  }

  "S4LVatChoice.S4LApiTransformer.toApi" should {
    val specificDate = LocalDate.of(2017, 11, 12)
    val tradingName = "name"


    val s4l = S4LVatEligibilityChoice(
      taxableTurnover = Some(TaxableTurnover(TAXABLE_NO)),
      voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
      voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(INTENDS_TO_SELL)),
      overThreshold = Some(OverThresholdView(false, None))
    )

    "transform complete S4L with voluntary registration model to API" in {
      val expected = VatEligibilityChoice(
          necessity = NECESSITY_VOLUNTARY,
          reason = Some(INTENDS_TO_SELL),
          vatThresholdPostIncorp = Some(validVatThresholdPostIncorp))

      S4LVatEligibilityChoice.apiT.toApi(s4l) shouldBe expected
    }

    "transform complete S4L with mandatory registration model to API" in {

      val expected = VatEligibilityChoice(
          necessity = NECESSITY_OBLIGATORY,
          reason = None,
          vatThresholdPostIncorp = Some(validVatThresholdPostIncorp))

      val s4lMandatoryBydefault = s4l.copy(voluntaryRegistration = None, voluntaryRegistrationReason = None)
      S4LVatEligibilityChoice.apiT.toApi(s4lMandatoryBydefault) shouldBe expected

      val s4lMandatoryExplicit = s4l.copy(voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_NO)), voluntaryRegistrationReason = None)
      S4LVatEligibilityChoice.apiT.toApi(s4lMandatoryExplicit) shouldBe expected

    }
  }

  "S4LVatEligibility.S4LModelTransformer.toApi" should {
    "transform complete s4l container to API with a vatChoice" in {
      val s4l = S4LVatEligibility(Some(validServiceEligibility))
      S4LVatEligibility.apiT.toApi(s4l) shouldBe validServiceEligibility
    }

    "transform complete s4l container to API without a vatChoice" in {
      val s4l = S4LVatEligibility(Some(validServiceEligibilityNoChoice))
      S4LVatEligibility.apiT.toApi(s4l) shouldBe validServiceEligibilityNoChoice
    }

    "transform s4l container with incomplete data error" in {
      val s4l = S4LVatEligibility()
      an[IllegalStateException] should be thrownBy S4LVatEligibility.apiT.toApi(s4l)
    }
  }
}