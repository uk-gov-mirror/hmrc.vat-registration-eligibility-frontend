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

package forms.test

import models.test.{TestSetup, ThresholdTestSetup}
import models.view.Eligibility
import play.api.data.Form
import play.api.data.Forms._

object TestSetupForm {

  val thresholdTestSetupMapping = mapping(
    "taxableTurnoverChoice" -> optional(text),
    "voluntaryChoice" -> optional(text),
    "voluntaryRegistrationReason" -> optional(text),
    "overThresholdSelection" -> optional(text),
    "overThresholdMonth" -> optional(text),
    "overThresholdYear" -> optional(text),
    "expectationOverThresholdSelection" -> optional(text),
    "expectationOverThresholdDay" -> optional(text),
    "expectationOverThresholdMonth" -> optional(text),
    "expectationOverThresholdYear" -> optional(text)
  )(ThresholdTestSetup.apply)(ThresholdTestSetup.unapply)

  val eligibilityTestSetupMapping = mapping(
    "haveNino" -> optional(boolean),
    "doingBusinessAbroad" -> optional(boolean),
    "doAnyApplyToYou" -> optional(boolean),
    "applyingForAnyOf" -> optional(boolean),
    "applyingForVatExemption" -> optional(boolean),
    "companyWillDoAnyOf" -> optional(boolean)
  )(Eligibility.apply)(Eligibility.unapply)

  val form = Form(mapping(
    "vatServiceEligibility" -> eligibilityTestSetupMapping,
    "vatEligibilityChoice" -> thresholdTestSetupMapping
  )(TestSetup.apply)(TestSetup.unapply))
}