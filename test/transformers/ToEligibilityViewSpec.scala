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

package transformers

import models.view.Eligibility
import uk.gov.hmrc.play.test.UnitSpec
import common.enums.{Version1EligibilityResult => ResultV1}

class ToEligibilityViewSpec extends UnitSpec {
  private val T = Some(true)
  private val F = Some(false)
  private val N = None

  "fromAPIVersion1" should {
    val eligibilityResults: Map[String, Eligibility] = Map(
      ResultV1.noNino.toString                      -> Eligibility(F,N,N,N,N,N),
      ResultV1.doingInternationalBusiness.toString  -> Eligibility(T,T,N,N,N,N),
      ResultV1.otherInvolvementsOrCOLE.toString     -> Eligibility(T,F,T,N,N,N),
      ResultV1.wantsAFRSOrAAS.toString              -> Eligibility(T,F,F,T,N,N),
      ResultV1.wantsExemption.toString              -> Eligibility(T,F,F,F,T,N),
      ResultV1.racehorsesOrLandAndProperty.toString -> Eligibility(T,F,F,F,F,T),
      ResultV1.success.toString                     -> Eligibility(T,F,F,F,F,F)
    )

    eligibilityResults foreach { case (result, expected) =>
      s"return a correct Eligibility view model when result is $result" in {
        ToEligibilityView.fromAPIVersion1(result) shouldBe expected
      }
    }

    "return an NoSuchElementException when the result is unknown" in {
      a[NoSuchElementException] shouldBe thrownBy(ToEligibilityView.fromAPIVersion1("test"))
    }
  }
}
