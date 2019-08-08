package hmi.parkinglot.navigation;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.routing.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jmcf on 4/11/15.
 */
public class RouteData {
    public String originCity = "";
    public String origin = "";
    public String city = "";
    public String destination = "";
    public int parkingDistance = 0;
    public List<String> parkingCategory = new ArrayList<String>();
    public String vehicle = "";

    public GeoCoordinate originCoordinates;
    public GeoCoordinate destinationCoordinates;

    public GeoCoordinate parkingCoordinates;
    public String parkingAddress;

    public Route route;
}
