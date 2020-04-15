package hmi.parkinglot.render;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapOverlayType;

import java.util.List;

import hmi.parkinglot.Application;
import hmi.parkinglot.R;
import hmi.parkinglot.ngsi.Entity;

/**
 * Renders extra objects such as parking restrictions, gas stations or garages
 */
public class ExtraObjRenderer {
    public static void render(Context ctx, Map map, java.util.Map<String, List<Entity>> entities) {
        List<Entity> gasStations = entities.get(Application.GAS_STATION_TYPE);

        if (gasStations != null) {
            for (Entity gasStation : gasStations) {
                if (Application.renderedEntities.get(gasStation.id) != null) {
                    continue;
                }

                renderGasStation(ctx, map, gasStation);
            }
        }

        List<Entity> garages = entities.get(Application.GARAGE_TYPE);
        if (garages != null) {
            for (Entity garage : garages) {
                if (Application.renderedEntities.get(garage.id) != null) {
                    continue;
                }
                renderGarage(ctx, map, garage);
            }
        }

        List<Entity> parkingRestrictions = entities.get(Application.PARKING_RESTRICTION_TYPE);
        if (parkingRestrictions != null) {
            for (Entity parkingRestriction : parkingRestrictions) {
                if (Application.renderedEntities.get(parkingRestriction.id) != null) {
                    continue;
                }
                renderParkingRestriction(ctx, map, parkingRestriction);
            }
        }
    }

    public static void renderGasStation(Context ctx, Map map, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        RenderStyle style = new RenderStyle();
        style.textColor = Color.BLACK;
        style.textStyle = Typeface.NORMAL;

        MapMarker mapMarker = new MapMarker(coords,
                RenderUtilities.createLabeledIcon(ctx,
                        (String) ent.attributes.get("name"), style,
                        R.drawable.gas_station));

        mapMarker.setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);
        map.addMapObject(mapMarker);

        Application.mapObjects.add(mapMarker);

        Application.renderedEntities.put(ent.id, ent.id);
    }

    public static void renderGarage(Context ctx, Map map, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        RenderStyle style = new RenderStyle();
        style.textColor = Color.BLACK;
        style.textStyle = Typeface.NORMAL;

        MapMarker mapMarker = new MapMarker(coords,
                RenderUtilities.createLabeledIcon(ctx,
                        (String) ent.attributes.get("name"), style,
                        R.drawable.car_repair));

        mapMarker.setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);
        map.addMapObject(mapMarker);

        Application.mapObjects.add(mapMarker);

        Application.renderedEntities.put(ent.id, ent.id);
    }

    public static void renderParkingRestriction(Context ctx, Map map, Entity ent) {

    }
}
