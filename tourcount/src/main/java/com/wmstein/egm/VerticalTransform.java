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
package com.wmstein.egm;

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
 * Code adaptation for use by TourCount by wm.stein on 2017-08-22
 * Last edited on 2020-04-17
 */
public abstract class VerticalTransform extends AbstractMathTransform
{
    /**
     * Creates a new instance of VerticalTransform.
     */
    VerticalTransform()
    {
    }

    /**
     * Gets the dimension of input points.
     */
    public final int getSourceDimensions()
    {
        return 3;
    }

    /**
     * Gets the dimension of output points.
     */
    public final int getTargetDimensions()
    {
        return 3;
    }

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
    protected abstract double heightOffset(double longitude, double latitude, double height)
        throws Exception;

}

