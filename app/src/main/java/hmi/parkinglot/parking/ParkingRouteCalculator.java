package hmi.parkinglot.parking;

import android.os.AsyncTask;

import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.util.List;

/**
 * Created by jmcf on 12/11/15.
 */
public class ParkingRouteCalculator extends AsyncTask<ParkingRouteData, Integer, Integer> {
    private RouteCalculationListener listener;

    protected Integer doInBackground(ParkingRouteData... request) {
        // Initialize RouteManager
        CoreRouter routeManager = new CoreRouter();

        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        routePlan.addWaypoint(new RouteWaypoint(request[0].origin));
        routePlan.addWaypoint(new RouteWaypoint(request[0].parkingDestination));

        // Retrieve Routing information via RouteManagerListener
        routeManager.calculateRoute(routePlan, new CoreRouter.Listener() {
            @Override
            public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
                if (routingError == RoutingError.NONE &&
                        list.get(0).getRoute() != null) {
                    listener.onRouteReady(list.get(0).getRoute());
                }
            }

            @Override
            public void onProgress(int i) {
            }
        });
        return 0;
    }

    public void setListener(RouteCalculationListener list) {
        this.listener = list;
    }
}
