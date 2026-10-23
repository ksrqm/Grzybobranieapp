package pl.grzybtrop.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline sunset calculation (the classic "Almanac for Computers" algorithm).
 * Accurate to about a minute, needs no network, only a position and a date.
 */
object SunCalc {

    private const val ZENITH_DEG = 90.833 // official sunrise/sunset zenith

    /** Sunset as an [Instant], or null when the sun does not set (polar day/night). */
    private fun compute(date: LocalDate, latitude: Double, longitude: Double, rising: Boolean): Instant? {
        val lngHour = longitude / 15.0
        val t = date.dayOfYear + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0

        val meanAnomaly = 0.9856 * t - 3.289
        val trueLongitude = norm360(
            meanAnomaly +
                1.916 * sin(rad(meanAnomaly)) +
                0.020 * sin(rad(2 * meanAnomaly)) +
                282.634
        )

        var rightAscension = norm360(deg(atan(0.91764 * tan(rad(trueLongitude)))))
        val lQuadrant = floor(trueLongitude / 90.0) * 90.0
        val raQuadrant = floor(rightAscension / 90.0) * 90.0
        rightAscension = (rightAscension + (lQuadrant - raQuadrant)) / 15.0

        val sinDec = 0.39782 * sin(rad(trueLongitude))
        val cosDec = cos(asin(sinDec))

        val cosH = (cos(rad(ZENITH_DEG)) - sinDec * sin(rad(latitude))) /
            (cosDec * cos(rad(latitude)))
        if (cosH > 1.0 || cosH < -1.0) return null

        val hourAngle = (if (rising) 360.0 - deg(acos(cosH)) else deg(acos(cosH))) / 15.0
        val localMeanTime = hourAngle + rightAscension - 0.06571 * t - 6.622
        val utHours = norm24(localMeanTime - lngHour)

        return date.atStartOfDay().toInstant(ZoneOffset.UTC)
            .plusMillis((utHours * 3_600_000.0).toLong())
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun norm360(v: Double) = ((v % 360.0) + 360.0) % 360.0
    private fun norm24(v: Double) = ((v % 24.0) + 24.0) % 24.0

    fun sunrise(date: LocalDate, latitude: Double, longitude: Double) =
        compute(date, latitude, longitude, rising = true)
    fun sunset(date: LocalDate, latitude: Double, longitude: Double) =
        compute(date, latitude, longitude, rising = false)
}
