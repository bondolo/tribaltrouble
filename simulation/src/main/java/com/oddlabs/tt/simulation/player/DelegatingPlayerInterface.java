package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/// Adapts [PlayerInterface] calls by delegating them lazily to a supplier-provided instance.
public final class DelegatingPlayerInterface implements PlayerInterface {

    private final Supplier<PlayerInterface> supplier;

    public DelegatingPlayerInterface(Supplier<PlayerInterface> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
    }

    private PlayerInterface delegate() {
        return Objects.requireNonNull(supplier.get(), "delegate must not be null");
    }

    @Override
    public void deployUnits(Building building, DeployType type, int num_units) {
        delegate().deployUnits(building, type, num_units);
    }

    @Override
    public void createHarvesters(Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {
        delegate().createHarvesters(building, num_tree, num_rock, num_iron, num_rubber);
    }

    @Override
    public void buildWeapons(Building building, WeaponTier tier, int num_weapons, boolean infinite) {
        delegate().buildWeapons(building, tier, num_weapons, infinite);
    }

    @Override
    public void doMagic(Unit chieftain, MagicType magic) {
        delegate().doMagic(chieftain, magic);
    }

    @Override
    public void exitTower(Building building) {
        delegate().exitTower(building);
    }

    @Override
    public void trainChieftain(Building building, boolean start) {
        delegate().trainChieftain(building, start);
    }

    @Override
    public void placeBuilding(List<? extends Selectable<?>> selection, BuildingType template_type, int placing_grid_x,
            int placing_grid_y) {
        delegate().placeBuilding(selection, template_type, placing_grid_x, placing_grid_y);
    }

    @Override
    public void setRallyPoint(Building building, Target target) {
        delegate().setRallyPoint(building, target);
    }

    @Override
    public void setRallyPoint(Building building, int grid_x, int grid_y) {
        delegate().setRallyPoint(building, grid_x, grid_y);
    }

    @Override
    public void setTarget(List<? extends Selectable<?>> selection, Target target, Action action, boolean aggressive) {
        delegate().setTarget(selection, target, action, aggressive);
    }

    @Override
    public void setTarget(Selectable<?> unit, Target target, Action action, boolean aggressive) {
        delegate().setTarget(unit, target, action, aggressive);
    }

    @Override
    public void setLandscapeTarget(List<? extends Selectable<?>> selection, int grid_x, int grid_y, Action action,
            boolean aggressive) {
        delegate().setLandscapeTarget(selection, grid_x, grid_y, action, aggressive);
    }

    @Override
    public void setLandscapeTarget(Selectable<?> unit, int grid_x, int grid_y, Action action, boolean aggressive) {
        delegate().setLandscapeTarget(unit, grid_x, grid_y, action, aggressive);
    }

    @Override
    public void setPreferredGamespeed(int speed) {
        delegate().setPreferredGamespeed(speed);
    }

    @Override
    public void changePreferredGamespeed(int delta) {
        delegate().changePreferredGamespeed(delta);
    }
}
