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

import java.time.LocalDate

import config.FrontendAppConfig
import forms.ThresholdInTwelveMonthsFormProvider
import javax.inject.Inject
import models.NormalMode
import play.api.data.Form
import play.api.i18n.MessagesApi
import views.html.thresholdInTwelveMonths
import services.ThresholdService
import utils.TimeMachine
import views.newbehaviours.ViewBehaviours



class ThresholdInTwelveMonthsViewSpec @Inject()(implicit appConfig: FrontendAppConfig)  extends ViewBehaviours {

  object TestTimeMachine extends TimeMachine {
    override def today: LocalDate = LocalDate.parse("2020-01-01")
  }

  val messageKeyPrefix = "thresholdInTwelveMonths"
  implicit val msgs = messages
  val form = new ThresholdInTwelveMonthsFormProvider(TestTimeMachine)()

  val thresholdService: ThresholdService = app.injector.instanceOf[ThresholdService]

  val h1 = "In any 12-month period has the business gone over the VAT-registration threshold?"
  val paragraph = "£85,000 is the current VAT-registration threshold. It is the amount of VAT-taxable sales the business can make before it has to register for VAT."
  val bullet1 = "Yes"
  val bullet2 = "No"
  val h2 = "In any 12-month period has the business gone over the VAT-registration threshold?"
  val button = "Continue"

  object Selectors extends BaseSelectors

  def createView = () => thresholdInTwelveMonths(form, NormalMode, thresholdService)(fakeDataRequestIncorped, messages, frontendAppConfig)

  def createViewUsingForm = (form: Form[_]) => thresholdInTwelveMonths(form, NormalMode,thresholdService)(fakeDataRequestIncorped, messages, frontendAppConfig)

  "ThresholdNextThirtyDays view" must {
    behave like normalPage(createView(), messageKeyPrefix)

    "have a heading" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.h1).text() mustBe h1
    }
    "have a paragraph" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.p(1)).text() mustBe paragraph
    }

    "have bullet points" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.bullet(1)).text() mustBe bullet1
      doc.select(Selectors.bullet(2)).text() mustBe bullet2
    }

    "have a continue button" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.button).text() mustBe button
    }
    "contain a legend for the question" in {
      val doc = asDocument(createViewUsingForm(form))
      val legends = doc.getElementsByTag("valueDate")
      legends.size mustBe 1
      legends.first.text mustBe h2
    }
  }
}
