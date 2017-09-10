package observatory

import scala.annotation.tailrec

object KDTree {

  import Numeric._

  case class KDPoint[T, V](dimensions: Seq[T], data: V)

  // Task 1A. Build tree of KDNodes. Translated from Wikipedia.
  def apply[T, V](points: Seq[KDPoint[T, V]], depth: Int = 0)(implicit num: Numeric[T]): Option[KDNode[T, V]] = {
    if (points.isEmpty ) {
      None
    }
    else {
      val dim = points.head.dimensions.size
      if( dim < 1 ) { None }
      else {
        val axis = depth % dim
        val sorted = points.sortBy(_.dimensions(axis))
        val median = sorted(sorted.size / 2).dimensions(axis)

        val (left, right) = sorted.partition(v => num.lt(v.dimensions(axis), median))

        Some(KDNode(right.head, apply(left, depth + 1), apply(right.tail, depth + 1), axis))
      }
    }
  }

  // Task 1B. Find the nearest node in this subtree. Translated from Wikipedia.
  case class KDNode[T, V](value: KDPoint[T, V], left: Option[KDNode[T, V]], right: Option[KDNode[T, V]], axis: Int)(implicit num: Numeric[T]) {
    def nearest(to: Seq[T]): Nearest[T, V] = {
      val default = Nearest(value, to , Set(this)  )
      compare(to, value.dimensions) match {
        case 0 => default // exact match
        case t =>
          lazy val bestL = left.map(_ nearest to).getOrElse(default)
          lazy val bestR = right.map(_ nearest to).getOrElse(default)


          val branch1 = if (t < 0) bestL else bestR
          val best = if (num.lt(branch1.distsq, default.distsq)) branch1 else default
          val splitDist = num.minus(to(axis), value.dimensions(axis))
          if (num.lt(num.times(splitDist, splitDist), best.distsq)) {
            val branch2 = if (t < 0) bestR else bestL
            val visited = branch2.visited ++ best.visited + this
            if (num.lt(branch2.distsq, best.distsq)) {
              branch2.copy(visited = visited)
            }
            else {
              best.copy(visited = visited)
            }
          } else {
            best.copy(visited = best.visited + this)
          }
      }
    }
  }

  // Keep track of nodes visited, as per task. Pretty-printable.
  case class Nearest[T, V](value: KDPoint[T, V], to: Seq[T] , visited: Set[KDNode[T, V]] = Set[KDNode[T, V]]()  )(implicit num: Numeric[T]) {
    lazy val distsq = KDTree.distsq(value.dimensions, to)

    // override def toString = f"Searched for=${to} found=${value} distance=${math.sqrt(num.toDouble(distsq))}%.4f visited=${visited.size}"
  }

  // Numeric utilities
  def distsq[T](a: Seq[T], b: Seq[T])(implicit num: Numeric[T]): T = {

    @tailrec
    def childDistSq[T](a: Seq[T], b: Seq[T], acum: T)(implicit num: Numeric[T]): T = {
      if (a.isEmpty) {
        acum
      }
      else {
        val ac = num.plus(acum, num.times(num.minus(a.head, b.head), num.minus(a.head, b.head)))
        childDistSq(a.tail, b.tail, ac)
      }
    }

    childDistSq(a, b, num.zero)
  }


  //  def distsq[T](a: Seq[T], b: Seq[T])(implicit num: Numeric[T]) =
  //    a.zip(b).map(c => num.times(num.minus(c._1, c._2), num.minus(c._1, c._2))).sum

  def compare[T](a: Seq[T], b: Seq[T])(implicit num: Numeric[T]): Int = {

    @tailrec
    def childComp[T](a: Seq[T], b: Seq[T])(implicit num: Numeric[T]): Int = {
      if (a.isEmpty) {
        0
      }
      else {
        val comp = num.compare(a.head, b.head)
        if (comp != 0) {
          comp
        }
        else {
          childComp(a.tail, b.tail)
        }
      }
    }

    childComp(a, b)
  }

  //  def compare[T](a: Seq[T], b: Seq[T])(implicit num: Numeric[T]): Int =
  //    a.zip(b).find(c => num.compare(c._1, c._2) != 0).map(c => num.compare(c._1, c._2)).getOrElse(0)
}
