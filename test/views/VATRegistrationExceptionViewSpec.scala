/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import forms.VATRegistrationExceptionFormProvider
import models.NormalMode
import views.html.vatRegistrationException

class VATRegistrationExceptionViewSpec extends ViewSpecBase {
  val messageKeyPrefix = "vatRegistrationException"
  val form = new VATRegistrationExceptionFormProvider()()

  val h1 = "Would you like to apply for a VAT registration exception?"
  val paragraph = "You can apply for a registration exception if the business goes over the VAT threshold temporarily."

  object Selectors extends BaseSelectors

  "VATRegistrationException view" must {
    lazy val doc = asDocument(vatRegistrationException(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig))

    "have the correct continue button" in {
      doc.select(Selectors.button).text() mustBe continueButton
    }

    "have the correct back link" in {
      doc.getElementById(Selectors.backLink).text() mustBe backLink
    }

    "have the correct browser title" in {
      doc.select(Selectors.title).text() mustBe title(h1)
    }

    "have the correct heading" in {
      doc.select(Selectors.h1).text() mustBe h1
    }

    "have the first paragraph" in {
      doc.select(Selectors.p(1)).text() mustBe paragraph
    }

    "have the correct legend" in {
      doc.select(Selectors.legend(1)).text() mustBe h1
    }
  }
}
