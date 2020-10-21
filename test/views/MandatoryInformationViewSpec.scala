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

import views.html.mandatoryInformation
import views.newbehaviours.ViewBehaviours

class MandatoryInformationViewSpec extends ViewBehaviours with BaseSelectors {

  val messageKeyPrefix = "mandatoryInformation"
  implicit val msgs = messages

  val h1Text = "You must sign up for Making Tax Digital for VAT"
  val p1Text = "Most VAT registered businesses with a taxable turnover above £85,000 must use compatible software to keep VAT records and send VAT returns."
  val p2Text = "As your taxable turnover is above, or will go above, £85,000 in a rolling 12 month period, you will sign up for Making Tax Digital for VAT as part of the registration process."
  val buttonText = "Continue to register for VAT"

  def createView = () => mandatoryInformation()(fakeRequest, messages, frontendAppConfig)

  lazy val doc = asDocument(createView())

  "Introduction view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithHeading(createView(), h1Text)
    behave like pageWithSubmitButton(createView(), buttonText)

    "have the correct paragraph" in {
      doc.select(p(1)).first.text mustBe p1Text
    }
    "have the correct 2nd paragraph" in {
      doc.select(indent).first.text mustBe p2Text
    }
  }
}
