package com.github.jodersky.skeybase
package verification

import scala.concurrent.Future

import Verifier.extractSignedStatement
import Verifier.finalHost
import Verifier.verifyStatement
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
    val gistPipeline = withRedirects(sendReceive) ~> finalHost("api.github.com").tupled ~> unmarshal[Seq[Gist]] ~> urlOfHeadGist
    val rawPipeline = sendReceive ~> unmarshal[String]

    for (
      rawUrl <- gistPipeline(Get("https://api.github.com/users/" + proof.nametag + "/gists"));
      content <- rawPipeline(Get(rawUrl));
      signed <- extractSignedStatement(content);
      clear <- backend.verifySignature(signed, fingerprint);
      verified <- verifyStatement(clear, "github", proof.nametag)
    ) yield {
      proof
    }
  }

}
 