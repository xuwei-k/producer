package org.atnos.producer
import Producer._
import org.scalacheck._
import org.specs2._
import cats._
import cats.data._
import cats.implicits._
import transducers._
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

class TransducerSpec extends Specification with ScalaCheck { def is = s2"""

  a producer can be modified by a transducer $transduced
  a producer can be modified by a receiver   $received
  a receiver can run another producer if the first one is empty  $receivedOr

  take(n) $takeN
  take(n) + exception $takeException

  zipWithPrevious        $zipWithPreviousElement
  zipWithNext            $zipWithNextElement
  zipWithPreviousAndNext $zipWithPreviousAndNextElement
  zipWithIndex           $zipWithIndex1
  intersperse            $intersperse1

"""
  type S = Fx.fx1[Safe]
  
  def transduced = prop { xs: List[Int] =>
    val f = (x: Int) => (x+ 1).toString

    (emit[S, Int](xs) |> transducer(f)).safeToList ==== xs.map(f)
  }.setGen(Gen.listOf(Gen.choose(1, 100)))

  def received = prop { xs: List[Int] =>
    val f = (x: Int) => (x+ 1).toString

    def plusOne[R :_safe] =
      receive[R, Int, String](a => one(f(a)))

    (emit[S, Int](xs) |> plusOne).safeToList ==== xs.map(f)
  }.setGen(Gen.listOf(Gen.choose(1, 100)))

  def receivedOr = prop { xs: List[Int] =>
    val f = (x: Int) => (x+ 1).toString

    def plusOne[R :_safe] =
      receiveOr[R, Int, String](a => one(f(a)))(emit(List("1", "2", "3")))

    (Producer.done[S, Int] |> plusOne).safeToList ==== List("1", "2", "3")
  }.setGen(Gen.listOf(Gen.choose(1, 100)))

  def receivedOption = prop { xs: List[Int] =>
    (emit[S, Int](xs) |> receiveOption).safeToList ==== xs.map(Option(_)) ++ List(None)
  }.setGen(Gen.listOf(Gen.choose(1, 100)))

  def takeN = prop { (xs: List[Int], n: Int) =>
    (emit[S, Int](xs) |> take(n)).safeToList ==== xs.take(n)
  }.setGen2(Gen.choose(0, 10))

  def takeException = prop { n: Int =>
    type R = Fx.fx2[WriterInt, Safe]

    val producer = emit[R, Int](List(1)) append emitEff[R, Int](protect { throw new Exception("boom"); List(1) })
    (producer |> take(1)).runLog ==== List(1)
  }

  def zipWithNextElement = prop { xs: List[Int] =>
    (emit[S, Int](xs) |> zipWithNext).safeToList ==== (xs zip (xs.drop(1).map(Option(_)) :+ None))
  }

  def zipWithPreviousElement = prop { xs: List[Int] =>
    (emit[S, Int](xs) |> zipWithPrevious).safeToList ==== ((None +: xs.dropRight(1).map(Option(_))) zip xs)
  }

  def zipWithPreviousAndNextElement = prop { xs: List[Int] =>
    val ls = emit[S, Int](xs)
    (ls |> zipWithPreviousAndNext).safeToList ====
      (ls |> zipWithPrevious).zip(ls |> zipWithNext).map { case ((previous, a), (_, next)) => (previous, a, next) }.safeToList
  }

  def zipWithIndex1 = prop { xs: List[Int] =>
    emit[S, Int](xs).zipWithIndex.safeToList ==== xs.zipWithIndex
  }

  def intersperse1 = prop { (xs: List[String], c: String) =>
    emit[S, String](xs).intersperse(c).safeToList ==== intersperse(xs, c)
  }.noShrink.setGens(Gen.choose(0, 5).flatMap(n => Gen.listOfN(n, Gen.identifier)), Gen.oneOf("-", "_", ":"))

  /**
   * HELPERS
   */

  type WriterInt[A]    = Writer[Int, A]

  implicit class ProducerOperations[W](p: Producer[Fx2[Writer[W, ?], Safe], W]) {
    def runLog: List[W] =
      collect[Fx2[Writer[W, ?], Safe], W](p).runWriterLog.runSafe.run._1.toOption.get
  }

  implicit class ProducerOperations2[W, U[_]](p: Producer[Fx3[Writer[W, ?], U, Safe], W]) {
    def runLog =
      collect[Fx3[Writer[W, ?], U, Safe], W](p).runWriterLog.runSafe.map(_._1.toOption.get)
  }

  implicit class ProducerOperations3[A](p: Producer[Fx1[Safe], A]) {
    def safeToList =
      p.runList.runSafe.run._1.toOption.get
  }

  def intersperse[A](as: List[A], a: A): List[A] =
    if (as.isEmpty) Nil else as.init.foldRight(as.last +: Nil)(_ +: a +: _)


}

