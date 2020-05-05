package hmi.parkinglot.parking;

import android.content.Context;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapCircle;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.MapPolygon;

import java.io.IOException;
import java.util.List;

import hmi.parkinglot.Application;
import hmi.parkinglot.R;
import hmi.parkinglot.ngsi.Entity;
import hmi.parkinglot.render.RenderStyle;
import hmi.parkinglot.render.RenderUtilities;


/**
 * Renders parking entities
 */
public class ParkingRenderer {

    private static Image parkingIcon;

    static {
        parkingIcon = loadParkingIcon();
    }

    private static Image loadParkingIcon() {
        Image parkingIcon = new Image();

        try {
            parkingIcon.setImageResource(R.mipmap.parking);
        } catch (Exception e) {
            parkingIcon = null;
            e.printStackTrace();
        }

        return parkingIcon;
    }

    // Parking can contain as well a parking restriction
    public static void render(Context ctx, Map map, List<Entity> parkings) {
        for (Entity parking : parkings) {
            if (Application.renderedEntities.get(parking.id) != null) {
                continue;
            }

            if (parking.type.equals(Application.STREET_PARKING_TYPE)) {
                try {
                    renderStreetParking(ctx, map, parking);
                } catch (Throwable thr) {
                    Log.e(Application.TAG, "While rendering: " + parking.id);
                    thr.printStackTrace();
                }
            } else if (Application.PARKING_LOT_TYPE.equals(parking.type) ||
                    Application.PARKING_LOT_ZONE_TYPE.equals(parking.type)) {
                renderParkingLot(ctx, map, parking);
            } else if (parking.type.equals(Application.PARKING_RESTRICTION_TYPE)) {
                renderParkingRestriction(ctx, map, parking);
            }

            Application.renderedEntities.put(parking.id, parking.id);
        }
    }

    private static void renderStreetParking(Context ctx, Map map, Entity ent) {
        String available = ent.attributes.get(ParkingAttributes.AVAILABLE_SPOTS).toString();

        if (available.equals("0")) {
            return;
        }

        String total = ent.attributes.get(ParkingAttributes.TOTAL_SPOTS).toString();

        List<GeoPolygon> polygons = (List<GeoPolygon>) ent.attributes.get("polygon");
        for (int j = 0; j < polygons.size(); j++) {
            GeoPolygon polygon = polygons.get(j);

            GeoCoordinate coords = polygon.getBoundingBox().getCenter();

            MapPolygon streetPolygon = new MapPolygon(polygon);
            streetPolygon.setLineColor(Color.parseColor("#FF0000FF"));
            streetPolygon.setFillColor(Color.parseColor("#770000FF"));


            RenderStyle style = new RenderStyle();

            MapMarker mapMarker = new MapMarker(coords, RenderUtilities.createLabeledIcon(ctx,
                    available, style, R.mipmap.parking));
            mapMarker.setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);

            map.addMapObject(streetPolygon);
            map.addMapObject(mapMarker);

            Application.mapObjects.add(streetPolygon);
            Application.mapObjects.add(mapMarker);
        }
    }

    private static void renderParkingRestriction(Context ctx, Map map, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        GeoPolygon polygon = (GeoPolygon) ent.attributes.get("polygon");

        Log.d(Application.TAG, "Polygon:" + polygon.toString());

        for (int j = 0; j < polygon.getNumberOfPoints(); j++) {
            Log.d(Application.TAG, polygon.getPoint(j).getLatitude() + "," + polygon.getPoint(j).getLongitude());
        }

        try {
            MapPolygon streetPolygon = new MapPolygon(polygon);
            streetPolygon.setLineColor(Color.parseColor("#FF0000FF"));
            streetPolygon.setFillColor(Color.parseColor("#77FF0000"));
            map.addMapObject(streetPolygon);

            Application.mapObjects.add(streetPolygon);
        } catch (Throwable exc) {
            Log.e(Application.TAG, "Error while rendering parking restriction: " + exc);
            exc.printStackTrace();
        }

        Image sensorImg = new Image();
        try {
            sensorImg.setImageResource(R.drawable.park_restriction);
        } catch (IOException e) {
            Log.e(Application.TAG, "Cannot load image: " + e);
        }
        MapMarker marker = new MapMarker(coords, sensorImg);

        map.addMapObject(marker);
        Application.mapObjects.add(marker);
    }

    public static void render(Map map, Entity ent) {

    }

    private static void renderParkingLot(Context ctx, Map map, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        Integer nAvailable = (Integer) ent.attributes.get(ParkingAttributes.AVAILABLE_SPOTS);

        String available = "?";
        if (nAvailable != null) {
            available = nAvailable.toString();
        }

        String total = "?";
        Integer nTotal = (Integer) ent.attributes.get(ParkingAttributes.TOTAL_SPOTS);
        if (nTotal != null) {
            total = nTotal.toString();
        }

        String label = available + "/" + total;

        RenderStyle style = new RenderStyle();

        MapMarker mapMarker = new MapMarker(coords,
                RenderUtilities.createLabeledIcon(ctx,
                        label, style, R.mipmap.parking));
        mapMarker.setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);
        map.addMapObject(mapMarker);

        //Creating a default circle with 10 meters radius
        MapCircle circle = new MapCircle(10, coords);
        circle.setLineColor(Color.parseColor("#FF0000FF")); //(Color.GREEN);
        circle.setFillColor(Color.parseColor("#770000FF"));
        map.addMapObject(circle);

        Application.mapObjects.add(circle);
        Application.mapObjects.add(mapMarker);
    }

    public static void announceParkingMode(TextToSpeech tts) {
        tts.playEarcon("parking_mode", TextToSpeech.QUEUE_ADD, null, "ParkingMode");
        tts.speak("We are close to the destination. Parking mode is on",
                TextToSpeech.QUEUE_ADD, null, "ParkingMode");
    }

    public static void announceParking(TextToSpeech tts, String name) {
        tts.speak("Heading to parking: " + name, TextToSpeech.QUEUE_ADD, null, "ParkingFound");
    }
}
