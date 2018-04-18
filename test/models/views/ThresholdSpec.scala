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

import models.view.{ExpectationOverThresholdView, OverThresholdView, Threshold}
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec
import forms.VoluntaryRegistrationReasonForm._

class ThresholdSpec extends UnitSpec {

  val incorpDate: LocalDate = LocalDate.now()

  "apiReads" should {
    "return a correct model for pre incorp" when {
      "the registration is mandatory" in {
        val json = Json.parse(
          """
            |{
            |  "mandatoryRegistration":true
            |}
          """.stripMargin)
        val parseResult = JsSuccess(Threshold(Some(true), None, None, None, None))

        Json.fromJson[Threshold](json)(Threshold.apiReads(None)) shouldBe parseResult
      }

      "the registration is voluntary" in {
        val json = Json.parse(
          """
            |{
            |  "mandatoryRegistration":false,
            |  "voluntaryReason":"a reason"
            |}
          """.stripMargin)
        val parseResult = JsSuccess(Threshold(Some(false), Some(true), Some("a reason"), None, None))

        Json.fromJson[Threshold](json)(Threshold.apiReads(None)) shouldBe parseResult
      }
    }

    "return a correct model for post-incorp" when {
      "the registration is mandatory and both dates provided" in {
        val json = Json.parse(
          s"""
            |{
            |  "mandatoryRegistration":true,
            |  "overThresholdDate":"$incorpDate",
            |  "expectedOverThresholdDate":"$incorpDate"
            |}
          """.stripMargin)
        val parseResult = JsSuccess(
          Threshold(
            None,
            None,
            None,
            Some(OverThresholdView(selection = true, Some(incorpDate))),
            Some(ExpectationOverThresholdView(selection = true, Some(incorpDate)))
          )
        )

        Json.fromJson[Threshold](json)(Threshold.apiReads(Some(incorpDate))) shouldBe parseResult
      }

      "the registration is mandatory and no dates provided" in {
        val json = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration":true
             |}
          """.stripMargin)
        val parseResult = JsSuccess(
          Threshold(
            None,
            None,
            None,
            Some(OverThresholdView(selection = false, None)),
            Some(ExpectationOverThresholdView(selection = false, None))
          )
        )

        Json.fromJson[Threshold](json)(Threshold.apiReads(Some(incorpDate))) shouldBe parseResult
      }

      "the registration is voluntary" in {
        val json = Json.parse(
          s"""
             |{
             |  "mandatoryRegistration":false,
             |  "voluntaryReason":"a reason",
             |  "overThresholdDate":"$incorpDate",
             |  "expectedOverThresholdDate":"$incorpDate"
             |}
          """.stripMargin)
        val parseResult = JsSuccess(
          Threshold(
            None,
            Some(true),
            Some("a reason"),
            Some(OverThresholdView(selection = true, Some(incorpDate))),
            Some(ExpectationOverThresholdView(selection = true, Some(incorpDate)))
          )
        )

        Json.fromJson[Threshold](json)(Threshold.apiReads(Some(incorpDate))) shouldBe parseResult
      }
    }
  }

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
          Some(false),
          Some(true),
          Some(SELLS),
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
          Some(true),
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
          Some(true),
          Some(SELLS),
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
          Some(true),
          Some(SELLS),
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
          Some(true),
          Some(SELLS),
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
          Some(true),
          Some(SELLS),
          Some(OverThresholdView(true, Some(LocalDate.of(2017, 12, 3)))),
          Some(ExpectationOverThresholdView(true, Some(LocalDate.of(2017, 12, 3))))
        )

        Json.toJson(threshold)(Threshold.apiWrites) shouldBe expectedJson
      }
    }
  }
}
