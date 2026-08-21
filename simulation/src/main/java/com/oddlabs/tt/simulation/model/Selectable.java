package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.simulation.behaviour.Behaviour;
import com.oddlabs.tt.simulation.behaviour.Controller;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.ScanFilter;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.base.event.StateChecksum;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a {@link Model} that can be selected, commanded, and augmented with accessories.
 * Base class for both {@link Unit} and {@link Building}.
 */
public abstract sealed class Selectable<T extends Template> extends Model implements Target, Animated,
        ModelToolTip permits Unit, Building {

    public enum VisualPattern {
        NONE(Color.Standard.TRANSPARENT, Color.Standard.TRANSPARENT),
        FRIENDLY(Color.Standard.GREEN, Color.Standard.DARK_GREEN),
        NEUTRAL(Color.Standard.BLUE, Color.Standard.DARK_BLUE),
        ENEMY(Color.Standard.RED, Color.Standard.DARK_RED),
        FRIENDLY_BUILDING(Color.Standard.GREEN, Color.Standard.DARK_GREEN),
        NEUTRAL_BUILDING(Color.Standard.BLUE, Color.Standard.DARK_BLUE),
        ENEMY_BUILDING(Color.Standard.RED, Color.Standard.DARK_RED);

        public final Color selectedColor;
        public final Color hoveredColor;

        VisualPattern(Color selectedColor, Color hoveredColor) {
            this.selectedColor = selectedColor;
            this.hoveredColor = hoveredColor;
        }
    }

    private final Player owner;
    private @Nullable Behaviour current_behaviour;
    private final Abilities abilities = new Abilities(Abilities.NONE);
    private final T template;
    private final List<Controller> controller_stack = new ArrayList<>();

    private boolean dead;
    private boolean should_decide;
    private Behaviour.State last = Behaviour.State.DONE;

    private int grid_x;
    private int grid_y;

    protected Selectable(Player owner, T template) {
        super(owner.getWorld());
        this.owner = owner;
        this.template = template;
    }

    @Override
    public float getShadowDiameter() {
        return template.getShadowDiameter();
    }

    public final T getTemplate() {
        return template;
    }

    public float getDefenseChance() {
        return template.getDefenseChance();
    }

    @Override
    public final float getNoDetailSize() {
        return template.getNoDetailSize();
    }

    public abstract boolean isEnabled();

    public float getHitOffsetZ() {
        return template.getHitOffsetZ(0);
    }

    public final Controller getCurrentController() {
        return controller_stack.getLast();
    }

    @Override
    public final void animate(float t) {
        last = current_behaviour.animate(t);
        switch (last) {
            case UNINTERRUPTIBLE -> {
            }
            case INTERRUPTIBLE -> {
                if (should_decide)
                    decide();
            }
            case DONE -> decide();
        }
        doAnimate(t);
        animateClientState(t);
        owner.getWorld().updateGlobalChecksum(grid_x + grid_y);
    }

    protected void doAnimate(float t) {
    }

    protected final boolean isBlocking() {
        return current_behaviour.isBlocking();
    }

    public final @Nullable Behaviour getCurrentBehaviour() {
        return current_behaviour;
    }

    public final UnitGrid getUnitGrid() {
        return owner.getWorld().getUnitGrid();
    }

    public final void scanVicinity(ScanFilter filter) {
        assert !isDead();
        getUnitGrid().scan(filter, getGridX(), getGridY());
    }

    private static boolean isAdjacent(UnitGrid unit_grid, int grid_x, int grid_y, Occupant occ) {
        int t_x = occ.getGridX();
        int t_y = occ.getGridY();
        int dx = 0;
        int dy = 0;
        if (t_x > grid_x)
            dx = 1;
        else if (t_x < grid_x)
            dx = -1;
        if (t_y > grid_y)
            dy = 1;
        else if (t_y < grid_y)
            dy = -1;
        assert dx != 0 || dy != 0 : "occ = " + occ;
        return unit_grid.getOccupant(grid_x + dx, grid_y + dy) == occ;
    }

    public final boolean isCloseEnough(float max_dist, Target target) {
        assert !isDead();
        return isCloseEnough(getUnitGrid(), max_dist, getGridX(), getGridY(), target);
    }

    public static boolean isCloseEnough(UnitGrid unit_grid, float max_dist, int grid_x, int grid_y,
            Target target) {
        if (max_dist == 0f && target instanceof Occupant occupant) {
            return isAdjacent(unit_grid, grid_x, grid_y, occupant);
        } else {
            int dx = grid_x - target.getGridX();
            int dy = grid_y - target.getGridY();
            int dist_squared = dx * dx + dy * dy;
            float max_dist_squared = max_dist * max_dist;
            return dist_squared <= max_dist_squared;
        }
    }

    @Override
    public final void register() {
        super.register();
        owner.getWorld().getAnimationManagerGameTime().registerAnimation(this);
        enable();
    }

    private void decide() {
        if (last != Behaviour.State.UNINTERRUPTIBLE) {
            doDecide();
        } else {
            should_decide = true;
        }
    }

    private void doDecide() {
        should_decide = false;
        if (current_behaviour != null) {
            current_behaviour.onCleanup();
        }
        current_behaviour = null;
        getCurrentController().decide();
    }

    protected final void forceDecide() {
        current_behaviour.forceInterrupted();
        last = Behaviour.State.INTERRUPTIBLE;
        doDecide();
    }

    public final void pushController(Controller controller) {
        assert !isDead();
        controller_stack.add(controller);
        decide();
    }

    public final void pushControllers(Controller... controllers) {
        assert !isDead();
        controller_stack.addAll(Arrays.asList(controllers));
        decide();
    }

    public final void swapController(Controller controller) {
        assert !isDead();
        controller_stack.removeLast();
        pushController(controller);
    }

    public final void popController() {
        assert !isDead();
        controller_stack.removeLast();
        decide();
    }

    public final void setBehaviour(Behaviour behaviour) {
        assert !isDead();
        assert last != Behaviour.State.UNINTERRUPTIBLE : "Invalid behaviour state";
        if (current_behaviour != null) {
            current_behaviour.onCleanup();
        }
        current_behaviour = behaviour;
    }

    public final Controller getPrimaryController() {
        assert !isDead();
        return controller_stack.size() > 1 ? controller_stack.get(1) : controller_stack.getFirst(); // Jump over the default controller
    }

    protected final void clearControllerStack() {
        Controller default_controller = controller_stack.getFirst();
        controller_stack.clear();
        controller_stack.add(default_controller);
    }

    public final void initTarget(@Nullable Target target, Action action, boolean aggressive) {
        assert !isDead();
        if (target == null)
            return;
        clearControllerStack();
        setTarget(target, action, aggressive);
    }

    protected abstract void setTarget(Target target, Action action, boolean aggressive);

    public abstract AttackScanFilter.Priority getAttackPriority();

    public abstract int getStatusValue();

    public final Abilities getAbilities() {
        return abilities;
    }

    @Override
    public final int getGridX() {
        return grid_x;
    }

    @Override
    public final int getGridY() {
        return grid_y;
    }

    public final void setGridPosition(int grid_x, int grid_y) {
        assert !isDead();
        assert owner.getWorld().getHeightMap().isGridInside(grid_x, grid_y) : grid_x + " " + grid_y + " " + this.grid_x
                + " " + this.grid_y;
        this.grid_x = grid_x;
        this.grid_y = grid_y;
    }

    public final Player getOwnerNoCheck() {
        return owner;
    }

    public final Player getOwner() {
        return getOwnerNoCheck();
    }

    public final VisualPattern getVisualPattern(Player localPlayer) {
        boolean isBuilding = this instanceof Building;
        return owner == localPlayer
                ? isBuilding ? VisualPattern.FRIENDLY_BUILDING : VisualPattern.FRIENDLY
                : localPlayer.isEnemy(owner)
                        ? isBuilding ? VisualPattern.ENEMY_BUILDING : VisualPattern.ENEMY
                : isBuilding ? VisualPattern.NEUTRAL_BUILDING : VisualPattern.NEUTRAL;
    }

    public final Color getSelectionColor(Player localPlayer, boolean selected, boolean hovered) {
        VisualPattern pattern = getVisualPattern(localPlayer);
        return selected
                ? pattern.selectedColor
                : hovered
                        ? pattern.hoveredColor
                : owner.getColor();
    }

    @Override
    public void remove() {
        super.remove();
        owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
    }

    @Override
    public final void updateChecksum(StateChecksum checksum) {
        /*		checksum.update(getGridX());
        		checksum.update(getGridY());
        		checksum.update(getPositionX());
        		checksum.update(getPositionY());*/
    }

    protected final void disable() {
        owner.getWorld().getNotificationListener().unregisterTarget(this);
        owner.getUnits().remove(this);
    }

    protected final void enable() {
        owner.getWorld().getNotificationListener().registerTarget(this);
        owner.getUnits().add(this);
//(new Throwable()).printStackTrace();
    }

    protected void removeDying() {
        disable();
        dead = true;
    }

    @Override
    public final boolean isDead() {
        return dead;
    }

    public void hit(int damage, float direction_x, float direction_y, Player attacker) {
        if (owner.isEnemy(attacker))
            owner.getWorld().getNotificationListener().newAttackNotification(this);
    }

    public static <T extends Template> Class<Selectable<T>> genericClass() {
        //noinspection unchecked
        return (Class<Selectable<T>>) (Class<?>) Selectable.class;
    }

    public static <T extends Template> Selectable<T>[] newArray(int length) {
        //noinspection unchecked
        return (Selectable<T>[]) new Selectable[length];
    }

    public static <T extends Template> Selectable<T>[] newArray(Selectable<
            T>... selectables) {
        return selectables;
    }
}
