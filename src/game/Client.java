package game;

import engine.Core;
import engine.Input;
import engine.Signal;
import static game.PingArrow.drawArrow;
import network.Connection;
import network.NetworkUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import util.Color4;
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
        Input.whenKey(Keyboard.KEY_C, true).onEvent(() -> clientColor = Color4.random());

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
            onMainThread(() -> Tile.grid[x][y].drawable = new Drawable(color, "box"));
        });
        conn.registerHandler(12, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            Color4 color = conn.read(Color4.class);
            onMainThread(() -> Tile.grid[x][y].drawable = new Creature(color));
        });
        conn.registerHandler(13, () -> {
            int x = conn.read(Integer.class);
            int y = conn.read(Integer.class);
            onMainThread(() -> Tile.grid[x][y].drawable = null);
        });
    }

    public static void sendMessage(int id, Object... contents) {
        if (conn != null && !conn.isClosed()) {
            conn.sendMessage(id, contents);
        }
    }
}
