package io.github.terra121.dataset.osm.config;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.github.terra121.dataset.geojson.Geometry;
import io.github.terra121.dataset.geojson.geometry.LineString;
import io.github.terra121.dataset.geojson.geometry.MultiLineString;
import io.github.terra121.dataset.geojson.geometry.MultiPoint;
import io.github.terra121.dataset.geojson.geometry.MultiPolygon;
import io.github.terra121.dataset.geojson.geometry.Point;
import io.github.terra121.dataset.geojson.geometry.Polygon;
import io.github.terra121.dataset.osm.Element;
import io.github.terra121.dataset.osm.config.mapper.LineMapper;
import io.github.terra121.dataset.osm.config.mapper.PolygonMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.lib.common.util.PorkUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static io.github.terra121.TerraConstants.*;

/**
 * Root of the OpenStreetMap configuration.
 *
 * @author DaPorkchop_
 */
@JsonAdapter(Root.Parser.class)
@Getter
@Builder
final class Root implements OSMMapper<Geometry> {
    @NonNull
    protected final LineMapper line;
    @NonNull
    protected final PolygonMapper polygon;

    @Override
    public Collection<Element> apply(String id, @NonNull Map<String, String> tags, @NonNull Geometry geometry) {
        if (geometry instanceof Point || geometry instanceof MultiPoint) { //points can't be generated
            return null;
        }

        //convert to multi type if not already
        if (geometry instanceof LineString) {
            geometry = new MultiLineString(new LineString[]{ (LineString) geometry });
        } else if (geometry instanceof Polygon) {
            geometry = new MultiPolygon(new Polygon[]{ (Polygon) geometry });
        }

        if (geometry instanceof MultiLineString) {
            return this.line.apply(id, tags, (MultiLineString) geometry);
        } else if (geometry instanceof MultiPolygon) {
            return this.polygon.apply(id, tags, (MultiPolygon) geometry);
        } else {
            throw new IllegalArgumentException("unsupported geometry type: " + PorkUtil.className(geometry));
        }
    }

    static final class Parser extends JsonParser<Root> {
        @Override
        public Root read(JsonReader in) throws IOException {
            RootBuilder builder = builder();

            in.beginObject();
            while (in.peek() != JsonToken.END_OBJECT) {
                String name = in.nextName();
                switch (name) {
                    case "line":
                        in.beginObject();
                        builder.line(GSON.fromJson(in, LineMapper.class));
                        in.endObject();
                        break;
                    case "polygon":
                        in.beginObject();
                        builder.polygon(GSON.fromJson(in, PolygonMapper.class));
                        in.endObject();
                        break;
                    default:
                        throw new IllegalStateException("invalid property: " + name);
                }
            }
            in.endObject();

            return builder.build();
        }
    }
}
