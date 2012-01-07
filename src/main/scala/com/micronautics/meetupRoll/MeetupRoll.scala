package com.micronautics.meetupRoll

import java.util.Properties
import java.net.URL
import scalax.io._
import scalax.io.JavaConverters._
import scala.util.Random


object MeetupRoll extends App {
  private val random = new Random()
  private val Names = """<span class="D_name">(\S+) (\S*)\n""".r
  private val properties = readProps
  private val meetupGroup = Option(properties.getProperty("meetupGroup")).orElse(Some("Bay-Area-Scala-Enthusiasts")).get
  private val eventId     = Option(properties.getProperty("eventId"))    .orElse(Some("44582312")).get
  
  private def groupUrl = "http://www.meetup.com/" + meetupGroup + "/events/" + eventId + "/printrsvp"
  private val attendeesPage = new URL(groupUrl).asInput.slurpString(Codec.UTF8)

  private def names = (for (m <- (Names findAllIn attendeesPage).matchData)
    yield m.group(1) + " " + m.group(2)).toIndexedSeq

  private def numNames = names.length
  
  private def randomName = names(random.nextInt(numNames))
  
  private def readProps = {
    val properties = new Properties()
    val in = MeetupRoll.getClass().getClassLoader().getResourceAsStream("meetup.properties")
    if (in!=null) {
      properties.load(in)
      in.close()
    }
    properties
  }
  
  val aPage = attendeesPage
  println("Parsed " + names.length + " names from " + groupUrl)
  while (true) {
    println("Winner: " + randomName)
    val line = Console.readLine("Type q to exit> ")
    if (line=="q")
      System.exit(0)
  }
}