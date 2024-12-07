package com.wmstein.egm

import android.content.Context

import com.wmstein.tourcount.R

import java.io.IOException
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.nio.charset.StandardCharsets
import java.util.StringTokenizer

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/******************
 *    Derived from:
 *    GeoTools - The Open Source Java GIS Toolkit
 *    https://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    The original file is derived from NGA/NASA software available for unlimited distribution.
 *    See https://earth-info.nima.mil/GandG/wgs84/gravitymod/.
 *
 * Transforms vertical coordinates using coefficients from the
 * https://earth-info.nima.mil/GandG/wgs84/gravitymod/wgs84_180/wgs84_180.html
 * Earth Gravitational Model.
 *
 * @author Pierre Cardinal
 * @author Martin Desruisseaux
 * @version $Id$
 * @since 2.3
 *
 * Code adaptation for use by TourCount by wm.stein on 2017-08-22,
 * last edit in Java on 2020-04-17,
 * converted to Kotlin on 2023-07-05,
 * last edit on 2024-11-19
 */
// Maximum degree and order attained.
class EarthGravitationalModel @JvmOverloads constructor(
    private val nmax: Int = DEFAULT_ORDER) : VerticalTransform() {

    // WGS 84 semi-major axis.
    private val semiMajor = 6378137.0

    // The first Eccentricity Squared (e²) for WGS 84 ellipsoid.
    private val esq = 0.00669437999013

    // Even zonal coefficient.
    private val c2 = 108262.9989050e-8

    // WGS 84 Earth's Gravitational Constant w/ atmosphere.
    private val rkm = 3.986004418e+14

    // Theoretical (Normal) Gravity at the Equator (on the Ellipsoid).
    private val grava = 9.7803267714

    // Theoretical (Normal) Gravity Formula Constant.
    private val star = 0.001931851386

    /**
     * The geopotential coefficients read from the ASCII file.
     * Those arrays are filled by the load method.
     */
    private val cnmGeopCoef: DoubleArray
    private val snmGeopCoef: DoubleArray

    /**
     * Cleanshaw coefficients needed for the selected gravimetric quantities that are computed.
     * Those arrays are computed by the initialize method.
     */
    private val aClenshaw: DoubleArray
    private val bClenshaw: DoubleArray
    private val `as`: DoubleArray

    /**
     * Temporary buffer for use by heightOffset only. Allocated once for ever
     * for avoiding too many objects creation / destruction.
     */
    private val cr: DoubleArray
    private val sr: DoubleArray
    private val s11: DoubleArray
    private val s12: DoubleArray

    // Creates a model with the default maximum degree and order.
    init {
        /**
         * WGS84 model values.
         * NOTE: The Fortran program gives 3.9860015e+14 for 'rkm' constant. This value has been
         * modified in later programs. From http://cddis.gsfc.nasa.gov/926/egm96/doc/S11.HTML :
         *
         *     "We next need to consider the determination of GM, GM0, W0, U0. The value of GM0
         *      will be that adopted for the updated GM of the WGS84 ellipsoid. This value is
         *      3.986004418e+14 m³/s², which is identical to that given in the IERS Numerical
         *      Standards [McCarthy, 1996, Table 4.1]. The best estimate of GM can be taken as
         *      the same value based on the recommendations of the IAG Special Commission SC3,
         *      Fundamental Constants [Bursa, 1995b, p. 381]."
         */
        val cleanshawLength = locatingArray(nmax + 3)
        val geopCoefLength = locatingArray(nmax + 1)
        aClenshaw = DoubleArray(cleanshawLength)
        bClenshaw = DoubleArray(cleanshawLength)
        cnmGeopCoef = DoubleArray(geopCoefLength)
        snmGeopCoef = DoubleArray(geopCoefLength)
        `as` = DoubleArray(nmax + 1)
        cr = DoubleArray(nmax + 1)
        sr = DoubleArray(nmax + 1)
        s11 = DoubleArray(nmax + 3)
        s12 = DoubleArray(nmax + 3)
    }

    /**
     * Loads the coefficients from the specified ASCII file and initialize the internal
     * clenshaw arrays.
     *
     * Note: ASCII may looks like an unefficient format for binary distribution.
     * A binary file with coefficient values read by java.io.DataInput readDouble would
     * be more compact than an uncompressed ASCII file. However, binary files are hard to
     * compress by the ZIP algorithm. Our experience show that a 675 kb uncompressed ASCII file
     * is only 222 kb after ZIP or JAR compression. The same data as a binary file is 257 kb
     * uncompressed and 248 kb compressed. So surprisingly, the ASCII file is more compact than
     * the binary file after compression. Since it is the primary format provided by the
     * Earth-Info web site, we use it directly in order to avoid a multiplication of formats.
     */
    @Throws(IOException::class)
    fun load(context: Context) {
        val egm = context.resources.openRawResource(R.raw.egm180)
        val `in` = LineNumberReader(InputStreamReader(egm, StandardCharsets.ISO_8859_1))
        var line: String?
        while (`in`.readLine().also { line = it } != null) {
            val tokens = StringTokenizer(line)
            try {
                /**
                 * Note: we use 'parseShort' instead of 'parseInt' as an easy way to ensure that
                 *       the values are in some reasonable range. The range is typically [0..180].
                 *       We don't check that, but at least 'parseShort' disallows values greater
                 *       than 32767. Additional note: we real all lines in all cases even if we
                 *       discard some of them, in order to check the file format.
                 */
                val n = tokens.nextToken().toShort().toInt()
                val m = tokens.nextToken().toShort().toInt()
                val cbar = tokens.nextToken().toDouble()
                val sbar = tokens.nextToken().toDouble()
                if (n <= nmax) {
                    val ll = locatingArray(n) + m
                    cnmGeopCoef[ll] = cbar
                    snmGeopCoef[ll] = sbar
                }
            } catch (cause: RuntimeException) {
                /**
                 * Catch the following exceptions:
                 *   - NoSuchElementException      if a line has too few numbers.
                 *   - NumberFormatException       if a number can't be parsed.
                 *   - IndexOutOfBoundsException   if 'n' or 'm' values are illegal.
                 */
                throw IOException("egm180" + `in`.lineNumber, cause)
            }
        }
        `in`.close()
        initialize()
    }

    /**
     * Computes the clenshaw arrays after all coefficients have been read.
     * We performs this step in a separated method than {from} in case we wish
     * to read the coefficient from an other source than an ASCII file in some future
     * version.
     */
    private fun initialize() {
        /**
         * MODIFY CNM EVEN ZONAL COEFFICIENTS.
         */
        val c2n = DoubleArray(6)
        c2n[1] = c2
        var sign = 1
        var esqi = esq
        for (i in 2 until c2n.size) {
            sign *= -1
            esqi *= esq
            c2n[i] = sign * (3 * esqi) / ((2 * i + 1) * (2 * i + 3)) * (1 - i + 5 * i * c2 / esq)
        }
        // all nmax
        cnmGeopCoef[3] += c2n[1] / SQRT_05
        cnmGeopCoef[10] += c2n[2] / 3
        cnmGeopCoef[21] += c2n[3] / SQRT_13
        if (nmax > 6) cnmGeopCoef[36] += c2n[4] / SQRT_17
        if (nmax > 9) cnmGeopCoef[55] += c2n[5] / SQRT_21

        /**
         * BUILD ALL CLENSHAW COEFFICIENT ARRAYS.
         */
        for (i in 0..nmax) {
            `as`[i] = -sqrt(1.0 + 1.0 / (2 * (i + 1)))
        }
        for (i in 0..nmax) {
            for (j in i + 1..nmax) {
                val ll = locatingArray(j) + i
                val n = 2 * j + 1
                val ji = (j - i) * (j + i)
                aClenshaw[ll] = sqrt(n * (2 * j - 1) / ji.toDouble())
                bClenshaw[ll] =
                    sqrt(n * (j + i - 1) * (j - i - 1) / (ji * (2 * j - 3)).toDouble())
            }
        }
    }

    /**
     * Returns the value to add to a height above the ellipsoid in order to get a
     * height above the geoid for the specified geographic coordinate.
     *
     * @param longitude The geodetic longitude, in decimal degrees.
     * @param latitude  The geodetic latitude, in decimal degrees.
     * @param height    The height above the ellipsoid in metres.
     * @return The value to add in order to get the height above the geoid (in metres).
     */
    public override fun heightOffset(longitude: Double, latitude: Double, height: Double): Double {
        /**
         * Note: no need to ensure that longitude is in [-180..+180°] range, because its value
         * is used only in trigonometric functions (sin / cos), which roll it as we would expect.
         * Latitude is used only in trigonometric functions as well.
         */
        val phi = Math.toRadians(latitude)
        val sinPhi = sin(phi)
        val sin2Phi = sinPhi * sinPhi
        val rni = sqrt(1.0 - esq * sin2Phi)
        val rn = semiMajor / rni
        val t22 = (rn + height) * cos(phi)
        val x2y2 = t22 * t22
        val z1 = (rn * (1 - esq) + height) * sinPhi
        val th = Math.PI / 2.0 - atan(z1 / sqrt(x2y2))
        val y = sin(th)
        val t = cos(th)
        val f1 = semiMajor / sqrt(x2y2 + z1 * z1)
        val f2 = f1 * f1
        val rlam = Math.toRadians(longitude)
        val gravn: Double = grava * (1.0 + star * sin2Phi) / rni
        sr[0] = 0.0
        sr[1] = sin(rlam)
        cr[0] = 1.0
        cr[1] = cos(rlam)
        for (j in 2..nmax) {
            sr[j] = 2.0 * cr[1] * sr[j - 1] - sr[j - 2]
            cr[j] = 2.0 * cr[1] * cr[j - 1] - cr[j - 2]
        }
        var sht = 0.0
        var previousSht = 0.0
        for (i in nmax downTo 0) {
            for (j in nmax downTo i) {
                val ll = locatingArray(j) + i
                val ll2 = ll + j + 1
                val ll3 = ll2 + j + 2
                val ta = aClenshaw[ll2] * f1 * t
                val tb = bClenshaw[ll3] * f2
                s11[j] = ta * s11[j + 1] - tb * s11[j + 2] + cnmGeopCoef[ll]
                s12[j] = ta * s12[j + 1] - tb * s12[j + 2] + snmGeopCoef[ll]
            }
            previousSht = sht
            sht = -`as`[i] * y * f1 * sht + s11[i] * cr[i] + s12[i] * sr[i]
        }
        return (s11[0] + s12[0]) * f1 + previousSht * SQRT_03 * y * f2 * rkm /
                (semiMajor * (gravn - height * 0.3086e-5))
    }

    companion object {
        // Pre-computed values of some square roots.
        private const val SQRT_03 = 1.7320508075688772
        private const val SQRT_05 = 2.23606797749979
        private const val SQRT_13 = 3.605551275463989
        private const val SQRT_17 = 4.123105625617661
        private const val SQRT_21 = 4.58257569495584

        // The default value for #nmax.
        private const val DEFAULT_ORDER = 180

        /**
         * Computes the index as it would be returned by the locating array iv
         *
         * Tip (used in some place in this class):
         * locatingArray(n+1) == locatingArray(n) + n + 1.
         */
        private fun locatingArray(n: Int): Int {
            return (n + 1) * n shr 1
        }
    }

}
