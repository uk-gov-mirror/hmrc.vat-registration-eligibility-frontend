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

package forms.test

import models.test.{TestSetup, ThresholdTestSetup}
import models.view.Eligibility
import play.api.data.Form
import play.api.data.Forms._

object TestSetupForm {

  val thresholdTestSetupMapping = mapping(
    "taxableTurnoverChoice" -> optional(boolean),
    "voluntaryChoice" -> optional(boolean),
    "voluntaryRegistrationReason" -> optional(text),
    "overThresholdTwelveSelection" -> optional(boolean),
    "overThresholdTwelveMonth" -> optional(text),
    "overThresholdTwelveYear" -> optional(text),
    "pastOverThresholdThirtySelection" -> optional(boolean),
    "pastOverThresholdThirtyDay" -> optional(text),
    "pastOverThresholdThirtyMonth" -> optional(text),
    "pastOverThresholdThirtyYear" -> optional(text),
    "overThresholdThirtySelection" -> optional(boolean),
    "overThresholdThirtyDay" -> optional(text),
    "overThresholdThirtyMonth" -> optional(text),
    "overThresholdThirtyYear" -> optional(text)
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