/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.behaviours.BooleanFieldBehaviours
import identifiers.ApplicantUKNinoId
import models.ConditionalNinoFormElement
import play.api.data.FormError

class ApplicantUKNinoFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "applicantUKNino.error.required"
  val invalidKey = "error.boolean"

  def form(officerName : Option[String] = None) = new ApplicantUKNinoFormProvider().apply(officerName)

  "bind" should {
    val selectionFieldName = s"${ApplicantUKNinoId}Selection"
    val entryFieldName = s"${ApplicantUKNinoId}Entry"
    val requiredKey = "applicantUKNino.error.required"
    val requiredKeyOnBehalfOff = "applicantUKNino.error.required.onBehalfOf"
    val ninoRequiredKey = "applicantUKNino.error.nino.required"
    val ninoRequiredKeyOnBehalfOff = "applicantUKNino.error.nino.required.onBehalfOf"
    val ninoInvalidKey = "applicantUKNino.error.nino.valid"

    "return errors" when {
      "nothing is selected" in {
        form().bind(Map("" -> "")).errors shouldBe Seq(FormError(selectionFieldName, requiredKey, Seq()))
      }

      "nothing is selected with a name provided" in {
        form(Some("test-name")).bind(Map("" -> "")).errors shouldBe Seq(FormError(selectionFieldName, requiredKeyOnBehalfOff, Seq("test-name")))
      }

      "yes is selected but no nino is provided" in {
        form().bind(Map(selectionFieldName -> "true")).errors shouldBe Seq(FormError(entryFieldName, ninoRequiredKey, Seq()))
      }

      "yes is selected but no nino is provided with an officer name" in {
        form(Some("test-name")).bind(Map(selectionFieldName -> "true")).errors shouldBe Seq(FormError(entryFieldName, ninoRequiredKeyOnBehalfOff, Seq("test-name")))
      }

      "yes is selected but an invalid nino is provided" in {
        form().bind(
          Map(
            selectionFieldName -> "true",
            entryFieldName -> "fake-nino"
          )
        ).errors shouldBe Seq(FormError(entryFieldName, ninoInvalidKey))
      }
    }

    "return a ConditionalFromElement" when {
      "no is selected" in {
        form().bind(
          Map(
            selectionFieldName -> "false"
          )
        ).value shouldBe Some(ConditionalNinoFormElement(false, None))
      }
      "yes is selected and a valid nino is provide" in {
        form().bind(
          Map(
            selectionFieldName -> "true",
            entryFieldName -> "JW778877A"
          )
        ).value shouldBe Some(ConditionalNinoFormElement(true, Some("JW778877A")))
      }
    }
  }
}
