package com.github.jodersky.skeybase
package verifiers

import scala.concurrent.Future
import Verifier.withRedirects
import akka.actor.ActorSystem
import openpgp.Backend
import spray.client.pipelining.Get
import spray.client.pipelining.WithTransformerConcatenation
import spray.client.pipelining.sendReceive
import spray.client.pipelining.sendReceive$default$3
import spray.client.pipelining.unmarshal
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.json.DefaultJsonProtocol

class WebsiteFileVerifier(backend: Backend) extends Verifier {
  import Verifier._

  def verify(fingerprint: String, proof: Proof)(implicit sys: ActorSystem) = {
    import sys.dispatcher

    val pipeline = withRedirects(sendReceive) ~> finalHost(proof.nametag) ~> unmarshal[String]

    val service = Service(
      name = None,
      username = None,
      hostname = Some(proof.nametag),
      domain = None)

    for (
      content <- pipeline(Get(proof.proofUrl));
      signed <- extractSignedMessage(content);
      clear <- backend.verifySignature(signed, fingerprint);
      verified <- verifyOwnershipStatement(clear, service)
    ) yield {
      proof
    }
  }

}
 