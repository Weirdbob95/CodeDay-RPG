package game;

import engine.AbstractEntity;
import engine.Core;
import graphics.Graphics2D;
import util.Color4;
import util.Vec2;

public class PingCircle extends AbstractEntity {

    public Vec2 pos;
    public Color4 color;

    public PingCircle(Vec2 pos, Color4 color) {
        this.pos = pos;
        this.color = color;
    }

    @Override
    public void create() {
        Core.render.toSignal(Core.time()).forEach(x -> Graphics2D.drawEllipse(pos, new Vec2(x * 100), color, 20)).addChild(this);
        Core.timer(2, this::destroy);
    }
}
