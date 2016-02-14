package ui;

import graphics.Graphics2D;
import java.util.function.Supplier;
import org.newdawn.slick.Color;
import util.Vec2;

public class UIText extends UIElement {

    public Supplier<String> text;

    public UIText(Supplier<String> text) {
        this.text = text;
    }

    @Override
    public void draw() {
        super.draw();
        Graphics2D.drawText(text.get(), "Default", getUL().add(padding.multiply(new Vec2(1, -1))), Color.black, (int) size.x);
    }

    @Override
    public void resize() {
        double width = Graphics2D.getTextWidth(text.get(), "Default") + 1;
        size = size.withX(Math.min(width, 400));

        double height = Graphics2D.getTextHeight(text.get(), "Default", (int) size.x);
        size = size.withY(height);

        size = size.add(padding.multiply(2));
    }
}
