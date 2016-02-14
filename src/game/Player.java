package game;

import charactercreator.PlayerData;
import examples.Premade2D;
import ui.UIElement;
import util.Color4;
import util.RegisteredEntity;

public class Player extends RegisteredEntity {

    public PlayerData pd;
    public Color4 color;
    public int id;
    public UIElement view;

    public Player(PlayerData pd, Color4 color) {
        this(pd, color, -1);
    }

    public Player(PlayerData pd, Color4 color, int id) {
        this.pd = pd;
        this.color = color;
        this.id = id;
        view = pd.toUI();
    }

    @Override
    public void createInner() {
        Premade2D.makePosition(this);
        Premade2D.makeVelocity(this);
        Premade2D.makeCircleGraphics(this, () -> 20., () -> color);
    }
}
