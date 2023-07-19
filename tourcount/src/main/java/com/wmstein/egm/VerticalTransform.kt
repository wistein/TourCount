/*
 *    Derived from:
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
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
 */
package com.wmstein.egm

/*****************************************************************************
 * Base class for transformations from a height above the ellipsoid to a
 * height above the geoid. This transform expects three-dimensional geographic
 * coordinates in (longitude, latitude, height) order. The
 * transformations are usually backed by some ellipsoid-dependent database.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 * @since 2.3
 *
 * Code adaptation for use by TourCount by wm.stein on 2017-08-22,
 * last edited on 2020-04-17,
 * converted to Kotlin on 2023-07-05
 */
abstract class VerticalTransform
/**
 * Creates a new instance of VerticalTransform.
 */
internal constructor() : AbstractMathTransform() {
    /**
     * Gets the dimension of input points.
     */
    override val sourceDimensions: Int
        get() = 3

    /**
     * Gets the dimension of output points.
     */
    override val targetDimensions: Int
        get() = 3

    /**
     * Returns the value to add to a height above the ellipsoid in order to get a
     * height above the geoid for the specified geographic coordinate.
     *
     * @param longitude The geodetic longitude, in decimal degrees.
     * @param latitude  The geodetic latitude, in decimal degrees.
     * @param height    The height above the ellipsoid in metres.
     * @return The value to add in order to get the height above the geoid (in metres).
     * @throws Exception if the offset can't be computed for the specified coordinates.
     */
    @Throws(Exception::class)
    protected abstract fun heightOffset(longitude: Double, latitude: Double, height: Double): Double
}