package fiware.smartcity.ambient;

import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.mapping.MapPolygon;

/**
 *   Data about ambient area
 *
 */
public class AmbientAreaData {
    public String id;
    public GeoPolygon polygon;
    public MapPolygon view;
}
