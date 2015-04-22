package com.github.jodersky.skeybase
package verification

import scala.language.implicitConversions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.github.jodersky.skeybase.Proof
import com.github.jodersky.skeybase.PublicKey

import akka.actor.ActorSystem
import spray.http.HttpHeaders.Location
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.Uri
import spray.json.DefaultJsonProtocol
import spray.json.JsonParser
import spray.json.ParserInput.apply

trait Verifier {

  def verify(fingerprint: String, proof: Proof)(implicit sys: ActorSystem): Future[Proof]

}

object Verifier {

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val serviceFormat = jsonFormat2(Service.apply)
    implicit val keyFormat = jsonFormat1(PublicKey.apply)
    implicit val statementBodyFormat = jsonFormat2(StatementBody.apply)
    implicit val statementFormat = jsonFormat1(Statement.apply)
  }
  import JsonProtocol._

  implicit def tryToFuture[A](t: Try[A]): Future[A] = t match {
    case Success(a) => Future.successful(a)
    case Failure(e) => Future.failed(e)
  }

  def withRedirects(
    sendReceive: HttpRequest => Future[HttpResponse],
    maxRedirects: Int = 5)(implicit ec: ExecutionContext): HttpRequest => Future[(Uri, HttpResponse)] = { request =>

    def dispatch(request: HttpRequest, redirectsLeft: Int): Future[(Uri, HttpResponse)] = if (redirectsLeft <= 0) {
      Future.failed(new RuntimeException("Too many redirects."))
    } else {
      sendReceive(request).flatMap { response =>
        if (response.status.value.startsWith("3")) {
          response.header[Location].map { location =>
            dispatch(request.copy(uri = location.uri), redirectsLeft - 1)
          } getOrElse {
            Future.failed(new RuntimeException("Missing location header in redirect response."))
          }
        } else {
          Future.successful(request.uri, response)
        }
      }
    }

    dispatch(request, maxRedirects)
  }

  def finalHost(host: String) = (uri: Uri, response: HttpResponse) => {
    if (uri.authority.host.address != host)
      throw new VerificationException("Final host is not " + host)
    else
      response
  }

  def extractSignedStatement(content: String): Try[String] = Try {
    val regex = """(-----BEGIN PGP MESSAGE-----(.|\n)*-----END PGP MESSAGE-----?)""".r
    regex.findFirstIn(content) getOrElse {
      throw new VerificationException("No OpenPGP message found.")
    }
  }

  def verifyStatement(statement: String, service: String, username: String): Try[String] = Try {
    val stmt = JsonParser(statement).convertTo[Statement]

    if (stmt.body.service.name != service) throw new VerificationException(
      "The service specified in the signed statement (" + stmt.body.service.name + ") is not " +
        "the same as the service under which the statement was found (" + service + ")")
    else if (stmt.body.service.username != username) throw new VerificationException(
      "The username specified in the signed statement (" + stmt.body.service.username + ") is not " +
        "the same as the username under which the statement was found (" + username + ")")
    else statement

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