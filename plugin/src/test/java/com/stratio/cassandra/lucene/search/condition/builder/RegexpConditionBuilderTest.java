/*
 * Copyright 2015, Stratio.
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

package com.stratio.cassandra.lucene.search.condition.builder;

import com.stratio.cassandra.lucene.search.condition.Condition;
import com.stratio.cassandra.lucene.search.condition.RegexpCondition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Class for testing {@link RegexpConditionBuilder}.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class RegexpConditionBuilderTest extends AbstractConditionBuilderTest {

    @Test
    public void testBuild() {
        RegexpConditionBuilder builder = new RegexpConditionBuilder("field", "value").boost(0.7f);
        RegexpCondition condition = builder.build();
        assertNotNull(condition);
        assertEquals(0.7f, condition.boost, 0);
        assertEquals("field", condition.field);
        assertEquals("value", condition.value);
    }

    @Test
    public void testBuildDefaults() {
        RegexpConditionBuilder builder = new RegexpConditionBuilder("field", "value");
        RegexpCondition condition = builder.build();
        assertNotNull(condition);
        assertEquals(Condition.DEFAULT_BOOST, condition.boost, 0);
        assertEquals("field", condition.field);
        assertEquals("value", condition.value);
    }

    @Test
    public void testJsonSerialization() {
        RegexpConditionBuilder builder = new RegexpConditionBuilder("field", "value").boost(0.7f);
        testJsonSerialization(builder, "{type:\"regexp\",field:\"field\",value:\"value\",boost:0.7}");
    }

    @Test
    public void testJsonSerializationDefaults() {
        RegexpConditionBuilder builder = new RegexpConditionBuilder("field", "value");
        testJsonSerialization(builder, "{type:\"regexp\",field:\"field\",value:\"value\"}");
    }
}
