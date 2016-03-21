package marketsobsessed.utils

import java.util.{Date, Calendar}

/**
  * Created by sgandhi on 2/23/16.
  */
object DateUtils {

  val WEEK_DAYS = 7

  def toLastHourTime(timestamp: Long): Long = {
    val cal = Calendar.getInstance()
    cal.setTime(new Date(timestamp))
    cal.add(Calendar.HOUR_OF_DAY, -1) // subtracts one hour
    cal.getTimeInMillis
  }

  def toLastYearTime(timestamp: Long): Long = {
    val cal = Calendar.getInstance()
    cal.setTime(new Date(timestamp))
    cal.add(Calendar.YEAR, -1) // subtracts one year
    cal.getTimeInMillis
  }

  def toMidNightTimestamp = (timestamp: Long) => {
    val cal = Calendar.getInstance()
    cal.setTime(new Date(timestamp))
    cal.set(Calendar.HOUR, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTimeInMillis
  }

  def toTopOfTheHourTimestamp = (timestamp: Long) => {
    val cal = Calendar.getInstance()
    cal.setTime(new Date(timestamp))
    cal.set(Calendar.HOUR, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTimeInMillis
  }

  def to5MinIntervalTimestamp = (timestamp: Long) => {
    val cal = Calendar.getInstance()
    cal.setTime(new Date(timestamp))
    val newMinute = cal.get(Calendar.MINUTE) - (cal.get(Calendar.MINUTE) % 5)
    cal.set(Calendar.MINUTE, newMinute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTimeInMillis
  }

  def getLastWeekDate: Long = {
    val cal = Calendar.getInstance()
    cal.setTime(new Date())
    cal.add(Calendar.DAY_OF_MONTH, -WEEK_DAYS) // subtracts seven days
    cal.getTimeInMillis
  }

  def getCurrentTime: Long = {
    val cal = Calendar.getInstance()
    cal.setTime(new Date())
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTimeInMillis
  }

  def toYesterdayTime(epoch: Long): Long = {
    val cal = Calendar.getInstance()
    cal.setTime(new Date(epoch))
    cal.add(Calendar.DAY_OF_MONTH, -1) // subtracts one day
    cal.getTimeInMillis
  }

  def isSameDay(date1: Date, date2: Date): Boolean = {
    val cal1 = Calendar.getInstance()
    cal1.setTime(date1)
    val cal2 = Calendar.getInstance()
    cal2.setTime(date2)
    cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
      cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
      cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
  }


}
