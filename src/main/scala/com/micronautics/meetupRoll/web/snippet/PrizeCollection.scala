package com.micronautics.meetupRoll.web.snippet

import xml.NodeSeq
import com.typesafe.config.{ConfigObject, ConfigFactory}
import com.micronautics.meetupRoll.PrizeRules
import net.liftweb.http.SessionVar

/**
 * @author Julia Astakhova
 */
//object PrizeCollection {
//  object prizes extends SessionVar[Option[List[Prize]]](None)
//
//
//}

class PrizeCollection {

//  import ParticipantCrowd.actualNumberOfParticipants
//  import PrizeCollection._
//
//  private val sponsors = ConfigFactory.load("sponsors")
//  private val prizesData = sponsors.getList("prizeRules")
//  private val prizeRules = prizesData.toArray.toList.collect {case c:ConfigObject => c} .map (PrizeRules.apply)
//
//  prizes.set(Some(prizeRules.map(rule => Prize(
//    rule.name,
//    rule.forNumberOfParticipants(actualNumberOfParticipants.getOrElse
//      {throw new IllegalStateException("No actual number of participants specified")})))
//    .filter(_.quantity > 0)))
}

