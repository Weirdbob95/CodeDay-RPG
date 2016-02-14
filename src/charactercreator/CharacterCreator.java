package charactercreator;

import engine.Core;
import engine.Input;
import engine.Signal;
import graphics.Window2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import org.lwjgl.input.Keyboard;
import static ui.UIElement.space;
import static ui.UIList.list;
import static ui.UIText.text;
import ui.*;
import util.Color4;
import static util.Color4.TRANSPARENT;
import static util.Color4.WHITE;
import util.Mutable;
import util.Vec2;

public class CharacterCreator {

    private static List<String> ACTIVE_POWERS;

    static {
        try {
            ACTIVE_POWERS = Files.readAllLines(Paths.get("dat/active_powers.txt"));
        } catch (IOException ex) {
        }
    }

    private static int maxRank = 10;
    private static int maxPoints = 30;
    private static Signal<Integer> points = new Signal(100);
    private static PlayerData player = new PlayerData();

    public static void main(String[] args) {
        Core.init();
        Window2D.background = Color4.gray(.5);

        //Screen
        UIList leftBar = new UIList();
        leftBar.color = () -> Color4.gray(.9);
        leftBar.gravity = .5;
        UIShowOne groups = new UIShowOne();
        groups.color = () -> WHITE;

        UIList screen = list(true, leftBar, groups);
        screen.setAllBorders(true);

        //Left Bar
        leftBar.add(new UIText(() -> "Points remaining: " + points.get()));
        leftBar.add(space(15));
        addToSelectbar(() -> "Attributes", attributesGroup(), leftBar, groups);
        addToSelectbar(() -> "Power Descriptions", powerDescriptions(), leftBar, groups);
        addToSelectbar(() -> "Active Powers", activePowersGroup(), leftBar, groups);
        //leftBar.setAllBorders(true);
        leftBar.setAllPadding(new Vec2(10));
        leftBar.padding = new Vec2(5);

        //Loop
        Signal<Boolean> clicked = Input.whenMouse(0, true).combineEventStreams(Input.whileMouseDown(1).limit(.05)).map(() -> true);
        Core.render.onEvent(() -> {
            screen.resize();
            screen.setUL(new Vec2(-600, 400));
            screen.update(clicked.get());
            screen.resize();
            screen.setUL(new Vec2(-600, 400));
            screen.draw();
            clicked.set(false);
        });

        Input.whenKey(Keyboard.KEY_S, true).onEvent(() -> player.saveTo("char.txt"));
        Input.whenKey(Keyboard.KEY_L, true).onEvent(() -> player.loadFrom("char.txt"));
        Input.whenKey(Keyboard.KEY_P, true).onEvent(() -> System.out.println(player));

        Core.run();
    }

    private static UIElement activePowerBuy(UIList powerList, UIShowOne rightArea) {
        UIList powerView = new UIList();
        Mutable<UIText> inList = new Mutable(null);
        Mutable<PowerData> pd = new Mutable(null);

        //Header
        UISelector powerType = new UISelector("Choose an Active Power", ACTIVE_POWERS, () -> inList.o == null);
        UIValue rank = new UIValue("Rank", x -> x < maxRank && inList.o == null, x -> x > 0 && inList.o == null);
        rank.value.filter(x -> pd.o != null).forEach(x -> pd.o.rank = x);
        Mutable<Supplier<String>> name = new Mutable<>(() -> "Unfinished Power");
        powerType.chosen.forEach(s -> name.o = () -> s + " " + rank.value.get());

        UIList header = list(true, powerType, rank);
        header.setAllPadding(new Vec2(15));
        header.gravity = .5;

        //Body
        UIShowOne body = new UIShowOne();
        powerType.chosen.forEach(s -> {
            PowerType p = new PowerType(s);
            pd.o = new PowerData(p);
            pd.o.rank = rank.value.get();

            //Cost
            List<Supplier<Integer>> costFlat = new LinkedList();
            List<Supplier<Integer>> costPerRank = new LinkedList();
            costPerRank.add(() -> p.cost);
            Supplier<Integer> costTotal = () -> Math.max(1, costFlat.stream().mapToInt(Supplier::get).sum()
                    + Math.max(1, costPerRank.stream().mapToInt(Supplier::get).sum()) * rank.value.get());

            //Options
            UIList options = new UIList();
            p.options.forEach(o -> {
                UIElement buyer;
                if (o.max != 1) {
                    UIValue amt = new UIValue("Amt", x -> (x < o.max || o.max == 0) && inList.o == null, x -> x > 0 && inList.o == null);
                    amt.value.forEach(x -> pd.o.optionLevels.put(o, x));
                    (o.perRank ? costPerRank : costFlat).add(() -> o.cost * amt.value.get());
                    buyer = amt;
                } else {
                    UICheckbox check = new UICheckbox(() -> inList.o == null);
                    check.on.forEach(b -> pd.o.optionLevels.put(o, b ? 1 : 0));
                    (o.perRank ? costPerRank : costFlat).add(() -> o.cost * (check.on.get() ? 1 : 0));
                    buyer = check;
                }

                UIList option = list(true,
                        text(o.name + ": " + o.desc, "Small"),
                        buyer);
                option.gravity = .5;
                option.setAllPadding(new Vec2(6));
                options.add(option);
            });
            options.border = options.listBorders = true;

            //Bottom bar
            UIButton buy = new UIButton(() -> "Buy", () -> costTotal.get() <= points.get() && costTotal.get() <= maxPoints && inList.o == null && rank.value.get() > 0);
            buy.onPress.onEvent(() -> {
                inList.o = addToSelectbar(() -> name.o.get(), powerView, powerList, rightArea);
                inList.o.onClick.sendEvent();
                player.powerData.add(pd.o);
                points.edit(x -> x - costTotal.get());
            });
            UIButton edit = new UIButton(() -> "Edit", () -> inList.o != null);
            edit.onPress.onEvent(() -> {
                powerList.parts.remove(inList.o);
                inList.o = null;
                player.powerData.remove(pd.o);
                points.edit(x -> x + costTotal.get());
            });
            UIButton delete = new UIButton(() -> "Delete", () -> inList.o == null);
            delete.onPress.onEvent(() -> {
                rightArea.parts.remove(powerView);
                rightArea.showing = new UIElement();
            });
            UIList bottomBar = list(true,
                    new UIText(() -> "Cost: " + costTotal.get()),
                    space(150, 0),
                    buy,
                    space(150, 0),
                    edit,
                    space(150, 0),
                    delete);
            bottomBar.gravity = .5;

            //Whole body
            UIList bodyArea = list(false,
                    text("Options:"),
                    space(10),
                    options,
                    space(20),
                    bottomBar);
            bodyArea.padding = new Vec2(15);
            body.add(bodyArea);
            body.showing = bodyArea;
        });

        //Whole
        powerView.add(header, body);
        powerView.listBorders = true;
        rightArea.parts.add(powerView);
        return powerView;
    }

    private static UIElement activePowersGroup() {
        UIList powerList = list(false, space(20));
        powerList.padding = new Vec2(15);
        UIShowOne rightArea = new UIShowOne();

        UIText newButton = text("New Power");
        newButton.padding = new Vec2(10);

        newButton.onClick.onEvent(() -> {
            rightArea.showing = activePowerBuy(powerList, rightArea);
            powerList.setAllColors(() -> TRANSPARENT);
        });

        UIList leftArea = list(false, powerList, newButton);
        leftArea.listBorders = true;
        leftArea.gravity = .5;
        UIList whole = list(true, leftArea, rightArea);
        whole.gravity = 1;
        whole.listBorders = true;
        return whole;
    }

    private static UIText addToSelectbar(Supplier<String> name, UIElement toShow, UIList leftBar, UIShowOne groups) {
        groups.add(toShow);
        UIText button = new UIText(name);
        leftBar.add(button);
        button.onClick.onEvent(() -> {
            groups.showing = toShow;
            leftBar.setAllColors(() -> TRANSPARENT);
            button.color = () -> new Color4(.4, .7, 1);
        });
        return button;
    }

    private static UIElement attributesGroup() {
        UIList attrs = list(false, text("Spend points to improve your attributes"));
        Arrays.asList("Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma").forEach(a -> {
            player.attributeScores.put(a, 0);

            UIValue v = new UIValue(a, x -> x < maxRank && x + 1 <= points.get(), x -> x > 0);
            v.value.forEach(x -> player.attributeScores.put(a, x));
            v.onPlus.forEach(x -> points.edit(p -> p - x));
            v.onMinus.forEach(x -> points.edit(p -> p + x + 1));

            attrs.add(v);
        });
        attrs.setAllPadding(new Vec2(10));
        attrs.padding = new Vec2(5);
        return attrs;
    }

    private static UIElement powerDescriptions() {
        UIShowOne currentPower = new UIShowOne();
        UIList powerList = new UIList();
        ACTIVE_POWERS.forEach(name -> {
            PowerType p = new PowerType(name);
            UIList desc = list(false,
                    text(name),
                    space(20),
                    text("Target: " + p.target, "Medium"),
                    text("Cost: " + p.cost + " point" + (p.cost != 1 ? "s" : "") + " per rank", "Medium"),
                    space(20));
            p.desc.forEach(s -> desc.add(text(s, "Small")));
            if (p.options.size() > 0) {
                desc.add(space(20), text("The " + name + " power has the following options:", "Medium"), space(10));
            }
            p.options.forEach(o -> desc.add(text(o.name + ": " + o.desc, "Small")));

            addToSelectbar(() -> name, desc, powerList, currentPower);
        });

        UIList powers = list(true,
                list(false,
                        text("Choose an Active Power"),
                        space(15),
                        powerList),
                currentPower);
        powers.listBorders = true;
        powers.setAllPadding(new Vec2(15));
        return powers;
    }
}
