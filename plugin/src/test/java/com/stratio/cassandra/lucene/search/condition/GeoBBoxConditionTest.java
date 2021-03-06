/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.cassandra.lucene.search.condition;

import com.stratio.cassandra.lucene.IndexException;
import com.stratio.cassandra.lucene.schema.Schema;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import static com.stratio.cassandra.lucene.schema.SchemaBuilders.*;
import static com.stratio.cassandra.lucene.search.SearchBuilders.geoBBox;
import static org.junit.Assert.*;

/**
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class GeoBBoxConditionTest {

    @Test
    public void testConstructor() {
        GeoBBoxCondition condition = new GeoBBoxCondition(0.5f, "name", -90D, 90D, -180D, 180D);
        assertEquals(0.5, condition.boost, 0);
        assertEquals("name", condition.field);
        assertEquals(-180, condition.minLongitude, 0);
        assertEquals(180, condition.maxLongitude, 0);
        assertEquals(-90, condition.minLatitude, 0);
        assertEquals(90, condition.maxLatitude, 0);
    }

    @Test
    public void testConstructorWithDefaults() {
        GeoBBoxCondition condition = new GeoBBoxCondition(null, "name", 2D, 3D, 0D, 1D);
        assertEquals(GeoBBoxCondition.DEFAULT_BOOST, condition.boost, 0);
        assertEquals("name", condition.field);
        assertEquals(0, condition.minLongitude, 0);
        assertEquals(1, condition.maxLongitude, 0);
        assertEquals(2, condition.minLatitude, 0);
        assertEquals(3, condition.maxLatitude, 0);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithNullField() {
        new GeoBBoxCondition(null, null, 2D, 3D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithEmptyField() {
        new GeoBBoxCondition(null, "", 2D, 3D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithBlankField() {
        new GeoBBoxCondition(null, " ", 2D, 3D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithNullMinLongitude() {
        new GeoBBoxCondition(null, "name", 2D, 3D, null, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithToSmallMinLongitude() {
        new GeoBBoxCondition(null, "name", 2D, 3D, -181D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithToBiglMinLongitude() {
        new GeoBBoxCondition(null, "name", 2D, 3D, 181D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithNullMaxLongitude() {
        new GeoBBoxCondition(null, "name", 2D, 3D, 0D, null);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithTooSmallMaxLongitude() {
        new GeoBBoxCondition(null, "name", 2D, 3D, 0D, -181D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithTooBigMaxLongitude() {
        new GeoBBoxCondition(null, "name", 2D, 3D, 0D, 181D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithNullLatitude() {
        new GeoBBoxCondition(null, "name", null, 3D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithTooSmallMinLatitude() {
        new GeoBBoxCondition(null, "name", -91D, 3D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithTooBigMinLatitude() {
        new GeoBBoxCondition(null, "name", 91D, 3D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithNullMaxLatitude() {
        new GeoBBoxCondition(null, "name", 2D, null, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithTooSmallMaxLatitude() {
        new GeoBBoxCondition(null, "name", 2D, -91D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithTooBigMaxLatitude() {
        new GeoBBoxCondition(null, "name", 2D, 91D, 0D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithMinLongitudeGreaterThanMaxLongitude() {
        new GeoBBoxCondition(null, "name", 3D, 3D, 2D, 1D);
    }

    @Test(expected = IndexException.class)
    public void testConstructorWithMinLatitudeGreaterThanMaxLatitude() {
        new GeoBBoxCondition(null, "name", 4D, 3D, 0D, 1D);
    }

    @Test
    public void testQuery() {
        Schema schema = schema().mapper("name", geoPointMapper("lat", "lon").maxLevels(8)).build();
        GeoBBoxCondition condition = new GeoBBoxCondition(0.5f, "name", -90D, 90D, -180D, 180D);
        Query query = condition.query(schema);
        assertNotNull(query);
        assertTrue(query instanceof ConstantScoreQuery);
        query = ((ConstantScoreQuery) query).getQuery();
        assertTrue(query instanceof BooleanQuery);
    }

    @Test(expected = IndexException.class)
    public void testQueryWithoutValidMapper() {
        Schema schema = schema().mapper("name", UUIDMapper()).build();
        GeoBBoxCondition condition = new GeoBBoxCondition(0.5f, "name", -90D, 90D, -180D, 180D);
        condition.query(schema);
    }

    @Test
    public void testToString() {
        GeoBBoxCondition condition = geoBBox("name", -180D, 180D, -90D, 90D).boost(0.5f).build();
        assertEquals("GeoBBoxCondition{boost=0.5, field=name, " +
                     "minLatitude=-90.0, maxLatitude=90.0, minLongitude=-180.0, maxLongitude=180.0}",
                     condition.toString());
    }

}
