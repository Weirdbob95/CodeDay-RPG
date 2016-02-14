package game;

import engine.AbstractEntity;
import engine.Core;
import graphics.Graphics2D;
import util.Color4;
import util.Vec2;

public class PingArrow extends AbstractEntity {

    public Vec2 start, end;
    public Color4 color;

    public PingArrow(Vec2 start, Vec2 end, Color4 color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    @Override
    public void create() {
        onRender(() -> drawArrow(start, end, color));
        Core.timer(2, this::destroy);
    }

    public static void drawArrow(Vec2 start, Vec2 end, Color4 color) {
        Graphics2D.drawLine(start, end, color, 2);
        Graphics2D.drawLine(end.add(end.subtract(start).rotate(Math.PI / 4).withLength(-20)), end, color, 2);
        Graphics2D.drawLine(end.add(end.subtract(start).rotate(-Math.PI / 4).withLength(-20)), end, color, 2);
    }
}
