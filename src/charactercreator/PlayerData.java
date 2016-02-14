package charactercreator;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ui.UIElement;
import static ui.UIElement.space;
import ui.UIList;
import static ui.UIText.text;
import util.Vec2;

public class PlayerData {

    public Map<String, Integer> attributeScores = new HashMap();
    public List<PowerData> powerData = new LinkedList();
    public List<String> data;

    public void loadData(List<String> lines) {
        data = lines;
        for (int i = 0; i < 6; i++) {
            String s = lines.get(i);
            attributeScores.put(s.substring(0, s.indexOf("=")), Integer.parseInt(s.substring(s.indexOf("=") + 1)));
        }
        for (String s : lines.subList(6, lines.size())) {
            String[] parts = s.split(" \\| ");
            PowerData pd = new PowerData(new PowerType(parts[0]));
            pd.rank = Integer.parseInt(parts[1]);
            for (int i = 2; i < parts.length; i++) {
                String[] parts2 = parts[i].split("=");
                pd.optionLevels.put(pd.powerInfo.options.stream()
                        .filter(o -> o.name.equals(parts2[0])).findAny().get(), Integer.parseInt(parts2[1]));
            }
            powerData.add(pd);
        }
    }

    public void loadFrom(String file) {
        try {
            loadData(Files.readAllLines(Paths.get(file)));
        } catch (Exception ex) {
        }
    }

    public void saveTo(String file) {
        try {
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            attributeScores.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).forEach(writer::println);
            powerData.stream().map(PowerData::save).forEach(writer::println);
            writer.close();
        } catch (Exception ex) {
        }
    }

    @Override
    public String toString() {
        return "Player{" + "attributeScores=" + attributeScores + ", powerData=" + powerData + '}';
    }

    public UIElement toUI() {
        UIList all = new UIList();
        attributeScores.forEach((s, x) -> all.add(text(s + ": " + x, "Medium")));
        all.add(space(25));
        powerData.forEach(pd -> {
            all.add(text(pd.powerInfo.name));
            pd.powerInfo.desc.forEach(s -> all.add(text(s, "Small")));
            pd.optionLevels.forEach((o, x) -> {
                if (x == 1) {
                    all.add(text(o.name + ": " + o.desc, "Small"));
                }
                if (x > 1) {
                    all.add(text(o.name + " " + x + ": " + o.desc, "Small"));
                }
                all.add(space(10));
            });
            all.add(space(10));
        });
        all.padding = new Vec2(15);
        return all;
    }
}
