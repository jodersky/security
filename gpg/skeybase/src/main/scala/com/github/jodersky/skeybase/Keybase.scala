package com.github.jodersky.skeybase

import scala.concurrent.Future

import akka.actor.ActorSystem
import spray.client.pipelining.Get
import spray.client.pipelining.WithTransformerConcatenation
import spray.client.pipelining.sendReceive
import spray.client.pipelining.sendReceive$default$3
import spray.client.pipelining.unmarshal
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.json.DefaultJsonProtocol
import spray.json.DeserializationException
import spray.json.JsArray
import spray.json.JsObject
import spray.json.JsValue
import spray.json.RootJsonFormat

object Keybase {
  def origin = new Keybase("https://keybase.io")

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val basicsFormat = jsonFormat1(Basics.apply)
    implicit val proofFormat = jsonFormat(Proof.apply, "nametag", "proof_type", "proof_url")

    implicit object PrimaryKeyFormat extends RootJsonFormat[PublicKey] {
      def write(key: PublicKey) = throw new NotImplementedError
      def read(value: JsValue) = value.asJsObject.getFields("primary") match {
        case Seq(JsObject(data)) => data.get("key_fingerprint") map (f => PublicKey(f.convertTo[String].toUpperCase())) getOrElse {
          throw new DeserializationException("Fingerprint expected")
        }
        case _ => throw new DeserializationException("Primary key expected")
      }
    }

    implicit object ProofsFormat extends RootJsonFormat[Seq[Proof]] {
      def write(proofs: Seq[Proof]) = throw new NotImplementedError
      def read(value: JsValue) = value.asJsObject.getFields("all") match {
        case Seq(JsArray(values)) => values.map(_.convertTo[Proof])
        case _ => throw new DeserializationException("Proofs array expected")
      }
    }
    implicit val userFormat = jsonFormat(User.apply, "basics", "proofs_summary", "public_keys")
    implicit val lookupFormat = jsonFormat1(LookupResponse.apply)
  }
}

class Keybase(host: String) {

  import Keybase.JsonProtocol._

  def lookup(username: String)(implicit system: ActorSystem): Future[User] = {
    import system.dispatcher

    val lookup = sendReceive ~> unmarshal[LookupResponse]
    val url = host + "/_/api/1.0/user/lookup.json?usernames=" + username + "&fields=proofs_summary,public_keys"

    lookup(Get(url)).map(_.them.head)
  }

}