/*
 *    Derived from:
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2001-2015, Open Source Geospatial Foundation (OSGeo)
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
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */

package com.wmstein.egm;

/**
 * Provides a default implementation for most methods required by the {MathTransform}
 * interface. {@code AbstractMathTransform} provides a convenient base class from which other
 * transform classes can be easily derived. In addition, {@code AbstractMathTransform} implements
 * methods required by the {MathTransform2D} interface, but <strong>does not</strong>
 * implements {@code MathTransform2D}. Subclasses must declare {@code implements MathTransform2D}
 * themself if they know to maps two-dimensional coordinate systems.
 *
 * @author Martin Desruisseaux (IRD)
 * @tutorial http://docs.codehaus.org/display/GEOTOOLS/Coordinate+Transformation+Parameters
 * @since 2.0
 * Code adaptation for use by MyPositionActivity by wm.stein
 */
public abstract class AbstractMathTransform 
{
    /**
     * Constructs a math transform.
     */
    protected AbstractMathTransform() 
	{
    }

    /**
     * Gets the dimension of input points.
     */
    public abstract int getSourceDimensions();

    /**
     * Gets the dimension of output points.
     */
    public abstract int getTargetDimensions();

    /**
     * Returns a hash value for this transform.
     */
    @Override
    public int hashCode() 
	{
        return getSourceDimensions() + 37 * getTargetDimensions();
    }

}

