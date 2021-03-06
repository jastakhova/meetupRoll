package com.micronautics.meetupRoll.web.snippet

import net.liftweb._
import http._
import util.Helpers._
import js._
import JsCmds._
import JE._
import scala.collection.immutable.ListMap
import http.SHtml._
import xml.{NodeSeq, Elem}
import net.liftweb.util._
import com.typesafe.config.{ConfigObject, ConfigFactory}
import com.micronautics.meetupRoll.PrizeRules
import com.micronautics.meetupRoll.web.snippet.ParticipantCrowd.actualNumberOfParticipants
import com.micronautics.util.Mailer
import scala.Some
import scala.util.{Try, Success, Failure}
import scalax.io._
import java.io.File
import com.github.nscala_time.time.Imports._

/**
 * @author Julia Astakhova
 */
object WinnerChoice {

  object winners extends SessionVar[List[Winner]](List[Winner]())
  object prizeList extends SessionVar[List[Prize]](List[Prize]())
  object remainingPrizes extends SessionVar[Option[Map[Prize, Int]]](None)

  def reload() {
    def loadPrizesFromConfig() {
      val sponsors = ConfigFactory.load("sponsors")
      val prizesData = sponsors.getList("prizeRules")
      val prizeRules = prizesData.toArray.toList.collect {case c:ConfigObject => c} .map (PrizeRules.apply)

      prizeList.set(prizeRules.map(rule => Prize(
        rule.name,
        rule.forNumberOfParticipants(actualNumberOfParticipants.getOrElse
        {throw new IllegalStateException("No actual number of participants specified")})))
        .filter(_.quantity > 0))
    }

    if (prizeList.get.isEmpty)
      loadPrizesFromConfig()

    Meetup.reloadParticipantCrowd()
    remainingPrizes.set(Some(Map() ++ prizeList.get.map(prize => (prize -> prize.quantity))))
    winners.set(List[Winner]())
  }
}

class WinnerChoice {

  import WinnerChoice._

  var currentWinner: Option[Attendant] = Meetup.pickWinner()

  def currentWinnerNode = currentWinner.map{winner => <span>{winner.name}</span>}

  def sendNode: NodeSeq = {
    def sendButtonNode =
      ajaxButton("Send", () => {
        def alertResults(message2error: Map[Pair[String, String], Try[Unit]]): JsCmd = {
          message2error.values.filter(_.isFailure).foreach(_.failed.get.printStackTrace())
          val alertNodes = message2error.map{
            case (message, Success(_)) => NodeUtil.alertSuccess(message._1)
            case (message, Failure(error)) => NodeUtil.alertError(message._2 + " [" + error.getMessage + "]")
          }.foldLeft(<span/>){case (res, node) => <span><span>{res}</span><span>{node}</span></span>}
          SetHtml("send", <span>{alertNodes}</span><span>{sendNode}</span>)
        }

        val meetupName = Meetup.chosenMeetup.get.map(_.title).getOrElse("meetup")

        val winString = winners.get map (w => (w.person.name + ": " + w.prize)) mkString(
          "Winners for " + meetupName + " at " + DateTime.now + " are:\n  ", "\n  ", "\n")

        val trySaveLocally = Meetup.chosenMeetup.get.flatMap(meetup => Try {
          val pathToSaveLocally = RealMeetupAPI.config.map(_.getString("localPath")).getOrElse("./")
          val pathDirectory = new File(pathToSaveLocally)
          if (!pathDirectory.exists()) {
            throw new RuntimeException("Not enough permissions to create a directory for the local save action")
          }
          val fileName = pathToSaveLocally + meetup.id + ".txt"
          Resource.fromFile(fileName).write(winString)(Codec.UTF8)
          println("========================");
          println("Data was saved to " + new File(fileName).getAbsolutePath);
          println("========================");
        })

        val trySend = EmailSettingsPage.emailSettings.get.flatMap(settings => Try {
          new Mailer().sendMail(
            settings.email, settings.smtpHost, settings.smtpSender, settings.smtpPwd, "Giveaway winners", winString)
        })

        val message2try = Map(
          Pair("The letter was successfully sent", "Problem trying to send the letter") -> trySend,
          Pair("The winners list was saved locally", "Problem with saving locally") -> trySaveLocally
        )

        if (message2try.values.forall(_.isSuccess)) {
          SetHtml("send", NodeUtil.alertSuccess("The letter was successfully sent and saved locally."))
        } else {
          alertResults(message2try)
        }
      }, "class" -> "btn ovalbtn btn-success")

    <span>{sendButtonNode}</span><span class="help-inline">
      Email is {EmailSettingsPage.emailSettings.get.map(_.email).getOrElse("undefined")}</span>
  }

  private def updateWinner(): JsCmd = {
    currentWinner = Meetup.pickWinner()
    if (remainingPrizes.get.get.isEmpty || currentWinner.isEmpty)
      JsHideId("winnerChoice") & SetHtml("winners", winnersNode) & SetHtml("send", sendNode)
    else
      SetHtml("currentWinner", currentWinnerNode.get) & SetHtml("winners", winnersNode) &
        SetHtml("currentPrizes", currentPrizesAndPhotoNode)
  }

  def currentPrizesAndPhotoNode = {
    val remainingPrizeList = List() ++ remainingPrizes.get.get.keys

    def prizeButtonNode(prize: Prize) =
      ajaxButton(prize.name, () => {
        winners.set(Winner(currentWinner.get, prize.name) :: winners.get)
        val remainingQuantity: Int = remainingPrizes.get.get(prize)
        remainingPrizes.set(Some(remainingPrizes.get.get - prize))
        if (remainingQuantity > 1)
          remainingPrizes.set(Some(new ListMap() + (prize -> (remainingQuantity - 1)) ++ remainingPrizes.get.get))
        updateWinner()
      }, "class" -> "btn btn-success ovalbtn prizelabel")
    <span class="row">
      {
        currentWinner.get.photo.map(photo =>
                  <span class="span2 prizephoto"><img src={photo}/></span>
            ).getOrElse(
                  <span class="span2"/>)
      }
      <div class="span4 offset2 well prizes">
        <h4 class="prizelabel text-center">Prizes</h4>

        <ol>{remainingPrizeList.sortBy(_.name).map(prize => <li>{prizeButtonNode(prize)}</li>)}</ol>
      </div>
    </span>
  }

  def winnersNode =
    <table class="table table-striped">
      {winners.get.sortBy(_.prize).map(winner =>
        <tr>
          {winner.person.thumbnail.map(photo => <td><img src={photo} width="40"/></td>).getOrElse(<td/>)}
          <td><strong>{winner.person.name}</strong></td>
          <td>won</td>
          <td><strong>{winner.prize}</strong></td>
        </tr>)
      }
  </table>


  def render = {
    remainingPrizes.get match {
      case None => S.redirectTo("/")
      case Some(prizes) => {
        val choice: CssSel = if (!prizes.isEmpty && currentWinner.isDefined) {
          "@text1" #> "is a winner. Choose a prize:" &
            "@text2" #> "Or mark if the person is not here:" &
            "@currentPrizes" #> currentPrizesAndPhotoNode &
            "@currentWinner" #> currentWinnerNode &
            "@choiceNo" #> ajaxButton("Not here", () => updateWinner, "class" -> "ovalbtn btn btn-danger")
        } else if (!winners.get.isEmpty)
          "@send" #> sendNode
        else
          ClearClearable

        "@winners" #> winnersNode & choice
      }
    }
  }
}

case class Winner(person: Attendant, prize: String)

case class Prize(name: String, quantity: Int)
