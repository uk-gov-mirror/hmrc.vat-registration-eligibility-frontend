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
import forms.VATRegistrationExceptionFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.vatRegistrationException
import views.newbehaviours.YesNoViewBehaviours

class VATRegistrationExceptionViewSpec extends YesNoViewBehaviours {
  val messageKeyPrefix = "vatRegistrationException"
  val form = new VATRegistrationExceptionFormProvider()()
  implicit val msgs = messages

  val saveAndContinueButton = "Save and continue"
  val h1 = "Is the business applying for a VAT registration 'exception'?"
  val paragraph = "The business may not need to register for VAT if it can give evidence that it won't go over the threshold in the next 12 months. This is called a registration 'exception'."

  object Selectors extends BaseSelectors

  def createView: () => HtmlFormat.Appendable =
    () => vatRegistrationException(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => vatRegistrationException(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  "VATRegistrationException view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithBackLink(createViewUsingForm(form))
    behave like yesNoPage(form, createViewUsingForm, messageKeyPrefix, routes.VATRegistrationExceptionController.onSubmit().url)
    behave like pageWithSubmitButton(createViewUsingForm(form), saveAndContinueButton)

    "have the correct heading" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.h1).text() mustBe h1
    }

    "have the first paragraph " in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.p(1)).text() mustBe paragraph
    }
  }
}
