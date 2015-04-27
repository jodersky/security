package com.github.jodersky.skeybase

/** A keybase user. */
case class User(basics: Basics, proofs: Seq[Proof], primaryKey: PublicKey)

/** Basic information about a user. */
case class Basics(username: String)

/**
 * An identity "proof" consisting of an online, cryptographically signed statement asserting
 * that a specific username of an online service has control of the signing key.
 * @param nametag the name of the user controlling the key
 * @param proofType string identifying the kind of proof, i.e. the service or website
 * @param proofUrl the URL under which the proof can be found
 */
case class Proof(nametag: String, proofType: String, proofUrl: String)

/** An OpenPGP public key belonging to a user, represented by its fingerprint*/
case class PublicKey(fingerprint: String)

/**
 * A statement, usually available in a signed form, that asserts
 * the ownership of a public key and service handle (i.e. a username
 * or an entire website).
 */
case class OwnershipStatement(body: StatementBody)

/** 
 * Actual meaningful contents of an ownership statement.
 */
case class StatementBody(key: PublicKey, service: Service)

/**
 * An online service, e.g. a social media handle or a website. 
 * Note: since keybase.io are inconsistent with their statements across various services, this class
 * provides only optional fields. It is up to verifiers to ensure that the correct fields are set.  
 */
case class Service(name: Option[String], username: Option[String], hostname: Option[String], domain: Option[String])
