package charactercreator;

import charactercreator.PowerType.Option;
import java.util.HashMap;
import java.util.Map;

public class PowerData {

    public PowerType powerInfo;
    public int rank;
    public Map<Option, Integer> optionLevels;

    public PowerData(PowerType powerInfo) {
        this.powerInfo = powerInfo;
        optionLevels = new HashMap();
        powerInfo.options.forEach(o -> optionLevels.put(o, 0));
    }

    public String save() {
        return powerInfo.name + " | " + rank + optionLevels.entrySet().stream().filter(e -> e.getValue() != 0)
                .map(e -> " | " + e.getKey().name + "=" + e.getValue()).reduce("", String::concat);
    }

    @Override
    public String toString() {
        return "PowerData{" + "powerInfo=" + powerInfo.name + ", rank=" + rank + ", optionLevels=" + optionLevels + '}';
    }
}
