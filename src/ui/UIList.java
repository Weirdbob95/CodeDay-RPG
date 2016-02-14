package ui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import util.Vec2;

public class UIList extends UIElement {

    public List<UIElement> parts = new LinkedList();
    public boolean horizontal;
    public boolean gravityDown;

    public void add(UIElement... a) {
        parts.addAll(Arrays.asList(a));
    }

    @Override
    public void draw() {
        super.draw();
        parts.forEach(UIElement::draw);
    }

    public static UIList list(boolean horizontal, UIElement... a) {
        UIList r = new UIList();
        r.horizontal = horizontal;
        r.add(a);
        return r;
    }

    @Override
    public void resize() {
        parts.forEach(UIElement::resize);
        double width, height;
        if (!horizontal) {
            width = parts.stream().mapToDouble(e -> e.size.x).max().orElse(10);
            height = parts.stream().mapToDouble(e -> e.size.y).sum();
        } else {
            width = parts.stream().mapToDouble(e -> e.size.x).sum();
            height = parts.stream().mapToDouble(e -> e.size.y).max().orElse(10);
        }
        size = new Vec2(width, height);
        parts.forEach(e -> {
            if (!horizontal) {
                e.size = e.size.withX(width);
            } else {
                e.size = e.size.withY(height);
            }
        });
        size = size.add(padding.multiply(2));
    }

    public void setAllBorders(boolean border) {
        parts.forEach(e -> e.border = border);
    }

    public void setAllPadding(Vec2 padding) {
        parts.forEach(e -> e.padding = padding);
    }

    @Override
    public void setPos(Vec2 pos) {
        super.setPos(pos);
        Vec2 along = padding.multiply(new Vec2(1, -1));
        for (UIElement e : parts) {
            e.setUL(getUL().add(along));
            along = along.add(horizontal ? e.size.withY(0) : e.size.withX(0).reverse());
        }
    }

    @Override
    public void update(boolean click) {
        super.update(click);
        parts.forEach(e -> e.update(click));
    }
}
