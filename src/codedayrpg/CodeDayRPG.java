package codedayrpg;

import engine.Core;
import engine.Input;
import graphics.Graphics2D;
import graphics.Window2D;
import org.lwjgl.input.Keyboard;
import util.Color4;
import util.Util;
import util.Vec2;

public class CodeDayRPG {

    public static void main(String[] args) {
        Core.init();

        int size = 50;
        int lines = 10;
        Core.render.onEvent(() -> {
            Util.forRange(-lines, lines, -lines, lines, (x, y) -> {
                Graphics2D.fillRect(new Vec2(x * size, y * size), new Vec2(size), Color4.gray(Math.random() * .5 + .5));
            });
            Util.forRange(-lines, lines + 1, n -> {
                Graphics2D.drawLine(new Vec2(-size * lines, n * size), new Vec2(size * lines, n * size));
                Graphics2D.drawLine(new Vec2(n * size, -size * lines), new Vec2(n * size, size * lines));
            });
        });

        double speed = 500;
        Input.whileKeyDown(Keyboard.KEY_W).forEach(dt -> Window2D.viewPos = Window2D.viewPos.add(new Vec2(0, speed * dt)));
        Input.whileKeyDown(Keyboard.KEY_A).forEach(dt -> Window2D.viewPos = Window2D.viewPos.add(new Vec2(speed * -dt, 0)));
        Input.whileKeyDown(Keyboard.KEY_S).forEach(dt -> Window2D.viewPos = Window2D.viewPos.add(new Vec2(0, speed * -dt)));
        Input.whileKeyDown(Keyboard.KEY_D).forEach(dt -> Window2D.viewPos = Window2D.viewPos.add(new Vec2(speed * dt, 0)));

        Core.run();
    }
}
