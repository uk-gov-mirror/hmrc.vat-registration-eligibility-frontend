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

import forms.CompletionCapacityFormProvider
import identifiers.CompletionCapacityId
import models.{CompletionCapacity, Name, NormalMode, Officer}
import play.api.data.Form
import views.behaviours.ViewBehaviours
import views.html.completionCapacity

class CompletionCapacityViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "completionCapacity"

  val officersList: Seq[Officer] = Seq(
    Officer(Name(Some("First"), Some("Middle"), "Last",Some("Mrs")),"director", None, Some("some-url")),
    Officer(Name(Some("Second"), None, "VeryLast",Some("Mr")), "secretary", None, Some("some-url"))
  )
  val singleOfficer: Officer =
    Officer(Name(Some("First"), Some("Middle"), "Last",Some("Sr")),"director", None, Some("some-url"))

  val form = new CompletionCapacityFormProvider()(CompletionCapacityId)(officersList)

  def createView = () => completionCapacity(frontendAppConfig, form, NormalMode, CompletionCapacity.multipleOfficers(officersList))(fakeRequest, messages)

  def createViewUsingForm = (form: Form[_]) => completionCapacity(frontendAppConfig, form, NormalMode, CompletionCapacity.multipleOfficers(officersList))(fakeRequest, messages)
  def createViewUsingFormSingle = (form: Form[_]) => completionCapacity(frontendAppConfig, form, NormalMode, CompletionCapacity.singleOfficer(singleOfficer), Some(singleOfficer.shortName))(fakeRequest, messages)

  "CompletionCapacity view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "CompletionCapacity view for multiple officers" when {
    "rendered" must {
      "contain radio buttons for the value" in {
        val doc = asDocument(createViewUsingForm(form))
        for (option <- officersList) {
          assertContainsRadioButton(doc, s"completionCapacity-${option.generateId}", "value", option.generateId, false)
        }
      }
    }

    for(option <- officersList) {
      s"rendered with a value of '${option.generateId}'" must {
        s"have the '${option.generateId}' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("value" -> s"${option.generateId}"))))
          assertContainsRadioButton(doc, s"completionCapacity-${option.generateId}", "value", option.generateId, true)

          for(unselectedOption <- officersList.filterNot(o => o.generateId == option.generateId)) {
            assertContainsRadioButton(doc, s"completionCapacity-${unselectedOption.generateId}", "value", unselectedOption.generateId, false)
          }
        }
      }
    }
  }

  "CompletionCapacity view for one officer" when {
    "rendered" must {
      "contain radio buttons for Yes and No" in {
        val doc = asDocument(createViewUsingFormSingle(form))
        assertContainsRadioButton(doc, s"completionCapacity-yes", "value", singleOfficer.generateId, false)
        assertContainsRadioButton(doc, s"completionCapacity-no", "value", "noneOfThese", false)
        assertPageTitleEqualsMessage(doc, "completionCapacity.heading.single", singleOfficer.shortName)
      }
    }

    s"rendered with a value of '${singleOfficer.generateId}'" must {
      s"have the '${singleOfficer.generateId}' radio button selected" in {
        val doc = asDocument(createViewUsingFormSingle(form.bind(Map("value" -> s"${singleOfficer.generateId}"))))
        assertContainsRadioButton(doc, s"completionCapacity-yes", "value", singleOfficer.generateId, true)
        assertContainsRadioButton(doc, s"completionCapacity-no", "value", "noneOfThese", false)
        assertPageTitleEqualsMessage(doc, "completionCapacity.heading.single", singleOfficer.shortName)
      }
    }
  }
}
