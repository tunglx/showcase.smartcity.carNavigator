package fiware.smartcity.parking;

import com.here.android.mpa.routing.Route;

/**
 * Created by jmcf on 12/11/15.
 */
public interface RouteCalculationListener {
    public void onRouteReady(Route r);
}
