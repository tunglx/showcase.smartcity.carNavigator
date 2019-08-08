package hmi.parkinglot.ngsi;

import java.util.HashMap;
import java.util.Map;

/**
 *  Represents a Smart City entity provided by FIWARE
 *
 *
 */
public class Entity {
    public String id;
    public String type = "";
    public Map<String, Object> attributes = new HashMap<String, Object>();
    public double[] location;
}
