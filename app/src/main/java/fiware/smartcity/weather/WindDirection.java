package fiware.smartcity.weather;

import java.util.Map;
import java.util.HashMap;

/**
 * Translates wind direction
 */

public class WindDirection {
    public static Map<Integer, String> directions =  new HashMap<Integer, String>();

    public static String[] DESCRIPTIONS = new String[] {
        "N", "S", "E", "O","NE", "NO","SE","SO"
    };

    public static int[] VALUES = new int[] {
            180, 0, -90,  90, -45, 45, -135, 135
    };

    static {
        int index = 0;
        for(String description: DESCRIPTIONS) {
            directions.put(new Integer(VALUES[index]), description);

            index++;
        }
    };

    public static String get(int angle) {
        return directions.get(angle);
    };
}
