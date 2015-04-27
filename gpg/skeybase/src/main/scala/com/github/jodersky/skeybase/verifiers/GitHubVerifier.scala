package com.github.jodersky.skeybase
package verifiers

import scala.concurrent.Future
import akka.actor.ActorSystem
import openpgp.Backend
import spray.client.pipelining.Get
import spray.client.pipelining.WithTransformerConcatenation
import spray.client.pipelining.sendReceive
import spray.client.pipelining.sendReceive$default$3
import spray.client.pipelining.unmarshal
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.json.DefaultJsonProtocol

object GitHubVerifier {
  case class GistFile(rawUrl: String)
  case class Gist(url: String, files: Map[String, GistFile])

  object GitHubProtocol extends DefaultJsonProtocol {
    implicit val gistFileFormat = jsonFormat(GistFile, "raw_url")
    implicit val gistFormat = jsonFormat2(Gist)
  }
}

class GitHubVerifier(backend: Backend) extends Verifier {
  import Verifier._
  import GitHubVerifier._
  import GitHubVerifier.GitHubProtocol._

  def verify(fingerprint: String, proof: Proof)(implicit sys: ActorSystem) = {
    import sys.dispatcher

    val urlOfHeadGist = (gists: Seq[Gist]) => {
      val url = for (
        gist <- gists.headOption;
        (_, file) <- gist.files.headOption
      ) yield {
        file.rawUrl
      }
      url getOrElse {
        throw new NoSuchElementException("No gist found.")
      }
    }
    val gistPipeline = withRedirects(sendReceive) ~> finalHost("api.github.com") ~> unmarshal[Seq[Gist]] ~> urlOfHeadGist
    val rawPipeline = sendReceive ~> unmarshal[String]

    val githubService = Service(
      name = Some("github"),
      username = Some(proof.nametag),
      hostname = None,
      domain = None)

    for (
      rawUrl <- gistPipeline(Get("https://api.github.com/users/" + proof.nametag + "/gists")); // url of raw gist
      content <- rawPipeline(Get(rawUrl)); // content of raw gist
      signed <- extractSignedMessage(content); // signed statement of ownership
      clear <- backend.verifySignature(signed, fingerprint); // verified against fingerprint
      verified <- verifyOwnershipStatement(clear, githubService) // verified statement
    ) yield {
      proof
    }
  }

}
 