package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Distributable;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/// Translates [PlayerInterface] actions into reified [PlayerCommand] records dispatched to a consumer sink.
public final class CommandAdapterPlayerInterface implements PlayerInterface {

    private static final int[] EMPTY_IDS = new int[0];

    private final Consumer<PlayerCommand> sink;

    public CommandAdapterPlayerInterface(Consumer<PlayerCommand> sink) {
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
    }

    @Override
    public void deployUnits(Building building, DeployType type, int numUnits) {
        sink.accept(new PlayerCommand.DeployUnits(building.getId(), type, numUnits));
    }

    @Override
    public void createHarvesters(Building building, int numTree, int numRock, int numIron, int numRubber) {
        sink.accept(new PlayerCommand.CreateHarvesters(building.getId(), numTree, numRock, numIron, numRubber));
    }

    @Override
    public void buildWeapons(Building building, WeaponTier tier, int numWeapons, boolean infinite) {
        sink.accept(new PlayerCommand.BuildWeapons(building.getId(), tier, numWeapons, infinite));
    }

    @Override
    public void doMagic(Unit chieftain, MagicType magic, boolean clearStack) {
        sink.accept(new PlayerCommand.DoMagic(chieftain.getId(), magic, clearStack));
    }

    @Override
    public void exitTower(Building building) {
        sink.accept(new PlayerCommand.ExitTower(building.getId()));
    }

    @Override
    public void trainChieftain(Building building, boolean start) {
        sink.accept(new PlayerCommand.TrainChieftain(building.getId(), start));
    }

    @Override
    public void placeBuilding(List<? extends Selectable<?>> selection, BuildingType templateType, int placingGridX,
            int placingGridY) {
        sink.accept(new PlayerCommand.PlaceBuilding(toIds(selection), templateType, placingGridX, placingGridY));
    }

    @Override
    public void setRallyPoint(Building building, Target target) {
        if (target instanceof LandscapeTarget(int gridX, int gridY)) {
            sink.accept(new PlayerCommand.SetRallyPointCoords(building.getId(), gridX, gridY));
        } else {
            sink.accept(new PlayerCommand.SetRallyPointTarget(building.getId(), toId(target)));
        }
    }

    @Override
    public void setRallyPoint(Building building, int gridX, int gridY) {
        sink.accept(new PlayerCommand.SetRallyPointCoords(building.getId(), gridX, gridY));
    }

    @Override
    public void setTarget(List<? extends Selectable<?>> selection, Target target, Action action,
            boolean aggressive) {
        if (target instanceof LandscapeTarget(int gridX, int gridY)) {
            sink.accept(new PlayerCommand.SetLandscapeTarget(toIds(selection), gridX, gridY, action,
                    aggressive));
        } else {
            sink.accept(new PlayerCommand.SetTarget(toIds(selection), toId(target), action, aggressive));
        }
    }

    @Override
    public void setTarget(Selectable<?> unit, Target target, Action action, boolean aggressive) {
        if (target instanceof LandscapeTarget(int gridX, int gridY)) {
            sink.accept(new PlayerCommand.SetLandscapeTarget(toIds(unit), gridX, gridY, action,
                    aggressive));
        } else {
            sink.accept(new PlayerCommand.SetTarget(toIds(unit), toId(target), action, aggressive));
        }
    }

    @Override
    public void setLandscapeTarget(List<? extends Selectable<?>> selection, int gridX, int gridY, Action action,
            boolean aggressive) {
        sink.accept(new PlayerCommand.SetLandscapeTarget(toIds(selection), gridX, gridY, action, aggressive));
    }

    @Override
    public void setLandscapeTarget(Selectable<?> unit, int gridX, int gridY, Action action,
            boolean aggressive) {
        sink.accept(new PlayerCommand.SetLandscapeTarget(toIds(unit), gridX, gridY, action, aggressive));
    }

    @Override
    public void setPreferredGamespeed(int speed) {
        sink.accept(new PlayerCommand.SetPreferredGamespeed(speed));
    }

    @Override
    public void changePreferredGamespeed(int delta) {
        sink.accept(new PlayerCommand.ChangePreferredGamespeed(delta));
    }

    private static int toId(Target target) {
        return target instanceof Distributable d ? d.getId() : Distributable.NO_ID;
    }

    private static int[] toIds(@Nullable List<? extends Selectable<?>> selection) {
        if (selection == null || selection.isEmpty()) {
            return EMPTY_IDS;
        }
        int[] ids = new int[selection.size()];
        for (int i = 0; i < selection.size(); i++) {
            ids[i] = selection.get(i).getId();
        }
        return ids;
    }

    private static int[] toIds(Selectable<?> unit) {
        return new int[]{unit.getId()};
    }
}
