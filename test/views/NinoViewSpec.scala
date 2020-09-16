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
import forms.NinoFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.nino
import views.newbehaviours.YesNoViewBehaviours

class NinoViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "nino"
  val form = new NinoFormProvider()()
  implicit val msgs = messages

  val button = "Continue"

  object Selectors extends BaseSelectors

  def createView: () => HtmlFormat.Appendable =
    () => nino(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => nino(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  "Nino view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithBackLink(createViewUsingForm(form))
    behave like yesNoPage(form, createViewUsingForm, messageKeyPrefix, routes.NinoController.onSubmit().url)
    behave like pageWithSubmitButton(createViewUsingForm(form), button)
  }
}
