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

package forms

import models._
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec


class BusinessEntityFormProviderSpec extends UnitSpec {
  val form = new BusinessEntityFormProvider()()

  "businessEntityForm" should {

    val businessEntity = "value"

    val businessEntityErrorKey = "businessEntity.error.required"

    "successfully parse a UK Company entity" in {
      val res = form.bind(Map(businessEntity -> UKCompany.toString))
      res.value should contain(UKCompany)
    }

    "successfully parse a Sole trader entity" in {
      val res = form.bind(Map(businessEntity -> SoleTrader.toString))
      res.value should contain(SoleTrader)
    }

    "successfully parse a Partnership entity" in {
      val res = form.bind(Map(businessEntity -> Partnership.toString))
      res.value should contain(Partnership)
    }

    "successfully parse a Division entity" in {
      val res = form.bind(Map(businessEntity -> Division.toString))
      res.value should contain(Division)
    }

    "successfully parse a Other entity" in {
      val res = form.bind(Map(businessEntity -> Other.toString))
      res.value should contain(Other)
    }

    "fail when nothing has been entered in the view" in {
      val res = form.bind(Map.empty[String, String])
      res.errors should contain(FormError(businessEntity, businessEntityErrorKey))
    }

    "fail when it is not an expected value in the view" in {
      val res = form.bind(Map(businessEntity -> "invalid"))
      res.errors should contain(FormError(businessEntity, businessEntityErrorKey))
    }
  }
}

