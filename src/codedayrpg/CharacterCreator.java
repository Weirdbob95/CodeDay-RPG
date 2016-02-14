package codedayrpg;

import engine.Core;
import engine.Input;
import engine.Signal;
import graphics.Window2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import ui.*;
import util.Color4;
import static util.Color4.*;
import util.Vec2;

public class CharacterCreator {

    private static int maxRank = 10;
    private static Signal<Integer> points = new Signal(100);
    private static Map<String, Integer> attributeScores = new HashMap();
    private static List<String> activePowers = new LinkedList();

    public static void main(String[] args) {
        Core.init();
        Window2D.background = Color4.gray(.5);

        //Screen
        UIList leftBar = new UIList();
        leftBar.color = () -> Color4.gray(.9);
        //leftBar.padding = new Vec2(15);
        UIShowOne groups = new UIShowOne();
        groups.color = () -> WHITE;

        UIList screen = UIList.list(true, leftBar, groups);
        screen.setAllBorders(true);

        //Left Bar
        leftBar.add(new UIText(() -> "Points remaining: " + points.get()));
        leftBar.add(new UIElement(new Vec2(0, 15)));
        addLeftBarElement("Attributes", attrs(), leftBar, groups);
        addLeftBarElement("Power Descriptions", powerDescriptions(), leftBar, groups);
        addLeftBarElement("Active Powers", activePowers(), leftBar, groups);
        leftBar.setAllBorders(true);
        leftBar.setAllPadding(new Vec2(15));

        //Loop
        Signal<Boolean> clicked = Input.whenMouse(0, true).map(() -> true);
        Core.render.onEvent(() -> {
            screen.resize();
            screen.setUL(new Vec2(-600, 400));
            screen.update(clicked.get());
            screen.resize();
            screen.setUL(new Vec2(-600, 400));
            screen.draw();
            clicked.set(false);
        });

        Core.run();
    }

    private static UIElement activePowers() {
        UIList leftArea = new UIList();
        UIShowOne rightArea = new UIShowOne();

        UIText newButton = new UIText(() -> "New Power");
        newButton.border = true;
        newButton.padding = new Vec2(10);

        leftArea.add(new UIElement(new Vec2(0, 20)), newButton);

        return UIList.list(true, leftArea, rightArea);
    }

    private static void addLeftBarElement(String name, UIElement toShow, UIList leftBar, UIShowOne groups) {
        groups.add(toShow);
        UIText powerDescriptionsButton = new UIText(() -> name);
        leftBar.add(powerDescriptionsButton);
        powerDescriptionsButton.onClick.onEvent(() -> groups.showing = toShow);
    }

    private static UIElement attrs() {
        UIList attrs = UIList.list(false, new UIText(() -> "Spend points to improve your attributes"));
        Arrays.asList("Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma").forEach(a -> {
            attributeScores.put(a, 0);

            UIImage plus = new UIImage("plus");
            Supplier<Boolean> plusGood = () -> attributeScores.get(a) < maxRank && attributeScores.get(a) + 1 <= points.get();
            plus.mouseOver.onEvent(() -> plus.sprite.color = plusGood.get() ? GREEN : RED);
            plus.onClick.filter(plusGood).onEvent(() -> points.edit(x -> x - attributeScores.compute(a, (k, v) -> v + 1)));

            UIImage minus = new UIImage("minus");
            Supplier<Boolean> minusGood = () -> attributeScores.get(a) > 0;
            minus.mouseOver.onEvent(() -> minus.sprite.color = minusGood.get() ? GREEN : RED);
            minus.onClick.filter(minusGood).onEvent(() -> points.edit(x -> x + attributeScores.compute(a, (k, v) -> v - 1) + 1));

            attrs.add(UIList.list(true,
                    new UIText(() -> a + ": " + attributeScores.get(a)),
                    new UIElement(new Vec2(15, 0)),
                    plus,
                    new UIElement(new Vec2(15, 0)),
                    minus));
        });
        attrs.setAllPadding(new Vec2(10));
        attrs.padding = new Vec2(5);
        return attrs;
    }

    private static UIElement powerDescriptions() {
        UIShowOne currentPower = new UIShowOne();
        UIList powerList = new UIList();
        try {
            for (String name : Files.readAllLines(Paths.get("dat/active_powers.txt"))) {
                UIList desc = new UIList();
                desc.add(new UIText(() -> name));
                desc.add(new UIElement(new Vec2(0, 10)));
                if (name.equals("Damage")) {
                    for (String line : Files.readAllLines(Paths.get("dat/" + name.toLowerCase() + ".txt"))) {
                        if (line.length() == 1) {
                            desc.add(new UIText(() -> "Cost: " + line));
                            desc.add(new UIElement(new Vec2(0, 10)));
                        } else {
                            desc.add(new UIText(() -> line));
                        }
                    }
                }
                currentPower.add(desc);

                UIText inList = new UIText(() -> name);
                inList.onClick.onEvent(() -> currentPower.showing = desc);
                powerList.add(inList);
            }
        } catch (IOException ex) {
        }

        UIList powers = UIList.list(true,
                UIList.list(false,
                        new UIText(() -> "Choose an Active Power"),
                        new UIElement(new Vec2(0, 15)),
                        powerList),
                currentPower);
        powers.setAllBorders(true);
        powers.setAllPadding(new Vec2(15));
        return powers;
    }
}
