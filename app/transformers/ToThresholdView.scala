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

import models.view.{TaxableTurnover,
                    Threshold,
                    VoluntaryRegistration,
                    VoluntaryRegistrationReason => VolRegReason,
                    OverThresholdView => OverThres,
                    ExpectationOverThresholdView => ExpOverThres}
import models.view.TaxableTurnover._
import models.view.VoluntaryRegistration._
import play.api.libs.json.JsValue

object ToThresholdView {
  private def toVoluntaryRegistration(mandatory: Boolean, reason: Option[String]): Option[VoluntaryRegistration] = {
    if (mandatory) {
      None
    } else if (reason.isEmpty) {
      Some(VoluntaryRegistration(REGISTER_NO))
    } else {
      Some(VoluntaryRegistration(REGISTER_YES))
    }
  }

  private def toEligibilityChoiceViewModelWithoutIncorpDate(api: JsValue): Threshold = {
    val mandatory: Boolean = (api \ "mandatoryRegistration").validate[Boolean].get
    val reason: Option[String] = (api \ "voluntaryReason").validateOpt[String].get

    Threshold(
      taxableTurnover = if (mandatory) Some(TaxableTurnover(TAXABLE_YES)) else Some(TaxableTurnover(TAXABLE_NO)),
      voluntaryRegistration = toVoluntaryRegistration(mandatory, reason),
      voluntaryRegistrationReason = reason map (VolRegReason(_)),
      overThreshold = None,
      expectationOverThreshold = None
    )
  }

  private def toEligibilityChoiceViewModelWithIncorpDate(api: JsValue): Threshold = {
    val mandatory: Boolean = (api \ "mandatoryRegistration").validate[Boolean].get
    val reason: Option[String] = (api \ "voluntaryReason").validateOpt[String].get

    Threshold(
      taxableTurnover = None,
      voluntaryRegistration = toVoluntaryRegistration(mandatory, reason),
      voluntaryRegistrationReason = reason map (VolRegReason(_)),
      overThreshold = Some(
        (api \ "overThresholdDate").validateOpt[LocalDate].get.fold(OverThres(false, None))(d => OverThres(true, Some(d)))
      ),
      expectationOverThreshold = Some(
        (api \ "expectedOverThresholdDate").validateOpt[LocalDate].get.fold(ExpOverThres(selection = false, None))(d => ExpOverThres(selection = true, Some(d)))
      )
    )
  }

  def fromAPI(api: JsValue, isIncorporated: Boolean): Threshold = {
    if (isIncorporated) toEligibilityChoiceViewModelWithIncorpDate(api) else toEligibilityChoiceViewModelWithoutIncorpDate(api)
  }
}
