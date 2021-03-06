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

package com.stratio.cassandra.lucene.schema.mapping;

import com.google.common.base.Objects;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Point;
import com.stratio.cassandra.lucene.IndexException;
import com.stratio.cassandra.lucene.schema.column.Column;
import com.stratio.cassandra.lucene.schema.column.Columns;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.AsciiType;
import org.apache.cassandra.db.marshal.DecimalType;
import org.apache.cassandra.db.marshal.DoubleType;
import org.apache.cassandra.db.marshal.FloatType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.IntegerType;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.bbox.BBoxStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;

import java.util.Arrays;

/**
 * A {@link Mapper} to map geographical points.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class GeoPointMapper extends Mapper {

    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    public static final SpatialContext SPATIAL_CONTEXT = SpatialContext.GEO;
    public static final int DEFAULT_MAX_LEVELS = 11;

    private final String latitude;
    private final String longitude;
    private final int maxLevels;

    private final SpatialStrategy distanceStrategy;
    private final SpatialStrategy bboxStrategy;

    /**
     * Builds a new {@link GeoPointMapper}.
     *
     * @param name      The name of the mapper.
     * @param latitude  The name of the column containing the latitude.
     * @param longitude The name of the column containing the longitude.
     * @param maxLevels The maximum number of levels in the tree.
     */
    public GeoPointMapper(String name, String latitude, String longitude, Integer maxLevels) {
        super(name,
              true,
              false,
              Arrays.<AbstractType<?>>asList(AsciiType.instance,
                                             UTF8Type.instance,
                                             Int32Type.instance,
                                             LongType.instance,
                                             IntegerType.instance,
                                             FloatType.instance,
                                             DoubleType.instance,
                                             DecimalType.instance),
              Arrays.asList(latitude, longitude));

        if (StringUtils.isBlank(latitude)) {
            throw new IndexException("latitude column name is required");
        }

        if (StringUtils.isBlank(longitude)) {
            throw new IndexException("longitude column name is required");
        }

        this.latitude = latitude;
        this.longitude = longitude;
        this.maxLevels = maxLevels == null ? DEFAULT_MAX_LEVELS : maxLevels;
        SpatialPrefixTree grid = new GeohashPrefixTree(SPATIAL_CONTEXT, this.maxLevels);
        distanceStrategy = new RecursivePrefixTreeStrategy(grid, name + ".dist");
        bboxStrategy = new BBoxStrategy(SPATIAL_CONTEXT, name + ".bbox");
    }

    /**
     * Checks if the specified latitude is correct.
     *
     * @param name     The name of the latitude field.
     * @param latitude The value of the latitude field.
     * @return The latitude.
     */
    public static Double checkLatitude(String name, Double latitude) {
        if (latitude == null) {
            throw new IndexException("%s required", name);
        } else if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IndexException("%s must be in range [%s, %s], but found '%s'",
                                     name,
                                     MIN_LATITUDE,
                                     MAX_LATITUDE,
                                     latitude);
        }
        return latitude;
    }

    /**
     * Checks if the specified longitude is correct.
     *
     * @param name      The name of the longitude field.
     * @param longitude The value of the longitude field.
     * @return The longitude.
     */
    public static Double checkLongitude(String name, Double longitude) {
        if (longitude == null) {
            throw new IndexException("%s required", name);
        } else if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IndexException("%s must be in range [%s, %s], but found %s",
                                     name,
                                     MIN_LONGITUDE,
                                     MAX_LONGITUDE,
                                     longitude);
        }
        return longitude;
    }

    /**
     * Returns the name of the column containing the latitude.
     *
     * @return The name of the column containing the latitude.
     */
    public String getLatitude() {
        return latitude;
    }

    /**
     * Returns the name of the column containing the longitude.
     *
     * @return The name of the column containing the longitude.
     */
    public String getLongitude() {
        return longitude;
    }

    /**
     * Returns the used {@link SpatialStrategy} for distances.
     *
     * @return The used {@link SpatialStrategy} for distances.
     */
    public SpatialStrategy getDistanceStrategy() {
        return distanceStrategy;
    }

    /**
     * Returns the used {@link SpatialStrategy} for bounding boxes.
     *
     * @return The used {@link SpatialStrategy} for bounding boxes.
     */
    public SpatialStrategy getBBoxStrategy() {
        return bboxStrategy;
    }

    /**
     * Returns the maximum number of levels in the tree.
     *
     * @return The maximum number of levels in the tree.
     */
    public int getMaxLevels() {
        return maxLevels;
    }

    /** {@inheritDoc} */
    @Override
    public String getAnalyzer() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addFields(Document document, Columns columns) {

        Double longitude = readLongitude(columns);
        Double latitude = readLatitude(columns);

        if (longitude == null && latitude == null) {
            return;
        } else if (latitude == null) {
            throw new IndexException("Latitude column required if there is a longitude");
        } else if (longitude == null) {
            throw new IndexException("Longitude column required if there is a latitude");
        }

        Point point = SPATIAL_CONTEXT.makePoint(longitude, latitude);

        for (IndexableField field : distanceStrategy.createIndexableFields(point)) {
            document.add(field);
        }
        for (IndexableField field : bboxStrategy.createIndexableFields(point)) {
            document.add(field);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SortField sortField(String name, boolean reverse) {
        throw new IndexException("GeoPoint mapper '%s' does not support sorting", name);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(CFMetaData metadata) {
        validate(metadata, latitude);
        validate(metadata, longitude);
    }

    /**
     * Returns the latitude contained in the specified {@link Columns}. A valid latitude must in the range [-90, 90].
     *
     * @param columns The {@link Columns} containing the latitude.
     */
    Double readLatitude(Columns columns) {
        Column<?> column = columns.getColumnsByName(latitude).getFirst();
        return column == null ? null : readLatitude(column.getComposedValue());
    }

    /**
     * Returns the longitude contained in the specified {@link Columns}. A valid longitude must in the range [-180,
     * 180].
     *
     * @param columns The {@link Columns} containing the latitude.
     */
    Double readLongitude(Columns columns) {
        Column<?> column = columns.getColumnsByName(longitude).getFirst();
        return column == null ? null : readLongitude(column.getComposedValue());
    }

    /**
     * Returns the latitude contained in the specified {@link Object}.
     *
     * A valid latitude must in the range [-90, 90].
     *
     * @param o The {@link Object} containing the latitude.
     * @return The latitude.
     */
    private double readLatitude(Object o) {
        Double value;
        if (o instanceof Number) {
            value = ((Number) o).doubleValue();
        } else {
            try {
                value = Double.valueOf(o.toString());
            } catch (NumberFormatException e) {
                throw new IndexException("Unparseable latitude: %s", o);
            }
        }
        return checkLatitude("latitude", value);
    }

    /**
     * Returns the longitude contained in the specified {@link Object}.
     *
     * A valid longitude must in the range [-180, 180].
     *
     * @param o The {@link Object} containing the latitude.
     * @return The longitude.
     */
    private static double readLongitude(Object o) {
        Double value;
        if (o instanceof Number) {
            value = ((Number) o).doubleValue();
        } else {
            try {
                value = Double.valueOf(o.toString());
            } catch (NumberFormatException e) {
                throw new IndexException("Unparseable longitude: %s", o);
            }
        }
        return checkLongitude("longitude", value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                      .add("name", name)
                      .add("latitude", latitude)
                      .add("longitude", longitude)
                      .add("maxLevels", maxLevels)
                      .toString();
    }
}
