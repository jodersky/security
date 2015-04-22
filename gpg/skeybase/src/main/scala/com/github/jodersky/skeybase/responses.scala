package com.github.jodersky.skeybase

case class PublicKey(fingerprint: String)
case class Proof(nametag: String, proofType: String, proofUrl: String)
case class Basics(username: String)
case class LookupResponse(them: Seq[User])
case class User(basics: Basics, proofs: Seq[Proof], key: PublicKey)