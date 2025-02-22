/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.actions

import com.google.inject.Inject
import connectors.{DataCacheConnector, S4LConnector}
import models.requests.{CacheIdentifierRequest, OptionalDataRequest}
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject()(val dataCacheConnector: DataCacheConnector,
                                        s4LConnector: S4LConnector)
                                       (implicit val executionContext: ExecutionContext) extends DataRetrievalAction {

  override protected def transform[A](request: CacheIdentifierRequest[A]): Future[OptionalDataRequest[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    dataCacheConnector.fetch(request.cacheId).flatMap {
      case None =>
        s4LConnector.fetchCacheMap(request.cacheId) flatMap {
          case Some(data) =>
            dataCacheConnector.save(data) map { savedCacheMap =>
              OptionalDataRequest(request.request, request.cacheId, request.currentProfile, Some(new UserAnswers(savedCacheMap)))
            }
          case _ =>
            Future.successful(OptionalDataRequest(request.request, request.cacheId, request.currentProfile, None))
        }
      case Some(data) =>
        Future.successful(OptionalDataRequest(request.request, request.cacheId, request.currentProfile, Some(new UserAnswers(data))))
    }
  }
}

trait DataRetrievalAction extends ActionTransformer[CacheIdentifierRequest, OptionalDataRequest]
