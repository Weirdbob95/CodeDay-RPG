package game;

import charactercreator.CharacterCreator;
import charactercreator.PlayerData;
import engine.Core;
import engine.Input;
import engine.Signal;
import examples.Premade2D;
import static game.PingArrow.drawArrow;
import graphics.Window2D;
import java.util.Arrays;
import network.Connection;
import network.NetworkUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import ui.UIElement;
import static ui.UIElement.space;
import ui.UIShowOne;
import util.Color4;
import util.RegisteredEntity;
import static util.ThreadManager.onMainThread;
import util.Vec2;

public class Client {

    private static Connection conn;

    public static void main(String[] args) {
        if (args.length == 0) {
            conn = NetworkUtils.connectManual();
        } else {
            if (args[0].equals("server")) {
                Server.main(args);
                return;
            } else if (args[0].equals("char")) {
                CharacterCreator.main(args);
                return;
            }
            conn = NetworkUtils.connect(args[0]);
        }
        registerMessageHandlers();

        Core.init();
        Core.render.bufferCount(Core.interval(1)).forEach(i -> Display.setTitle("FPS: " + i));

        Tile.init();

        Input.whenMouse(0, true).onEvent(() -> conn.sendMessage(0, Input.getMouse(), clientColor));
        Signal<Vec2> down = Input.whenMouse(1, true).map(Input::getMouse);
        Input.whenMouse(1, false).onEvent(() -> conn.sendMessage(1, down.get(), Input.getMouse(), clientColor));

        Core.render.filter(Input.mouseSignal(1)).onEvent(() -> drawArrow(down.get(), Input.getMouse(), clientColor.withA(.5)));

        //Loop
        UIShowOne screen = new UIShowOne();
        Signal<Boolean> clicked = Input.whenMouse(0, true).combineEventStreams(Input.whileMouseDown(1).limit(.05)).map(() -> true);
        Core.renderLayer(1).onEvent(() -> {
            screen.resize();
            screen.setUL(new Vec2(-600, 400).add(Window2D.viewPos));
            screen.update(clicked.get());
            screen.resize();
            screen.setUL(new Vec2(-600, 400).add(Window2D.viewPos));
            screen.draw();
            clicked.set(false);
        });
        screen.border = true;
        screen.color = () -> Color4.gray(.9);
        PlayerData pd = new PlayerData();
        pd.loadFrom("char.txt");
        UIElement player = pd.toUI();
        screen.add(player);
        //Switching
        Input.whenKey(Keyboard.KEY_I, true).onEvent(() -> {
            screen.show(space(0));
            RegisteredEntity.getAll(Player.class).stream()
                    .filter(p -> p.get("position", Vec2.class).get().subtract(Input.getMouse()).lengthSquared() < 400)
                    .findAny().ifPresent(p -> screen.show(p.view));
        });
        conn.sendMessage(5, pd.data.stream().reduce((s1, s2) -> s1 + "\n" + s2).get(), clientColor);
        Player me = new Player(pd, clientColor);
        me.create();
        Premade2D.makeArrowKeyMovement(me, 200);
        Input.whenKey(Keyboard.KEY_C, true).onEvent(() -> {
            clientColor = Color4.random();
            me.color = clientColor;
            conn.sendMessage(6, clientColor);
        });
        Core.interval(.05).onEvent(() -> {
            conn.sendMessage(7, me.get("position", Vec2.class).get(), me.get("velocity", Vec2.class).get());
        });

        Core.run();
        System.exit(0);
    }

    public static Color4 clientColor = Color4.random();

    public static void registerMessageHandlers() {
        conn.registerHandler(0, () -> {
            Vec2 pos = conn.read(Vec2.class);
            Color4 color = conn.read(Color4.class);
            onMainThread(() -> new PingCircle(pos, color).create());
        });
        conn.registerHandler(1, () -> {
            Vec2 start = conn.read(Vec2.class);
            Vec2 end = conn.read(Vec2.class);
            Color4 color = conn.read(Color4.class);
            onMainThread(() -> new PingArrow(start, end, color).create());
        });
        conn.registerHandler(5, () -> {
            String data = conn.read(String.class);
            Color4 color = conn.read(Color4.class);
            int id = conn.read(Integer.class);
            PlayerData pd = new PlayerData();
            pd.loadData(Arrays.asList(data.split("\n")));
            onMainThread(() -> new Player(pd, color, id).create());
        });
        conn.registerHandler(6, () -> {
            Color4 color = conn.read(Color4.class);
            int id = conn.read(Integer.class);
            onMainThread(() -> RegisteredEntity.getAll(Player.class).stream().filter(p -> p.id == id).findAny().get().color = color);
        });
        conn.registerHandler(7, () -> {
            Vec2 pos = conn.read(Vec2.class);
            Vec2 vel = conn.read(Vec2.class);
            int id = conn.read(Integer.class);
            onMainThread(() -> RegisteredEntity.getAll(Player.class).stream().filter(p -> p.id == id).findAny().ifPresent(p -> {
                p.get("position", Vec2.class).set(pos);
                p.get("velocity", Vec2.class).set(vel);
            }));
        });
        conn.registerHandler(10, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            Color4 color = conn.read(Color4.class);
            onMainThread(() -> Tile.grid[x][y].color = color);
        });
        conn.registerHandler(11, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            Color4 color = conn.read(Color4.class);
            onMainThread(() -> Tile.grid[x][y].drawable = new Drawable(color, "box", Tile.grid[x][y]));
        });
        conn.registerHandler(12, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            Color4 color = conn.read(Color4.class);
            onMainThread(() -> Tile.grid[x][y].drawable = new Creature(color, Tile.grid[x][y]));
        });
        conn.registerHandler(13, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            onMainThread(() -> Tile.grid[x][y].drawable = null);
        });
        conn.registerHandler(14, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            Color4 color = conn.read(Color4.class);
            onMainThread(() -> Tile.grid[x][y].drawable.color = color);
        });
        conn.registerHandler(15, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            int health = conn.read(Integer.class);
            onMainThread(() -> ((Creature) Tile.grid[x][y].drawable).health = health);
        });
    }

    public static void sendMessage(int id, Object... contents) {
        if (conn != null && !conn.isClosed()) {
            conn.sendMessage(id, contents);
        }
    }
}
