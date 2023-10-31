/*
 * Copyright (C) 2017 University of Pittsburgh.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.cmu.tetrad.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Sep 5, 2017 11:02:14 AM
 *
 * @param <T> annotation
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
public class AnnotatedClass<T extends Annotation> implements Serializable {

    private static final long serialVersionUID = 5060798016477163171L;

    private final Class clazz;

    private final T annotation;

    /**
     * Creates an annotated class.
     * @param clazz class
     * @param annotation annotation
     */
    public AnnotatedClass(Class clazz, T annotation) {
        this.clazz = clazz;
        this.annotation = annotation;
    }

    /**
     * Gets the class.
     * @return class
     */
    public Class getClazz() {
        return this.clazz;
    }

    /**
     * Gets the annotation.
     * @return annotation
     */
    public T getAnnotation() {
        return this.annotation;
    }

}
