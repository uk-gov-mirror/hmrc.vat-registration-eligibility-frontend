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

package models.test

import models.view.Eligibility
import play.api.libs.json.Json

case class ThresholdTestSetup(taxableTurnoverChoice: Option[Boolean],
                              voluntaryChoice: Option[Boolean],
                              voluntaryRegistrationReason: Option[String],
                              overThresholdSelection: Option[Boolean],
                              overThresholdMonth: Option[String],
                              overThresholdYear: Option[String],
                              expectationOverThresholdSelection: Option[Boolean],
                              expectationOverThresholdDay: Option[String],
                              expectationOverThresholdMonth: Option[String],
                              expectationOverThresholdYear: Option[String])

object ThresholdTestSetup {
  implicit val format = Json.format[ThresholdTestSetup]
}

case class TestSetup(eligibility: Eligibility,
                     threshold: ThresholdTestSetup)

object TestSetup {
  implicit val format = Json.format[TestSetup]
}
