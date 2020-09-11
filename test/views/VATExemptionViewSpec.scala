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
import forms.VATExemptionFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.vatExemption
import views.newbehaviours.YesNoViewBehaviours

class VATExemptionViewSpec extends YesNoViewBehaviours {

  val button = "Continue"
  val messageKeyPrefix = "vatExemption"
  val form = new VATExemptionFormProvider()()
  implicit val msgs = messages

  def createView: () => HtmlFormat.Appendable =
    () => vatExemption(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => vatExemption(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  "VATExemption view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithBackLink(createViewUsingForm(form))
    behave like yesNoPage(form, createViewUsingForm, messageKeyPrefix, routes.VATExemptionController.onSubmit().url)
    behave like pageWithSubmitButton(createViewUsingForm(form), button)
  }
}
