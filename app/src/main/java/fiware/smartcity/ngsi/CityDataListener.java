package fiware.smartcity.ngsi;

import java.util.List;
import java.util.Map;

import fiware.smartcity.ngsi.Entity;

/**
 *  Interface for listeners when city data retrieval tasks finish
 *
 *
 */
public interface CityDataListener {
    public void onCityDataReady(Map<String, List<Entity>> data);
}
