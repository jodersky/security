package com.github.jodersky.skeybase

import scala.language.implicitConversions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import akka.actor.ActorSystem
import spray.http.HttpHeaders.Location
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.Uri
import spray.json.DefaultJsonProtocol
import spray.json.JsonParser

/** Verifies a user identity proof. */
trait Verifier {

  /** Checks if a given proof is actually signed by the provided key. */
  def verify(fingerprint: String, proof: Proof)(implicit sys: ActorSystem): Future[Proof]

}

/** Contains utilities for concrete verifiers. */
object Verifier {

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val serviceFormat = jsonFormat4(Service.apply)
    implicit val keyFormat = jsonFormat1(PublicKey.apply)
    implicit val statementBodyFormat = jsonFormat2(StatementBody.apply)
    implicit val statementFormat = jsonFormat1(OwnershipStatement.apply)
  }
  import JsonProtocol._

  /** Convert a try to a future, useful for writing expressive for-comprehensions mixing futures and tries. */
  implicit def tryToFuture[A](t: Try[A]): Future[A] = t match {
    case Success(a) => Future.successful(a)
    case Failure(e) => Future.failed(e)
  }

  /** Pipeline stage that follows redirects and also keeps track of the final URL. */
  def withRedirects(
    sendReceive: HttpRequest => Future[HttpResponse],
    maxRedirects: Int = 5)(implicit ec: ExecutionContext): HttpRequest => Future[(Uri, HttpResponse)] = { request =>

    def dispatch(request: HttpRequest, redirectsLeft: Int): Future[(Uri, HttpResponse)] = if (redirectsLeft <= 0) {
      Future.failed(new UnsupportedOperationException("Too many redirects."))
    } else {
      sendReceive(request).flatMap { response =>
        if (response.status.value.startsWith("3")) {
          response.header[Location].map { location =>
            dispatch(request.copy(uri = location.uri), redirectsLeft - 1)
          } getOrElse {
            Future.failed(new NoSuchElementException("Missing location header in redirect response."))
          }
        } else {
          Future.successful(request.uri, response)
        }
      }
    }

    dispatch(request, maxRedirects)
  }

  /** Pipeline stage that ensures the request's host matches a provided parameter. */
  def finalHost(host: String) = ((uri: Uri, response: HttpResponse) => {
    if (uri.authority.host.address != host)
      throw new VerificationException("Final host " + uri.authority.host.address + " is not " + host)
    else
      response
  }).tupled

  /** Extract an OpenPGP delimited message from a content. */
  def extractSignedMessage(content: String): Try[String] = Try {
    val header = "-----BEGIN PGP MESSAGE-----"
    val footer = "-----END PGP MESSAGE-----"
    val inner = content.lines.dropWhile(_ != header).takeWhile(_ != footer)
    if (inner.isEmpty) {
      throw new VerificationException("No OpenPGP message found.")
    } else {
      (inner ++ Seq(footer)).mkString("\n")
    }
  }

  /** Verify the contents of a statement of ownership against a known service. */
  def verifyOwnershipStatement(statement: String, goodService: Service): Try[OwnershipStatement] = Try {
    val stmt = JsonParser(statement).convertTo[OwnershipStatement]
    
    if (stmt.body.service != goodService) {
      throw new VerificationException("The statement of ownership does not match the required service.")
    } else {
      stmt
    }
  }

  /*
   * if (!(uri.path.tail startsWith (Path(proof.nametag)))) {
   *   throw new VerificationException("Final github account does not match the one provided in the proof." + uri.path.head)
   * }
   

  def extractHtmlId(id: String, html: String): Option[String] = {
    val cleaner = new HtmlCleaner
    val root = cleaner.clean(html)
    root.getElementsByName("div", true).find(_.getAttributeByName("id") == id).map { div =>
      StringEscapeUtils.unescapeHtml4(div.getText.toString())
    }
  }*/

}