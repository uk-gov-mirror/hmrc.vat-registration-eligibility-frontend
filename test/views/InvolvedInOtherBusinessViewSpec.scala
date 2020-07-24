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
import forms.InvolvedInOtherBusinessFormProvider
import models.NormalMode
import play.api.data.Form
import views.behaviours.YesNoViewBehaviours
import views.html.involvedInOtherBusiness

class InvolvedInOtherBusinessViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "involvedInOtherBusiness"

  val form = new InvolvedInOtherBusinessFormProvider().form()

  def createView(officer: Option[String] = None) = () => involvedInOtherBusiness(frontendAppConfig, form, NormalMode, officer)(fakeRequest, messages)

  def createViewUsingForm(officer: Option[String] = None) = (form: Form[_]) => involvedInOtherBusiness(frontendAppConfig, form, NormalMode, officer)(fakeRequest, messages)

  "InvolvedInOtherBusiness view with no acting on behalf of officer" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like yesNoPage(createViewUsingForm(), messageKeyPrefix, routes.InvolvedInOtherBusinessController.onSubmit().url)

    "display the right headings and bullets correctly" in {
      val doc = asDocument(createView()())
      doc.getElementById("main-heading").text() mustBe "Have you been involved with another business or taken over a VAT-registered business?"
      doc.getElementById("involveBullet1").text() mustBe "over the past 2 years, you have had another self-employed business in the UK or Isle of Man (do not tell us if your only source of self-employed income was from being a landlord)"
      doc.getElementById("involveBullet5").text() mustBe "the company has taken over another VAT-registered company that was making a profit"
    }
  }
  "InvolvedInOtherBusiness view with an officer acting on behalf of" must {
    "display the right headings and bullets correctly" in {
    val doc = asDocument(createView(Some("foo bar wizz"))())
    doc.getElementById("main-heading").text() mustBe "Has foo bar wizz ever been involved with another business or taken over a VAT-registered business?"
    doc.getElementById("involveBullet1").text() mustBe "over the past 2 years, they have had another self-employed business in the UK or Isle of Man (do not tell us if their only source of self-employed income was from being a landlord)"
    doc.getElementById("involveBullet5").text() mustBe "the company has taken over another VAT-registered company that was making a profit"
      val legends = doc.getElementsByTag("legend")
      legends.first.text mustBe messages(s"$messageKeyPrefix.heading.behalfOf", "foo bar wizz")
    }
  }
}
