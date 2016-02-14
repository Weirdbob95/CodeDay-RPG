package codedayrpg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class Power {

    public String name;
    public int cost;
    public Target target;
    public List<String> desc;
    public List<Option> options;

    public Power(String name) {
        this.name = name;
        name = name.toLowerCase();

        try {
            List<String> lines = Files.readAllLines(Paths.get("dat/" + name + ".txt"));
            cost = Integer.parseInt(lines.get(0));
            target = Target.valueOf(lines.get(1));
            int i = 2;
            options = new LinkedList();
            for (; i < lines.size() && lines.get(i).startsWith("Option: "); i++) {
                options.add(new Option(lines.get(i).substring(8)));
            }
            desc = lines.subList(i + 1, lines.size());
        } catch (IOException ex) {
            throw new RuntimeException("Cannot find active power: " + name);
        }
    }

    @Override
    public String toString() {
        return "Power{" + "name=" + name + ", cost=" + cost + ", target=" + target + ", desc=" + desc + ", options=" + options + '}';
    }

    public static class Option {

        public String name;
        public int cost;
        public boolean perRank;
        public int max;
        public String desc;

        public Option(String data) {
            String[] dat = data.split(" \\| ");
            name = dat[0];
            cost = Integer.parseInt(dat[1]);
            perRank = Boolean.parseBoolean(dat[2]);
            max = Integer.parseInt(dat[3]);
            desc = dat[4];
        }

        @Override
        public String toString() {
            return "Option{" + "name=" + name + ", cost=" + cost + ", perRank=" + perRank + ", max=" + max + '}';
        }
    }

    public static enum Target {
        PERSONAL, REACH, RANGE, LOCATION;
    }
}
