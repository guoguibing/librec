package net.librec.spark.data

/**
  * Rating: basic information unit for ''user rate a item''
  *
  * @param user
  * @param item
  * @param rate
  *
  * @author WangYuFeng
  */
case class Rating(user: Int, item: Int, rate: Double) extends scala.AnyRef with scala.Product with Serializable {
  var time: String = _

  def this(user: Int, item: Int, rating: Double, time: String) {
    this(user, item, rating)
    this.time = time
  }

  override def hashCode(): Int = super.hashCode()

  override def equals(obj: scala.Any): Boolean = super.equals(obj)
}