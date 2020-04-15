package hmi.parkinglot.ambient;

import com.here.android.mpa.mapping.MapPolygon;

/**
 * Render listener for AmbientArea
 */
public interface AmbientAreaRenderListener {
    public void onRendered(String level, MapPolygon polygon);
}
