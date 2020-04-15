package hmi.parkinglot.ngsi;

import java.util.List;
import java.util.Map;

/**
 * Interface for listeners when city data retrieval tasks finish
 */
public interface CityDataListener {
    public void onCityDataReady(Map<String, List<Entity>> data);
}
