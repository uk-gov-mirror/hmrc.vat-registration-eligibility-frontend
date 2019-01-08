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
import play.api.data.FormError

class InvolvedInOtherBusinessFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey             = "involvedInOtherBusiness.error.required"
  val requredKeyActingOnBehalf = "involvedInOtherBusiness.behalfOf.error.required"
  val invalidKey              = "error.boolean"

  val form = (actingOnBehalf:Option[String]) => new InvolvedInOtherBusinessFormProvider().form(actingOnBehalf)

  ".value NOT ACTING on behalf of" must {

    val fieldName = "value"

    behave like booleanField(
      form(None),
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form(None),
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
  ".value ACTING on behalf of" must {

    val fieldName = "value"

    behave like booleanField(
      form(Some("foo")),
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form(Some("bar")),
      fieldName,
      requiredError = FormError(fieldName, requredKeyActingOnBehalf)
    )
  }
}
