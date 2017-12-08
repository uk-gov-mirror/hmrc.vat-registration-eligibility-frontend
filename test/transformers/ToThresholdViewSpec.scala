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

package transformers

import java.time.LocalDate

import models.view.{ExpectationOverThresholdView, OverThresholdView, TaxableTurnover, Threshold, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.TaxableTurnover._
import models.view.VoluntaryRegistration._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec

class ToThresholdViewSpec extends UnitSpec {
  "fromAPI" should {
    s"return a correct Threshold view model" when {
      "the company is NOT incorporated" that {
        "is mandatory registration" in {
          val jsonMandatory = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": true
               |}
          """.stripMargin)
          val thresholdMandatory = Threshold(Some(TaxableTurnover(TAXABLE_YES)), None, None, None, None)

          ToThresholdView.fromAPI(jsonMandatory, false) shouldBe thresholdMandatory
        }

        "is not mandatory registration" in {
          val jsonNotMandatory = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": false,
               |  "voluntaryReason": "a reason"
               |}
            """.stripMargin)
          val thresholdNotMandatory = Threshold(Some(TaxableTurnover(TAXABLE_NO)),
            Some(VoluntaryRegistration(REGISTER_YES)),
            Some(VoluntaryRegistrationReason("a reason")),
            None,
            None)

          ToThresholdView.fromAPI(jsonNotMandatory, false) shouldBe thresholdNotMandatory
        }

        "is not mandatory registration and NO reason" in {
          val jsonNotMandatoryNoReson = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": false
               |}
            """.stripMargin)
          val thresholdNotMandatoryNoReason = Threshold(Some(TaxableTurnover(TAXABLE_NO)),
            Some(VoluntaryRegistration(REGISTER_NO)),
            None,
            None,
            None)

          ToThresholdView.fromAPI(jsonNotMandatoryNoReson, false) shouldBe thresholdNotMandatoryNoReason
        }
      }

      "the company is incorporated" that {
        val date = LocalDate.of(2016, 8, 15)

        "is mandatory registration with both threshold date defined" in {
          val jsonMandatory = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": true,
               |  "overThresholdDate": "2016-08-15",
               |  "expectedOverThresholdDate": "2016-08-15"
               |}
            """.stripMargin)
          val thresholdMandatory = Threshold(None, None, None, Some(OverThresholdView(true, Some(date))), Some(ExpectationOverThresholdView(true, Some(date))))

          ToThresholdView.fromAPI(jsonMandatory, true) shouldBe thresholdMandatory
        }

        "is mandatory registration with overThresholdDate defined" in {
          val jsonMandatory = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": true,
               |  "overThresholdDate": "2016-08-15"
               |}
            """.stripMargin)
          val thresholdMandatory = Threshold(None, None, None, Some(OverThresholdView(true, Some(date))), Some(ExpectationOverThresholdView(false, None)))

          ToThresholdView.fromAPI(jsonMandatory, true) shouldBe thresholdMandatory
        }

        "is mandatory registration with expectedOverThresholdDate defined" in {
          val jsonMandatory = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": true,
               |  "expectedOverThresholdDate": "2016-08-15"
               |}
            """.stripMargin)
          val thresholdMandatory = Threshold(None, None, None, Some(OverThresholdView(false, None)), Some(ExpectationOverThresholdView(true, Some(date))))

          ToThresholdView.fromAPI(jsonMandatory, true) shouldBe thresholdMandatory
        }

        "is not mandatory registration with both threshold date NOT defined" in {
          val jsonNotMandatory = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": false,
               |  "voluntaryReason": "a reason"
               |}
            """.stripMargin)
          val thresholdNotMandatory = Threshold(None,
            Some(VoluntaryRegistration(REGISTER_YES)),
            Some(VoluntaryRegistrationReason("a reason")),
            Some(OverThresholdView(false, None)),
            Some(ExpectationOverThresholdView(false, None))
          )

          ToThresholdView.fromAPI(jsonNotMandatory, true) shouldBe thresholdNotMandatory
        }

        "is not mandatory registration with both threshold date NOT defined and NO reason" in {
          val jsonNotMandatoryNoReason = Json.parse(
            s"""
               |{
               |  "mandatoryRegistration": false
               |}
            """.stripMargin)
          val thresholdNotMandatoryNoReason = Threshold(None,
            Some(VoluntaryRegistration(REGISTER_NO)),
            None,
            Some(OverThresholdView(false, None)),
            Some(ExpectationOverThresholdView(false, None))
          )

          ToThresholdView.fromAPI(jsonNotMandatoryNoReason, true) shouldBe thresholdNotMandatoryNoReason
        }
      }
    }
  }
}
