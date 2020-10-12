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

package views

import controllers.routes
import forms.VoluntaryRegistrationFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.voluntaryRegistration
import views.newbehaviours.YesNoViewBehaviours

class VoluntaryRegistrationViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "voluntaryRegistration"
  val form = new VoluntaryRegistrationFormProvider()()
  implicit val msgs = messages

  val continueButton = "Continue"
  val h1 = "The business doesn't have to register for VAT"
  val paragraph = "The business can still register voluntarily, if it:"
  val bullet1 = "has ever sold VAT-taxable goods or services"
  val bullet2 = "currently sells VAT-taxable goods or services"
  val bullet3 = "intends to sell VAT-taxable goods or services"
  val h2 = "Does the business want to register voluntarily?"
  val indentText = "Only register voluntarily if the company intends to start reporting VAT in the next 3 months."

  object Selectors extends BaseSelectors

  def createView: () => HtmlFormat.Appendable =
    () => voluntaryRegistration(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => voluntaryRegistration(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  "VoluntaryRegistration view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithBackLink(createViewUsingForm(form))
    behave like pageWithSubmitButton(createViewUsingForm(form), continueButton)

    "have the correct heading" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.h1).text() mustBe h1
    }

    "have the correct legend" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select("legend").text() mustBe h2
    }

    "have the first paragraph " in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.p(1)).text() mustBe paragraph
    }

    "display the bullet text correctly" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.bullet(1)).first().text() mustBe bullet1
      doc.select(Selectors.bullet(2)).first().text() mustBe bullet2
      doc.select(Selectors.bullet(3)).first().text() mustBe bullet3
    }

    "have the correct second heading" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.h2(1)).first().text() mustBe h2
    }

    "have the correct indent text" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.indent).first().text() mustBe indentText
    }
  }

}
