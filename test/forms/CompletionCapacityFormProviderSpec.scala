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

import forms.behaviours.OptionFieldBehaviours
import identifiers.CompletionCapacityId
import models.{Name, Officer}
import play.api.data.FormError

class CompletionCapacityFormProviderSpec extends OptionFieldBehaviours {

  val officersList: Seq[Officer] = Seq(
    Officer(Name(Some("First"), Some("Middle"), "Last",Some("Mrs")),"director", None, Some("some-url")),
    Officer(Name(Some("Second"), None, "VeryLast",Some("Mr")), "secretary", None, Some("some-url"))
  )

  val form = new CompletionCapacityFormProvider()(CompletionCapacityId)(officersList)

  ".value" must {

    val fieldName = "value"
    val requiredKey = "completionCapacity.error.required.multipleOfficers"

    behave like optionsField[String](
      form,
      fieldName,
      validValues  = officersList.map(_.generateId).toSet,
      invalidError = FormError(fieldName, "error.unknown")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
