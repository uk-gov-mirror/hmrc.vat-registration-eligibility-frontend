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

import forms.mappings.Mappings
import javax.inject.Inject
import models.ConditionalNinoFormElement
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

class ApplicantUKNinoFormProvider @Inject() extends FormErrorHelper with Mappings {

  val applicantUKNinoSelection = s"applicantUKNinoSelection"
  val applicantUKNinoEntry = s"applicantUKNinoEntry"
  val errorKeyRoot = s"applicantUKNino.error"
  val valueRequiredKey = s"$errorKeyRoot.required"
  val ninoInvalidKey = s"$errorKeyRoot.nino.valid"
  val ninoNeededKey = s"$errorKeyRoot.nino.required"

  private def ninoFormatter(nino: String): String = nino.grouped(2).mkString(" ")

  def apply(officerName: Option[String]): Form[ConditionalNinoFormElement] = {
    val args = Seq(officerName).flatten
    val selectionRequired = officerName.fold(valueRequiredKey)(_ => s"$valueRequiredKey.onBehalfOf")
    val ninoRequired = officerName.fold(ninoNeededKey)(_ => s"$ninoNeededKey.onBehalfOf")

    Form(
      mapping(
        applicantUKNinoSelection -> boolean(selectionRequired, args = args),
        applicantUKNinoEntry -> mandatoryIf(
          isEqual(applicantUKNinoSelection, "true"),
          text(ninoRequired, args = args)
            .transform(_.replaceAll("\\s", "").toUpperCase, ninoFormatter)
            .verifying(validNino(ninoInvalidKey))
        )
      )(ConditionalNinoFormElement.apply)(ConditionalNinoFormElement.unapply)
    )
  }
}
