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

package models.views

import java.time.LocalDate

import models.view.{ExpectationOverThresholdView, OverThresholdView, TaxableTurnover, Threshold, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.TaxableTurnover._
import play.api.libs.json.Json
import models.view.VoluntaryRegistration._
import models.view.VoluntaryRegistrationReason._
import uk.gov.hmrc.play.test.UnitSpec

class ThresholdSpec extends UnitSpec {
  "apiWrites" should {
    "return a correct Json for pre incorp" when {
      "the registration is not mandatory" in {
        val expectedJson = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration": false,
             |  "voluntaryReason": "$SELLS"
             |}
         """.stripMargin)

        val threshold = Threshold(
          Some(TaxableTurnover(TAXABLE_NO)),
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          None,
          None
        )

        Json.toJson(threshold)(Threshold.apiWrites) shouldBe expectedJson
      }

      "the registration is mandatory" in {
        val expectedJson = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration": true
             |}
         """.stripMargin)

        val threshold = Threshold(
          Some(TaxableTurnover(TAXABLE_YES)),
          None,
          None,
          None,
          None
        )

        Json.toJson(threshold)(Threshold.apiWrites) shouldBe expectedJson
      }
    }

    "return a correct Json for post incorp" when {
      "the registration is not mandatory" in {
        val expectedJson = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration": false,
             |  "voluntaryReason": "$SELLS"
             |}
         """.stripMargin)

        val threshold = Threshold(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          Some(OverThresholdView(false, None)),
          Some(ExpectationOverThresholdView(false, None))
        )

        Json.toJson(threshold)(Threshold.apiWrites) shouldBe expectedJson
      }

      "the registration is mandatory with Over Threshold Date" in {
        val expectedJson = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration": true,
             |  "overThresholdDate": "2017-12-03"
             |}
         """.stripMargin)

        val threshold = Threshold(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          Some(OverThresholdView(true, Some(LocalDate.of(2017, 12, 3)))),
          Some(ExpectationOverThresholdView(false, None))
        )

        Json.toJson(threshold)(Threshold.apiWrites) shouldBe expectedJson
      }

      "the registration is mandatory with Expected Over Threshold Date" in {
        val expectedJson = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration": true,
             |  "expectedOverThresholdDate": "2017-12-03"
             |}
         """.stripMargin)

        val threshold = Threshold(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          Some(OverThresholdView(false, None)),
          Some(ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 12, 3))))
        )

        Json.toJson(threshold)(Threshold.apiWrites) shouldBe expectedJson
      }

      "the registration is mandatory with both Threshold dates" in {
        val expectedJson = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration": true,
             |  "overThresholdDate": "2017-12-03",
             |  "expectedOverThresholdDate": "2017-12-03"
             |}
         """.stripMargin)

        val threshold = Threshold(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          Some(OverThresholdView(true, Some(LocalDate.of(2017, 12, 3)))),
          Some(ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 12, 3))))
        )

        Json.toJson(threshold)(Threshold.apiWrites) shouldBe expectedJson
      }
    }

    "return IllegalStateException" when {
      "it can not determine mandatoryRegistration" in {
        val threshold = Threshold(None, None, None, None, None)

        an[IllegalStateException] shouldBe thrownBy(Json.toJson(threshold)(Threshold.apiWrites))
      }

      "it is missing an Over Threshold Date" in {
        val threshold = Threshold(
          None,
          None,
          None,
          Some(OverThresholdView(true, None)),
          Some(ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 12, 1))))
        )

        an[IllegalStateException] shouldBe thrownBy(Json.toJson(threshold)(Threshold.apiWrites))
      }

      "it is missing an Expected Over Threshold Date" in {
        val threshold = Threshold(
          None,
          None,
          None,
          Some(OverThresholdView(true, Some(LocalDate.of(2017, 12, 1)))),
          Some(ExpectationOverThresholdView(true, None))
        )

        an[IllegalStateException] shouldBe thrownBy(Json.toJson(threshold)(Threshold.apiWrites))
      }
    }
  }
}
