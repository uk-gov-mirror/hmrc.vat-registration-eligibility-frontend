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
import forms.RegisteringBusinessFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.registeringBusiness
import views.newbehaviours.YesNoViewBehaviours

class RegisteringBusinessViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "registeringBusiness"
  val form = new RegisteringBusinessFormProvider()()
  implicit val msgs = messages

  object Selectors extends BaseSelectors

  val button = "Save and continue"

  def createView: () => HtmlFormat.Appendable =
    () => registeringBusiness(form, NormalMode)(fakeDataRequestIncorped, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => registeringBusiness(form, NormalMode)(fakeDataRequestIncorped, messages, frontendAppConfig)

  "RegisteringBusiness view" must {
    behave like yesNoPage(form, createViewUsingForm, messageKeyPrefix, routes.RegisteringBusinessController.onSubmit().url)

    "have a save and continue button" in {
      val doc = asDocument(createViewUsingForm(form))

      doc.select(Selectors.button).text() mustBe button
    }
  }
}
