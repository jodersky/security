package com.github.jodersky.skeybase
package openpgp

import java.io.File
import scala.sys.process._
import java.io.ByteArrayInputStream
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

class GnuPG(
    val home: File = new File("."),
    val command: String = "gpg")
  extends Backend {

  import GnuPG._

  private val _gpg = s"${command} --home=${home.getAbsolutePath} --no-default-keyring --keyring=keybase.gpg --status-fd=2"
  private def gpg(args: String) = _gpg + " " + args

  def importKey(key: String) = {
    val result = (gpg("--import -") #< stream(key)).!
    result == 0
  }

  def verifySignature(message: String, fingerprint: String): Try[String] = Try{
    val stdout = new StringBuilder
    val stderr = new ArrayBuffer[String]

    val status = (gpg("-d -") #< stream(message)) ! ProcessLogger(stdout append _, stderr append _)
    
    if (status != 0) throw new VerificationException("GnuPG exited with non-zero exit code. Stderr: \n" + stderr.mkString("\n"))

    /* see doc/DETAILS of GnuPG for more information about structure */
    def fpr(line: String) = """\[GNUPG:\] VALIDSIG (\S+\s+){9}(\w+)""".r findPrefixMatchOf (line) map { m =>
      m.group(2)
    }
    
    val valid = stderr find (fpr(_) == Some(fingerprint))
    
    if (valid.isEmpty) {
      throw new VerificationException("Statement is not signed by the correct key.")
    } else {
      stdout.toString()
    }
  }

}

object GnuPG {

  private def stream(str: String) = {
    val bytes = str.getBytes("UTF-8")
    new ByteArrayInputStream(bytes)
  }

  val tmp = "~/.skeybase"
}