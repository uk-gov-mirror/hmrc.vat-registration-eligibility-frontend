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

package repositories

import javax.inject.{Inject, Singleton}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DatedCacheMap(id: String,
                         data: Map[String, JsValue],
                         lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object DatedCacheMap {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val formats = Json.format[DatedCacheMap]

  def apply(cacheMap: CacheMap): DatedCacheMap = DatedCacheMap(cacheMap.id, cacheMap.data)
}

class ReactiveMongoRepository(config: Configuration, mongo: () => DefaultDB)
  extends ReactiveRepository[DatedCacheMap, BSONObjectID](config.getString("appName").get, mongo, DatedCacheMap.formats) {

  val fieldName = "lastUpdated"
  val createdIndexName = "userAnswersExpiry"
  val expireAfterSeconds = "expireAfterSeconds"
  val timeToLiveInSeconds: Int = config.getInt("mongodb.timeToLiveInSeconds").get

  dropCollectionIndexTemp.map(_ => createIndex(fieldName, createdIndexName, timeToLiveInSeconds))

  private[repositories] def createIndex(field: String, indexName: String, ttl: Int): Future[Boolean] = {
    collection.indexesManager.ensure(Index(Seq((field, IndexType.Ascending)), Some(indexName),
      options = BSONDocument(expireAfterSeconds -> ttl))) map {
      result => {
        Logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
      }
    } recover {
      case e => Logger.error("Failed to set TTL index", e)
        false
    }
  }

  def dropCollectionIndexTemp = collection.indexesManager.drop(createdIndexName).map{ res =>
      Logger.warn(s"[$createdIndexName dropped successfully finding a count of $res before dropping this index")
    true
  }.recoverWith{
    case e =>
      Future.successful {
        Logger.warn(s"[$expireAfterSeconds did not drop as expected] and threw an exception with message ${e.getMessage}")
        false
      }
  }

  def removeEntry(id: String, key: String): Future[CacheMap] = {
    val selector = BSONDocument("id" -> id)
    val update = BSONDocument("$unset" -> BSONDocument(s"data.$key" -> 1))

    collection.findAndModify(selector, collection.updateModifier(update,true,false)).map {
      res => res.value.map(_.as[CacheMap]).getOrElse(throw new Exception(s"[removeEntry] Attempted to remove $key but document did not exist"))
    }
  }

  def upsert(cm: CacheMap): Future[Boolean] = {
    val selector = BSONDocument("id" -> cm.id)
    val cmDocument = Json.toJson(DatedCacheMap(cm))
    val modifier = BSONDocument("$set" -> cmDocument)

    collection.update(selector, modifier, upsert = true).map { lastError =>
      lastError.ok
    }
  }

  def get(id: String): Future[Option[CacheMap]] =
    collection.find(Json.obj("id" -> id)).one[CacheMap]
}

@Singleton
class SessionRepository @Inject()(config: Configuration) {

  class DbConnection extends MongoDbConnection

  private lazy val sessionRepository = new ReactiveMongoRepository(config, new DbConnection().db)

  def apply(): ReactiveMongoRepository = sessionRepository
}
