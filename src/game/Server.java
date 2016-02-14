package game;

import engine.Core;
import engine.Input;
import engine.Signal;
import static game.PingArrow.drawArrow;
import graphics.Window2D;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import network.Connection;
import network.NetworkUtils;
import org.lwjgl.input.Keyboard;
import ui.UIElement;
import ui.UIList;
import static ui.UIList.list;
import ui.UISelector;
import ui.UIShowOne;
import util.Color4;
import static util.Color4.*;
import util.Log;
import static util.ThreadManager.onMainThread;
import util.Vec2;

public class Server {

    private static List<ClientInfo> clients = new LinkedList();

    public static void main(String[] args) {
        NetworkUtils.server(conn -> {
            ClientInfo info = new ClientInfo(conn);
            clients.add(info);

            Log.print("Client " + info.id + " connected");
            conn.onClose(() -> {
                clients.remove(info);
                Log.print("Client " + info.id + " disconnected");
            });

            conn.registerHandler(0, () -> {
                Vec2 pos = conn.read(Vec2.class);
                Color4 color = conn.read(Color4.class);
                onMainThread(() -> pingCircle(pos, color));
            });
            conn.registerHandler(1, () -> {
                Vec2 start = conn.read(Vec2.class);
                Vec2 end = conn.read(Vec2.class);
                Color4 color = conn.read(Color4.class);
                onMainThread(() -> pingArrow(start, end, color));
            });
        }).start();

        registerCommand(() -> System.exit(0), "close", "end", "exit", "stop", "q", "quit");
        registerCommand(() -> {
            System.out.println("Client list:");
            clients.forEach(System.out::println);
        }, "all", "clients", "connected", "list", "players");
        new Thread(() -> startCommandLine()).start();

        //Game
        Core.init();

        Tile.init();
        UI();

        Input.whenMouse(0, true).filter(() -> !showUI).onEvent(() -> pingCircle(Input.getMouse(), serverColor));
        Signal<Vec2> down = Input.whenMouse(1, true).map(Input::getMouse);
        Input.whenMouse(1, false).filter(() -> !showUI).onEvent(() -> pingArrow(down.get(), Input.getMouse(), serverColor));
        Core.render.filter(Input.mouseSignal(1)).filter(() -> !showUI).onEvent(() -> drawArrow(down.get(), Input.getMouse(), serverColor.withA(.5)));

        Input.whenKey(Keyboard.KEY_C, true).onEvent(() -> serverColor = Color4.random());

        Core.run();

    }

    private static void pingArrow(Vec2 start, Vec2 end, Color4 color) {
        new PingArrow(start, end, color).create();
        sendToAll(1, start, end, color);
    }

    private static void pingCircle(Vec2 pos, Color4 color) {
        new PingCircle(pos, color).create();
        sendToAll(0, pos, color);
    }

    private static boolean showUI;
    private static Color4 serverColor = Color4.random();

    private static void UI() {
        //Loop
        UIShowOne screen = new UIShowOne();
        Signal<Boolean> clicked = Input.whenMouse(0, true).combineEventStreams(Input.whileMouseDown(1).limit(.05)).map(() -> true);
        Core.render.onEvent(() -> {
            screen.resize();
            screen.setUL(new Vec2(-600, 400).add(Window2D.viewPos));
            screen.update(clicked.get());
            screen.resize();
            screen.setUL(new Vec2(-600, 400).add(Window2D.viewPos));
            screen.draw();
            clicked.set(false);
        });

        //Create ui
        UISelector typeSelector = new UISelector("Tile", Arrays.asList("Tile", "Block", "Creature", "Clear"), () -> true);
        typeSelector.padding = new Vec2(20);
        UIElement showColor = new UIElement(new Vec2(200, 50));
        showColor.color = () -> WHITE;
        showColor.border = true;
        Function<Supplier<Color4>, UIElement> colorButton = s -> {
            UIElement button = new UIElement(new Vec2(30));
            button.color = s;
            button.border = true;
            button.onClick.onEvent(() -> {
                Color4 c = s.get();
                showColor.color = () -> c;
            });
            UIShowOne r = new UIShowOne();
            r.add(button);
            r.showing = button;
            return r;
        };
        UIList chooseColor1 = list(true,
                colorButton.apply(() -> RED),
                colorButton.apply(() -> ORANGE),
                colorButton.apply(() -> YELLOW),
                colorButton.apply(() -> GREEN),
                colorButton.apply(() -> BLUE),
                colorButton.apply(() -> PURPLE));
        UIList chooseColor2 = list(true,
                colorButton.apply(() -> WHITE),
                colorButton.apply(() -> Color4.gray(.75)),
                colorButton.apply(() -> Color4.gray(.5)),
                colorButton.apply(() -> Color4.gray(.25)),
                colorButton.apply(() -> BLACK),
                colorButton.apply(Core.interval(.5).map(Color4::random)));
        chooseColor1.setAllPadding(new Vec2(10));
        chooseColor2.setAllPadding(new Vec2(10));

        UIList ui = list(false, typeSelector, showColor, chooseColor1, chooseColor2);
        ui.gravity = .5;
        screen.add(ui);
        screen.color = () -> Color4.gray(.95);
        screen.border = true;

        //Key input
        Input.whenMouse(0, true).combineEventStreams(Input.whileMouseDown(1).limit(.05)).filter(() -> showUI).onEvent(()
                -> Tile.tileAt(Input.getMouse()).ifPresent(t -> {
            Color4 color = showColor.color.get();
            switch (typeSelector.chosen.get()) {
                case "Tile":
                    t.color = color;
                    sendToAll(10, t.x, t.y, color);
                    break;
                case "Block":
                    t.drawable = new Drawable(color, "box");
                    sendToAll(11, t.x, t.y, color);
                    break;
                case "Creature":
                    t.drawable = new Creature(color);
                    sendToAll(12, t.x, t.y, color);
                    break;
                case "Clear":
                    t.drawable = null;
                    sendToAll(13, t.x, t.y);
                    break;
            }
        }));
        Input.whenKey(Keyboard.KEY_SPACE, true).onEvent(() -> {
            showUI = !showUI;
            if (showUI) {
                screen.showing = ui;
            } else {
                screen.showing = new UIElement();
            }
        });
    }

    //Networking stuff
    private static class ClientInfo {

        static int maxID = 0;

        Connection conn;
        int id = maxID++;

        public ClientInfo(Connection conn) {
            this.conn = conn;
        }

        @Override
        public String toString() {
            return "Client " + id + ": " + conn;
        }
    }

    private static void sendToAll(int id, Object... data) {
        clients.forEach(ci -> ci.conn.sendMessage(id, data));
    }

    private static void relay(ClientInfo info, List<ClientInfo> clients, int id, Class... contents) {
        info.conn.registerHandler(id, () -> {
            Object[] data = new Object[contents.length];
            for (int i = 0; i < data.length; i++) {
                data[i] = info.conn.read(contents[i]);
            }
            clients.stream().filter(ci -> ci != info).forEach(ci -> ci.conn.sendMessage(id, () -> Arrays.asList(data).forEach(ci.conn::write)));
        });
    }

    private static void relayAll(ClientInfo info, List<ClientInfo> clients, int id, Class... contents) {
        info.conn.registerHandler(id, () -> {
            Object[] data = new Object[contents.length];
            for (int i = 0; i < data.length; i++) {
                data[i] = info.conn.read(contents[i]);
            }
            clients.forEach(ci -> ci.conn.sendMessage(id, () -> Arrays.asList(data).forEach(ci.conn::write)));
        });
    }

    private static final Map<String, Runnable> commands = new HashMap();

    private static void registerCommand(Runnable command, String... names) {
        for (String name : names) {
            commands.put(name.toLowerCase(), command);
        }
    }

    private static void startCommandLine() {
        Scanner in = new Scanner(System.in);
        while (true) {
            String s = in.nextLine().toLowerCase();
            if (commands.containsKey(s)) {
                commands.get(s).run();
            } else {
                System.out.println("Command not recognized");
            }
        }
    }
}
