package com.novetta.clavin.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/*#####################################################################
 * 
 * CLAVIN (Cartographic Location And Vicinity INdexer)
 * ---------------------------------------------------
 * 
 * Copyright (C) 2012-2013 Berico Technologies
 * http://clavin.bericotechnologies.com
 * 
 * ====================================================================
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * ====================================================================
 * 
 * LocationOccurrenceTest.java
 * 
 *###################################################################*/

/**
 * Basic tests for the {@link LocationOccurrence} class, which is a
 * container representing a named location entity found in a document.
 *
 */
public class LocationOccurrenceTest {

    /**
     * Ensures proper performance of the overridden equals() method.
     */
    @Test
    public void testEquals() {
        LocationOccurrence locationA = new LocationOccurrence("A", 0, 0);
        LocationOccurrence locationAdupe = new LocationOccurrence("A", 0, 0);
        LocationOccurrence locationB = new LocationOccurrence("B", 1, 1);
        LocationOccurrence locationB2 = new LocationOccurrence("B", 2, 2);
        LocationOccurrence locationNull = new LocationOccurrence(null, 0, 0);
        LocationOccurrence locationNulldupe = new LocationOccurrence(null, 0, 0);
        
        assertEquals("LocationOccurence == self", locationA, locationA);
        assertEquals("LocationOccurence == dupe", locationA, locationAdupe);
        assertNotEquals("LocationOccurence != null", locationA, null);
        assertNotEquals("LocationOccurence != different class object", locationA, Integer.valueOf(0));
        assertNotEquals("LocationOccurence != different position", locationB, locationB2);
        assertNotEquals("LocationOccurence != different name", locationA, locationB);
        
        assertNotEquals("Null name != LocationOccurence", locationNull, locationA);
        assertNotEquals("LocationOccurence != null name", locationA, locationNull);
        assertEquals("null name == null name", locationNull, locationNull);
        assertEquals("null name == null dupe", locationNull, locationNulldupe);
    }
    
    /**
     * Ensures proper performance of the overridden hashCode() method.
     */
    @Test
    public void testHashCode() {        
        LocationOccurrence locationNull = new LocationOccurrence(null, 0, 0);
        locationNull.hashCode();
        // if no exceptions are thrown, the above line is assumed to have succeeded
    }

}
