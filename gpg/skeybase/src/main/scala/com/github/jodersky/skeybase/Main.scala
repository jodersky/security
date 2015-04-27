package com.github.jodersky.skeybase

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.ActorSystem
import scala.language.implicitConversions
import scala.util.Success
import scala.util.Failure
import openpgp.GnuPG
import verifiers._

object Main {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    import system.dispatcher

    val verifier = new WebsiteFileVerifier(new GnuPG()) //new GitHubVerifier(new GnuPG())

    val proofs = for (
      user <- new Keybase().lookup("jodersky");
      github = user.proofs.find(_.proofType == "generic_web_site").get;
      verification <- verifier.verify(user.primaryKey.fingerprint, github)
    ) yield {
      verification
    }

    proofs onComplete { result =>
      result match {
        case Success(proof) => println("done")
        case Failure(err: VerificationException) => println("Verification exception! Someone may be doing something evil. " + err.getMessage) 
        case Failure(err) => err.printStackTrace()
      }
      system.shutdown()
    }
  }

}