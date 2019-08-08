package hmi.parkinglot.parking;

import android.os.AsyncTask;

import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;

import java.util.List;

/**
 * Created by jmcf on 12/11/15.
 */
public class ParkingRouteCalculator extends AsyncTask<ParkingRouteData, Integer, Integer> {
    private RouteCalculationListener listener;

    protected  Integer doInBackground(ParkingRouteData... request) {
        // Initialize RouteManager
        RouteManager routeManager = new RouteManager();

        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        routePlan.addWaypoint(request[0].origin);
        routePlan.addWaypoint(request[0].parkingDestination);

        // Retrieve Routing information via RouteManagerListener
        RouteManager.Error error =
                routeManager.calculateRoute(routePlan, new RouteManager.Listener() {
                    @Override
                    public void onCalculateRouteFinished(RouteManager.Error errorCode,
                                                         List<RouteResult> result) {
                        if (errorCode == RouteManager.Error.NONE &&
                                result.get(0).getRoute() != null) {
                            listener.onRouteReady(result.get(0).getRoute());
                        }
                    }

                    public void onProgress(int progress) {

                    }
                });

        return new Integer(0);
    }

    public void setListener(RouteCalculationListener list) {
        this.listener = list;
    }
}
