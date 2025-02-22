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

import forms.VoluntaryRegistrationFormProvider
import models.NormalMode
import views.html.voluntaryRegistration

class VoluntaryRegistrationViewSpec extends ViewSpecBase {

  val messageKeyPrefix = "voluntaryRegistration"
  val form = new VoluntaryRegistrationFormProvider()()

  val h1 = "Would you like to voluntarily register the business for VAT?"
  val paragraph = "The business can still register voluntarily, if it:"
  val bullet1 = "has ever sold VAT-taxable goods or services"
  val bullet2 = "currently sells VAT-taxable goods or services"
  val bullet3 = "intends to sell VAT-taxable goods or services"
  val h2 = "Does the business want to register voluntarily?"
  val indentText = "Only register voluntarily if the company intends to start reporting VAT in the next 3 months."

  val view = app.injector.instanceOf[voluntaryRegistration]

  object Selectors extends BaseSelectors

  "VoluntaryRegistration view" must {
    lazy val doc = asDocument(view(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig))

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

    "have the correct legend" in {
      doc.select(Selectors.legend(1)).text() mustBe h2
    }

    "have the correct indent text" in {
      doc.select(Selectors.indent).first().text() mustBe indentText
    }
  }
}
