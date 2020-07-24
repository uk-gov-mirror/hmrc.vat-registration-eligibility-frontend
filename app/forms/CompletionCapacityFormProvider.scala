/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.mappings.Mappings
import identifiers.{CompletionCapacityId, Identifier}
import javax.inject.Inject
import models.Officer
import play.api.data.Form

class CompletionCapacityFormProvider @Inject() extends FormErrorHelper with Mappings {

  def apply(pageId: Identifier)(officers: Seq[Officer]): Form[String] = {
    val (errorKey, errorArgs) = (pageId, officers) match {
      case (CompletionCapacityId, officer :: Nil) => (s"$pageId.error.required.singleOfficer", Seq(officer.shortName))
      case (CompletionCapacityId, _)              => (s"$pageId.error.required.multipleOfficers", Seq())
      case _                                      => (s"$pageId.error.required", Seq())
    }

    Form(
      "value" -> text(errorKey, errorArgs).verifying(value => officers.map(_.generateId).contains(value) || value == "noneOfThese")
    )
  }


}
