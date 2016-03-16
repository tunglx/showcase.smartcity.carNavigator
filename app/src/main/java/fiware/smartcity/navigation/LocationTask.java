package fiware.smartcity.navigation;

import android.os.AsyncTask;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.PositioningManager;

/**
 * Created by jmcf on 8/11/15.
 */
public class LocationTask extends AsyncTask<PositioningManager, Integer, GeoCoordinate> {
    private LocationListener listener;

    protected GeoCoordinate doInBackground(PositioningManager... pms) {
        PositioningManager posMan = pms[0];

        PositioningManager.LocationStatus status =
                posMan.getLocationStatus(PositioningManager.LocationMethod.GPS_NETWORK);
        try {
            while (status != PositioningManager.LocationStatus.AVAILABLE) {
                Thread.currentThread().sleep(500);
                status =
                        posMan.getLocationStatus(PositioningManager.LocationMethod.GPS_NETWORK);
            }
        }
        catch(InterruptedException ie) {

        }

        return posMan.getPosition().getCoordinate();
    }

    public void setListener(LocationListener listener) {
        this.listener = listener;
    }

    protected void onPostExecute(GeoCoordinate coord) {
        listener.onLocationReady(coord);
    }
}
