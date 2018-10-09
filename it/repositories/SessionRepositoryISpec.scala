package repositories

import helpers.IntegrationSpecBase
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.MongoConnector

import scala.concurrent.ExecutionContext.Implicits.global


class SessionRepositoryISpec extends IntegrationSpecBase {

  class Setup {
    val newMongoInstance = new ReactiveMongoRepository(app.injector.instanceOf[Configuration], mongo)

    val record1 = CacheMap("1",Map("foo" -> Json.obj("" -> "")))
    val record2 = CacheMap("2",Map("foo" -> Json.obj("" -> "")))
    val record3 = CacheMap("3",Map("foo" -> Json.obj("wuzz" -> "buzz"),"bar" -> Json.obj("fudge" -> "wizz")))
    val record3_updated = CacheMap("3",Map("bar" -> Json.obj("fudge" -> "wizz")))
    val record4 = CacheMap("4",Map("foo" -> Json.obj("wuzz" -> "buzz"),"bar" -> Json.obj("fudge" -> "wizz")))
  }


  "dropCollectionIndexTemp" should {
    "drop collection , create index, drop index using dropCollectionIndexTemp then attempt to create index again Successfully" in new Setup {
      await(newMongoInstance.drop)
      await(newMongoInstance.collection.indexesManager.list()).isEmpty mustBe true
      await(newMongoInstance.createIndex(newMongoInstance.fieldName, newMongoInstance.createdIndexName,10))
      await(newMongoInstance.collection.indexesManager.list()).nonEmpty mustBe true
      await(newMongoInstance.dropCollectionIndexTemp) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).nonEmpty mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe false
      await(newMongoInstance.createIndex(newMongoInstance.fieldName, newMongoInstance.createdIndexName,129)) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe true
    }
    "drop and create works sucessfully when documents are in the collection" in new Setup {
      await(newMongoInstance.collection.indexesManager.list()).nonEmpty mustBe true
      await(newMongoInstance.upsert(record1)) mustBe true
      await(newMongoInstance.upsert(record2)) mustBe true
      await(newMongoInstance.count) mustBe 2
      await(newMongoInstance.dropCollectionIndexTemp) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe false
      await(newMongoInstance.createIndex(newMongoInstance.fieldName, newMongoInstance.createdIndexName,129)) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe true
      await(newMongoInstance.count) mustBe 2
    }
  }
  "removeEntry" should {
    "throw an exception if the document does not exist in the repository" in new Setup {
      await(newMongoInstance.drop)
      intercept[Exception](await(newMongoInstance.removeEntry("int1","bar")))
    }
    "update the document to unset the appropriate key with other records in the db return document" in new Setup {
      await(newMongoInstance.drop)
      await(newMongoInstance.upsert(record4))
      await(newMongoInstance.upsert(record3))
      await(newMongoInstance.count) mustBe 2
      await(newMongoInstance.removeEntry("3","foo")) mustBe  record3_updated
      await(newMongoInstance.count) mustBe 2
      await(newMongoInstance.get("4")) mustBe Some(record4)

    }
    "update the document to unset a key that does not exist in mongo. return document" in new Setup {
      await(newMongoInstance.drop)
      await(newMongoInstance.upsert(record1))
      await(newMongoInstance.upsert(record3))
      await(newMongoInstance.count) mustBe 2
      await(newMongoInstance.removeEntry("3","doesNotExist")) mustBe  record3
      await(newMongoInstance.count) mustBe 2
    }
  }
}