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

package forms

import uk.gov.hmrc.play.test.UnitSpec

class VoluntaryRegistrationReasonFormSpec extends UnitSpec {
  val testForm = VoluntaryRegistrationReasonForm.form

  "binding a VoluntaryRegistrationReason to a form" should {
    "bind sucessfully if answer is intends to sell" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> VoluntaryRegistrationReasonForm.INTENDS_TO_SELL)
      val model = VoluntaryRegistrationReasonForm.INTENDS_TO_SELL
      val boundForm = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundForm shouldBe model
    }

    "bind sucessfully if answer is already sells" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> VoluntaryRegistrationReasonForm.SELLS)
      val model = VoluntaryRegistrationReasonForm.SELLS
      val boundForm = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundForm shouldBe model
    }

    "bind sucessfully if answer is neither" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> VoluntaryRegistrationReasonForm.NEITHER)
      val model = VoluntaryRegistrationReasonForm.NEITHER
      val boundForm = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundForm shouldBe model
    }

    "have errors if no field selected" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> "")

      val boundForm = testForm.bind(data)

      boundForm.errors map { formErrors =>
        (formErrors.key, formErrors.message)
      } shouldBe Seq("voluntaryRegistrationReasonRadio" -> "validation.voluntary.registration.reason.missing")
    }

    "have errors if an invalid reason is given" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> "skdfsdsdf")

      val boundForm = testForm.bind(data)

      boundForm.errors map { formErrors =>
        (formErrors.key, formErrors.message)
      } shouldBe Seq("voluntaryRegistrationReasonRadio" -> "validation.voluntary.registration.reason.missing")
    }
  }

  "unbinding a VoluntaryRegistrationReason from form" should {
    "unbind successfully for intends to sell" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> VoluntaryRegistrationReasonForm.INTENDS_TO_SELL)
      val model = VoluntaryRegistrationReasonForm.INTENDS_TO_SELL
      testForm.fill(model).data shouldBe data
    }
    "unbind sucessfully for already sells" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> VoluntaryRegistrationReasonForm.SELLS)
      val model = VoluntaryRegistrationReasonForm.SELLS
      testForm.fill(model).data shouldBe data
    }
    "unbind sucessfully for neither" in {
      val data = Map(VoluntaryRegistrationReasonForm.RADIO_REASON -> VoluntaryRegistrationReasonForm.NEITHER)
      val model = VoluntaryRegistrationReasonForm.NEITHER
      testForm.fill(model).data shouldBe data
    }
  }
}
