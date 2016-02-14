package game;

import graphics.Graphics2D;
import util.Color4;
import static util.Color4.*;
import util.Vec2;

public class Creature extends Drawable {

    public int maxHealth = 100;
    public int damage = 0;

    public Creature(Color4 color) {
        super(color, "ball");
    }

    @Override
    public void draw(Vec2 pos) {
        super.draw(pos);
        Graphics2D.fillRect(pos.add(new Vec2(-20, -20)), new Vec2(40, 6), BLACK);
        Graphics2D.fillRect(pos.add(new Vec2(-20, -20)), new Vec2(40. * (maxHealth - damage) / maxHealth, 6), GREEN);
        Graphics2D.drawRect(pos.add(new Vec2(-20, -20)), new Vec2(40, 6), BLACK);
    }
}
