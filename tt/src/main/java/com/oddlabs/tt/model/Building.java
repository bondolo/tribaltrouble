package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.Assets;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.gui.BuildSpinner;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.behaviour.AttackController;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.model.behaviour.NullController;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.behaviour.TransferUnitController;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.model.weapon.ThrowingWeapon;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a static building structure in the game world.
 */
public final class Building extends Selectable<BuildingTemplate> implements Occupant {
    private static final float REMOVE_DELAY = 1f / 10f;
    private static final AudioParameters COLLAPSE_AUDIO = new AudioParameters(
            Assets.SFX_BUILDING_CRASH, Assets.AUDIO_RANK_BUILDING_COLLAPSE,
            Assets.AUDIO_DISTANCE_BUILDING_COLLAPSE, Assets.AUDIO_GAIN_BUILDING_COLLAPSE, Assets.AUDIO_RADIUS_BUILDING_COLLAPSE);
    private static final @NonNull AudioParameters [] HITS_AUDIO =
            Arrays.stream(Assets.SFX_IMPACT_WOODS)
                    .map(rsrc -> new AudioParameters(rsrc, Assets.AUDIO_RANK_WEAPON_HIT,
                            Assets.AUDIO_DISTANCE_WEAPON_HIT, Assets.AUDIO_GAIN_WEAPON_HIT, Assets.AUDIO_RADIUS_WEAPON_HIT))
                    .toArray(AudioParameters[]::new);

    private static final int PLACING_BORDER = 1;
    private static final int MAX_SUPPLY_COUNT = 200;

    @SuppressWarnings({"unchecked"})
    public static final Cost COST_ROCK_WEAPON = new Cost(new Class[]{TreeSupply.class, RockSupply.class}, new int[]{2, 1});
    @SuppressWarnings({"unchecked"})
    public static final Cost COST_IRON_WEAPON = new Cost(new Class[]{TreeSupply.class, IronSupply.class}, new int[]{2, 1});
    @SuppressWarnings({"unchecked"})
    public static final Cost COST_RUBBER_WEAPON = new Cost(new Class[]{TreeSupply.class, RockSupply.class, IronSupply.class, RubberSupply.class}, new int[]{2, 1, 1, 1});

    private final Map<@NonNull Class<?>, @NonNull SupplyContainer> supply_containers = new HashMap<>();
    private final Map<@NonNull Class<?>, @NonNull BuildProductionContainer> build_containers = new HashMap<>();
    private final Map<@NonNull DeployType, @NonNull DeployContainer> deploy_containers = new EnumMap<>(DeployType.class);

    private @Nullable ChieftainContainer chieftain_container = null;
    private @Nullable WeaponsProducer weapons_producer = null;
    private float remove_delay = 0;
    private int hit_points = 1;
    private int build_points = 0;
    private float[][] old_landscape_heights;

    private @NonNull Target rally_point = this;
    private boolean is_training_chieftain = false;

    public enum BuildState {
        START, HALFBUILT, BUILT
    }

    public Building(@NonNull Player owner, @NonNull BuildingTemplate template, int grid_x, int grid_y) {
        super(owner, template);
        setGridPosition(grid_x, grid_y);
        float x = UnitGrid.coordinateFromGrid(grid_x);
        float y = UnitGrid.coordinateFromGrid(grid_y);
        setPosition(x, y);
        pushController(new NullController(this));

        addAccessory(new BuildingDamagedAccessory(this, getHitOffsetZ(), owner.getWorld().getRacesResources().getDamageSmokeTextures()));

        addAccessory(new BuildingProductionAccessory(this, getTemplate().getChimney(), owner.getWorld().getRacesResources().getSmokeTextures()));
    }

    public boolean hasRallyPoint() {
        return rally_point != this;
    }

    public @NonNull Target getRallyPoint() {
        return rally_point;
    }

    public boolean isProducing() {
        return weapons_producer != null && weapons_producer.isProducing();
    }

    @Override
    protected void doAnimate(float t) {
        if (!isDead()) {
            UnitContainer unit_container = getUnitContainer();
            if (unit_container != null)
                unit_container.animate(t);
            if (weapons_producer != null) {
                weapons_producer.animate(t);
            }

            int num_deploying = 0;
            for (DeployContainer deploy_container : deploy_containers.values()) {
                if (deploy_container.getNumSupplies() > 0) {
                    num_deploying++;
                }
            }
            if (num_deploying > 0) {
                float amount = t / num_deploying;
                for (DeployContainer deploy_container : deploy_containers.values()) {
                    if (deploy_container.getNumSupplies() > 0) {
                        deploy_container.deploy(amount);
                    }
                }
            }
        }

        if (remove_delay > 0) {
            remove_delay -= t;
            if (remove_delay <= 0) {
                remove();
                if (weapons_producer != null)
                    weapons_producer.stopSound();
                float energy = 3f;
                float fade_speed = 2.5f;

                RandomVelocityEmitter emitter = new RandomVelocityEmitter(getOwner().getWorld(), new Vector3f(getPositionX(), getPositionY(), getPositionZ()), 0f,
                        getTemplate().getSmokeRadius(), getTemplate().getSmokeHeight(), 0.05f, (float) Math.PI,
                        getTemplate().getNumFragments(), getTemplate().getNumFragments(),
                        new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, -25f),
                        new Color.Standard(1f, 1f, 1f, energy * fade_speed), new Color.Standard(0f, 0f, 0f, -fade_speed),
                        new Vector3f(1f, 1f, 1f), new Vector3f(0f, 0f, 0f), energy, .75f,
                        getOwner().getWorld().getRacesResources().getWoodFragments());
                new PointEmitterModel(getOwner().getWorld(), emitter);
            }
        }
    }

    public @Nullable UnitContainer getUnitContainer() {
        assert !isDead();
        return (UnitContainer) getSupplyContainer(Unit.class);
    }

    public @Nullable SupplyContainer getSupplyContainer(@NonNull Class<?> key) {
        assert !isDead();
        return supply_containers.get(key);
    }

    public @Nullable BuildSupplyContainer getBuildSupplyContainer(@NonNull Class<?> key) {
        assert !isDead();
        return build_containers.get(key);
    }

    public DeployContainer getDeployContainer(DeployType type) {
        assert !isDead();
        return deploy_containers.get(type);
    }

    public @Nullable ChieftainContainer getChieftainContainer() {
        assert !isDead();
        return chieftain_container;
    }

    @Override
    public boolean isEnabled() {
        return !isDead();
    }

    public int getUnitCount() {
        assert !isDead();
        return getUnitContainer().getNumSupplies();
    }

    public boolean canExitTower() {
        return !isDead() && getAbilities().hasAbilities(Abilities.ATTACK) && getUnitContainer().getNumSupplies() > 0 && getOwner().canExitTowers() &&
                !(((MountUnitContainer) getUnitContainer()).getUnit().getCurrentController() instanceof StunController);
    }

    public void exitTower() {
        assert !isDead();
        UnitContainer container = getUnitContainer();
        if (canExitTower()) {
            container.exit();
        }
    }

    public void deployUnits(@NonNull DeployType type, int num_units) {
        assert !isDead();
        getOwner().getWorld().updateGlobalChecksum(type.ordinal());
        getOwner().getWorld().updateGlobalChecksum(num_units);
        getDeployContainer(type).orderSupply(num_units);
    }

    public void createHarvesters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createHarvesters(TreeSupply.class, num_tree);
        createHarvesters(RockSupply.class, num_rock);
        createHarvesters(IronSupply.class, num_iron);
        createHarvesters(RubberSupply.class, num_rubber);
    }

    private void createHarvesters(@NonNull Class<? extends Supply> supply_type, int amount) {
        Race race = getOwner().getRace();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().prepareDeploy(-1);
            getUnitContainer().exit();
            Unit unit = createUnit(null, race.getUnitTemplate(Race.UNIT_PEON));
            unit.pushController(new GatherController<>(unit, null, supply_type));
        }
    }

    public void buildWeapons(@NonNull Class<? extends ThrowingWeapon> type, int num_weapons, boolean infinite) {
        assert !isDead();
        if (infinite)
            getOwner().getWorld().updateGlobalChecksum(num_weapons);
        else
            getOwner().getWorld().updateGlobalChecksum(1000000);
        ((BuildProductionContainer) getBuildSupplyContainer(type)).orderSupply(num_weapons, infinite);
    }

    public boolean canBuildChieftain() {
        return !isDead() && chieftain_container != null && getOwner().canBuildChieftains() && !getOwner().hasActiveChieftain() && !getOwner().isTrainingChieftain();
    }

    public boolean canStopChieftain() {
        return !isDead() && chieftain_container != null && chieftain_container.isTraining();
    }

    public void trainChieftain(boolean start) {
        if (canBuildChieftain() && start) {
            chieftain_container.startTraining();
            getOwner().setTrainingChieftain(true);
            is_training_chieftain = true;
        } else if (canStopChieftain() && !start) {
            chieftain_container.stopTraining();
            getOwner().setTrainingChieftain(false);
            is_training_chieftain = false;
        }
    }

    public void deployChieftain() {
        chieftain_container.stopTraining();
        getOwner().setTrainingChieftain(false);
        is_training_chieftain = false;
        Unit chieftain = createUnit(null, getOwner().getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN));
        getOwner().setActiveChieftain(chieftain);
    }

    private @NonNull Unit createUnit(@Nullable Target rally_point, @NonNull UnitTemplate template) {
        return new Unit(getOwner(), getPositionX(), getPositionY(), rally_point, template, null, true, true);
    }

    public void createArmy(int num_peon, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createArmy(num_peon, Race.UNIT_PEON);
        createArmy(num_rock, Race.UNIT_WARRIOR_ROCK);
        createArmy(num_iron, Race.UNIT_WARRIOR_IRON);
        createArmy(num_rubber, Race.UNIT_WARRIOR_RUBBER);
    }

    private void createArmy(int amount, int template) {
        Race race = getOwner().getRace();
        checkRallyPoint();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().prepareDeploy(-1);
            getUnitContainer().exit();
            Unit unit = createUnit(hasRallyPoint() ? rally_point : null, race.getUnitTemplate(template));
            if (getAbilities().hasAbilities(Abilities.REPRODUCE) && !hasRallyPoint()) {
                unit.pushController(new TransferUnitController(unit));
            }
        }
    }

    public void createTransporters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createTransporters(num_tree, TreeSupply.class);
        createTransporters(num_rock, RockSupply.class);
        createTransporters(num_iron, IronSupply.class);
        createTransporters(num_rubber, RubberSupply.class);
    }

    private void checkRallyPoint() {
        if (hasRallyPoint() && rally_point.isDead())
            rally_point = this;
    }

    private void createTransporters(int amount, Class<? extends Supply> supply) {
        Race race = getOwner().getRace();
        checkRallyPoint();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().prepareDeploy(-1);
            getUnitContainer().exit();
            Unit unit = createUnit(hasRallyPoint() ? rally_point : null, race.getUnitTemplate(Race.UNIT_PEON));
            unit.getSupplyContainer().increaseSupply(unit.getSupplyContainer().getMaxSupplyCount(), supply);
            //	getSupplyContainer(supply).increaseSupply(-unit.getSupplyContainer().getMaxSupplyCount());
            //	getSupplyContainer(supply).prepareDeploy(-unit.getSupplyContainer().getMaxSupplyCount());
        }

    }

    public boolean isDamaged() {
        assert !isDead();
        return hit_points > 0 && hit_points < getTemplate().getMaxHitPoints();
    }

    public int getHitPoints() {
        return hit_points;
    }

    private void adjustHitPoints(int amount) {
        setHitPoints(hit_points + amount);
    }
    private void setHitPoints(int new_hit_points) {
        hit_points = Math.clamp(new_hit_points, 0, getTemplate().getMaxHitPoints());
    }

    public void repair(int amount) {
        assert !isDead();
        assert isPlaced();
        if (!isDamaged())
            return;

        adjustHitPoints(amount);
        if (build_points < getTemplate().getMaxHitPoints()) {
            build_points = Math.min(build_points + amount, getTemplate().getMaxHitPoints());
            reinsert();
            if (build_points == getTemplate().getMaxHitPoints()) {
                getOwner().getWorld().getNotificationListener().newSelectableNotification(this);
                getAbilities().addAbilities(getTemplate().getAbilities());
                supply_containers.put(Unit.class, getTemplate().getUnitContainerFactory().createContainer(this));
                if (getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
                    SupplyContainer tree_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    SupplyContainer rock_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    SupplyContainer iron_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    SupplyContainer rubber_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(TreeSupply.class, tree_supply);
                    supply_containers.put(RockSupply.class, rock_supply);
                    supply_containers.put(IronSupply.class, iron_supply);
                    supply_containers.put(RubberSupply.class, rubber_supply);

                    SupplyContainer rock_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(RockAxeWeapon.class, rock_weapon_container);
                    supply_containers.put(RockSpearWeapon.class, rock_weapon_container);
                    SupplyContainer iron_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(IronAxeWeapon.class, iron_weapon_container);
                    supply_containers.put(IronSpearWeapon.class, iron_weapon_container);
                    SupplyContainer rubber_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(RubberAxeWeapon.class, rubber_weapon_container);
                    SupplyContainer rubber_spear_weapon = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(RubberSpearWeapon.class, rubber_spear_weapon);

                    BuildProductionContainer rock_axe_weapon = new BuildProductionContainer(BuildSpinner.INFINITE_LIMIT,
                            rock_weapon_container,
                            this,
                            COST_ROCK_WEAPON,
                            40f);
                    BuildProductionContainer iron_axe_weapon = new BuildProductionContainer(BuildSpinner.INFINITE_LIMIT,
                            iron_weapon_container,
                            this,
                            COST_IRON_WEAPON,
                            80f);
                    BuildProductionContainer rubber_axe_weapon = new BuildProductionContainer(BuildSpinner.INFINITE_LIMIT,
                            rubber_weapon_container,
                            this,
                            COST_RUBBER_WEAPON,
                            120f);
                    build_containers.put(RockAxeWeapon.class, rock_axe_weapon);
                    build_containers.put(IronAxeWeapon.class, iron_axe_weapon);
                    build_containers.put(RubberAxeWeapon.class, rubber_axe_weapon);
                    BuildProductionContainer[] production_containers = new BuildProductionContainer[]{rock_axe_weapon, iron_axe_weapon, rubber_axe_weapon};

                    weapons_producer = new WeaponsProducer(this, (WorkerUnitContainer) getUnitContainer(), production_containers);

                    deploy_containers.put(DeployType.ROCK_WARRIOR, new DeployContainer(this, 1f, DeployType.ROCK_WARRIOR, RockAxeWeapon.class));
                    deploy_containers.put(DeployType.IRON_WARRIOR, new DeployContainer(this, 1.5f, DeployType.IRON_WARRIOR, IronAxeWeapon.class));
                    deploy_containers.put(DeployType.RUBBER_WARRIOR, new DeployContainer(this, 2f, DeployType.RUBBER_WARRIOR, RubberAxeWeapon.class));
                    deploy_containers.put(DeployType.PEON, new DeployContainer(this, .5f, DeployType.PEON, null));
                    deploy_containers.put(DeployType.PEON_HARVEST_TREE, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_TREE, null));
                    deploy_containers.put(DeployType.PEON_TRANSPORT_TREE, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_TREE, TreeSupply.class));
                    deploy_containers.put(DeployType.PEON_HARVEST_ROCK, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_ROCK, null));
                    deploy_containers.put(DeployType.PEON_TRANSPORT_ROCK, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_ROCK, RockSupply.class));
                    deploy_containers.put(DeployType.PEON_HARVEST_IRON, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_IRON, null));
                    deploy_containers.put(DeployType.PEON_TRANSPORT_IRON, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_IRON, IronSupply.class));
                    deploy_containers.put(DeployType.PEON_HARVEST_RUBBER, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_RUBBER, null));
                    deploy_containers.put(DeployType.PEON_TRANSPORT_RUBBER, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_RUBBER, RubberSupply.class));
                } else if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
                    chieftain_container = new ChieftainContainer(this);
                    deploy_containers.put(DeployType.PEON, new DeployContainer(this, .5f, DeployType.PEON, null));
                }
            }
        }
    }

    public static boolean isPlacingLegal(@NonNull UnitGrid unit_grid, @NonNull BuildingTemplate template, int grid_x, int grid_y) {
        return doIsPlacingLegal(unit_grid, grid_x, grid_y, template.getPlacingSize());
    }

    public boolean isPlacingLegal() {
        return !isDead() && getOwner().canBuild(getTemplate().getTemplateID()) &&
                doIsPlacingLegal(getUnitGrid(), getGridX(), getGridY(), getTemplate().getPlacingSize() - PLACING_BORDER);
    }

    public boolean isPlaced() {
        assert !isDead();
        return build_points != 0;
    }

    public boolean isComplete() {
        return build_points == getTemplate().getMaxHitPoints();
    }

    @Override
    public float getHitOffsetZ() {
        return getTemplate().getHitOffsetZ(getRenderLevel().ordinal());
    }

    public static boolean doIsPlacingLegal(@NonNull UnitGrid unit_grid, int grid_x, int grid_y, int size) {
        if (!unit_grid.getHeightMap().canBuild(grid_x, grid_y, size))
            return false;

        for (int y = 0; y < size * 2 - 1; y++) {
            for (int x = 0; x < size * 2 - 1; x++) {
                int current_grid_x = grid_x + x - (size - 1);
                int current_grid_y = grid_y + y - (size - 1);
                if (current_grid_x >= unit_grid.getGridSize() || current_grid_y >= unit_grid.getGridSize() ||
                        current_grid_x < 0 || current_grid_y < 0 || unit_grid.isGridOccupied(current_grid_x, current_grid_y))
                    return false;
            }
        }
        return true;
    }

    @Override
    public AttackScanFilter.@NonNull Priority getAttackPriority() {
        return getAbilities().hasAbilities(Abilities.ATTACK)
                ? AttackScanFilter.Priority.TOWER
                : getAbilities().hasAbilities(Abilities.BUILD_ARMIES)
                  ? AttackScanFilter.Priority.ARMORY
                  : AttackScanFilter.Priority.QUARTERS;
    }

    @Override
    protected void setTarget(@NonNull Target target, @NonNull Action action, boolean aggressive) {
        if (getAbilities().hasAbilities(Abilities.ATTACK)) {
            if (target != this) {
                Unit unit = ((MountUnitContainer) getUnitContainer()).getUnit();
                boolean kill_friendly = action == Action.ATTACK;
                if (unit != null && unit.canAttack(target, kill_friendly))
                    unit.pushController(new AttackController(unit, (Selectable<?>) target));
            }
        } else {
            setRallyPoint(target);
        }
    }

    public void place() {
        assert !isDead();
        assert isPlacingLegal();
        register();
        occupy();
        flattenLandscape();
        int result = getOwner().getBuildingCountContainer().increaseSupply(1);
        assert (result == 1) : "Too many buildings";
        build_points = 1;
        reinsert();
    }

    @Override
    public float getSize() {
        assert !isDead();
        float radius = (getTemplate().getPlacingSize() - 1);
        return (float) Math.sqrt(2) * radius + .1f;
    }

    @Override
    public int getPenalty() {
        assert !isDead();
        return Occupant.STATIC;
    }

    @Override
    protected void removeDying() {

        RandomVelocityEmitter collapse_emitter = new RandomVelocityEmitter(getOwner().getWorld(), new Vector3f(getPositionX(), getPositionY(), getPositionZ()), 0f, 0f,
                getTemplate().getSmokeRadius(), getTemplate().getSmokeHeight(), 1f, 1f,
                30, 400f,
                new Vector3f(0f, 0f, .1f), new Vector3f(0f, 0f, -2.5f),
                new Color.Standard(1f, .8f, .6f, 1f), new Color.Standard(0f, 0f, 0f, -1f),
                new Vector3f(1f, 1f, 1f), new Vector3f(7.5f, 7.5f, 7.5f), 1f, 0.75f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                getOwner().getWorld().getRacesResources().getSmokeTextures());
        new PointEmitterModel(getOwner().getWorld(), collapse_emitter, getOwner().getWorld().getAnimationManagerRealTime());

        {
            float energy = 3f;
            float fade_speed = 2.5f;

            RandomVelocityEmitter emitter = new RandomVelocityEmitter(getOwner().getWorld(), new Vector3f(getPositionX(), getPositionY(), getPositionZ()), 0f,
                    getTemplate().getSmokeRadius(), getTemplate().getSmokeHeight(), 0.05f, (float) Math.PI,
                    getTemplate().getNumFragments(), getTemplate().getNumFragments(),
                    new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, -25f),
                    new Color.Standard(1f, 1f, 1f, energy * fade_speed), new Color.Standard(0f, 0f, 0f, -fade_speed),
                    new Vector3f(1f, 1f, 1f), new Vector3f(0f, 0f, 0f), energy, .75f,
                    getOwner().getWorld().getRacesResources().getWoodFragments());
            new PointEmitterModel(getOwner().getWorld(), emitter, getOwner().getWorld().getAnimationManagerRealTime());
        }

        remove_delay = REMOVE_DELAY;
        getOwner().getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(), COLLAPSE_AUDIO);
        if (getUnitContainer() != null) {
            while (getUnitContainer().getNumSupplies() > 0) {
                Unit unit = getUnitContainer().exit();
                if (unit != null)
                    unit.removeNow();
            }
        }
        SupplyContainer worker_container = getBuildSupplyContainer(Unit.class);
        if (worker_container != null) {
            int result = getOwner().getUnitCountContainer().increaseSupply(-worker_container.getNumSupplies());
            assert result == -worker_container.getNumSupplies();
        }
        for (DeployContainer deploy_container : deploy_containers.values()) {
            int result = getOwner().getUnitCountContainer().increaseSupply(-deploy_container.getNumSupplies());
            assert result == -deploy_container.getNumSupplies();
        }
        free();
        undoLandscape();
        int result = getOwner().getBuildingCountContainer().increaseSupply(-1);
        assert result == -1;
        super.removeDying();
    }

    public boolean isValidRallyPoint(Target t) {
        return t instanceof Building b &&
                getOwner() == b.getOwner() &&
                b.getAbilities().hasAbilities(Abilities.RALLY_TO);
    }

    public void setRallyPoint(@NonNull Target target) {
        if (!getOwner().canSetRallyPoints())
            return;
        rally_point = isValidRallyPoint(target)
                ? target
                : getUnitGrid().findGridTargets(target.getGridX(), target.getGridY(), 1, false)[0];
    }

    public @NonNull BuildState getRenderLevel() {
        var max_points = getTemplate().getMaxHitPoints();
        return build_points == max_points
                ? BuildState.BUILT
                : (float) build_points / max_points < .5
                  ? BuildState.START : BuildState.HALFBUILT;
    }

    @Override
    public @NonNull SpriteKey getSpriteRenderer() {
        return switch (getRenderLevel()) {
            case START -> getTemplate().getStartRenderer();
            case HALFBUILT -> getTemplate().getHalfbuiltRenderer();
            case BUILT -> getTemplate().getBuiltRenderer();
        };
    }

    private void flattenLandscape() {
        int size = getTemplate().getPlacingSize();
        int height_points = (size - PLACING_BORDER) * 2;
        int offset_x = getGridX() - (size - 1);
        int offset_y = getGridY() - (size - 1);
        float total_height = 0;
        old_landscape_heights = new float[height_points][height_points];
        for (int y = 0; y < height_points; y++) {
            for (int x = 0; x < height_points; x++) {
                float old_height = getOwner().getWorld().getHeightMap().getWrappedHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER);
                old_landscape_heights[y][x] = old_height;
                total_height += old_height;
            }
        }

        float new_height = total_height / (height_points * height_points);
        for (int y = 0; y < height_points; y++) {
            for (int x = 0; x < height_points; x++) {
                getOwner().getWorld().getHeightMap().editHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER, new_height);
            }
        }
    }

    private void undoLandscape() {
        int size = getTemplate().getPlacingSize();
        int offset_x = getGridX() - (size - 1);
        int offset_y = getGridY() - (size - 1);
        for (int y = 0; y < old_landscape_heights.length; y++) {
            for (int x = 0; x < old_landscape_heights[y].length; x++) {
                getOwner().getWorld().getHeightMap().editHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER, old_landscape_heights[y][x]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void occupy() {
        UnitGrid grid = getUnitGrid();
        grid.getRegion(getGridX(), getGridY()).registerObject((Class<Building>) getClass(), this);
        int size = getTemplate().getPlacingSize() * 2 - 1;
        for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
            for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
                grid.occupyGrid(getGridX() - size / 2 + x, getGridY() - size / 2 + y, this);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void free() {
        UnitGrid grid = getUnitGrid();
        grid.getRegion(getGridX(), getGridY()).unregisterObject((Class<Building>) getClass(), this);
        int size = getTemplate().getPlacingSize() * 2 - 1;
        for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
            for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
                grid.freeGrid(getGridX() - size / 2 + x, getGridY() - size / 2 + y, this);
            }
        }
    }

    @Override
    public void hit(int damage, float dir_x, float dir_y, @NonNull Player owner) {
        super.hit(damage, dir_x, dir_y, owner);
        if (!isDead()) {
            adjustHitPoints(- damage);
            World world = getOwner().getWorld();
            world.getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(), HITS_AUDIO[world.getRandom().nextInt(HITS_AUDIO.length)]);
            if (hit_points <= 0) {
                // stats
                getOwner().buildingLost();
                owner.buildingDestroyed();
                if (is_training_chieftain)
                    getOwner().setTrainingChieftain(false);
                removeDying();
            }
        }
    }

    @Override
    public @NonNull String toString() {
        return "Building: isDead() = " + isDead();
    }

    public void fillSupplies(@NonNull Class<?> key, int max) {
        SupplyContainer container = getSupplyContainer(key);
        if (container != null) {
            container.increaseSupply(Math.min(container.getMaxSupplyCount() - container.getNumSupplies(), max));
        }
    }

    public void removeSupplies(@NonNull Class<?> key) {
        SupplyContainer container = getSupplyContainer(key);
        if (container != null) {
            container.increaseSupply(-container.getNumSupplies());
        }
    }

    @Override
    public int getStatusValue() {
        return getAbilities().hasAbilities(Abilities.REPRODUCE)
                ? getUnitContainer().getNumSupplies()
                : getAbilities().hasAbilities(Abilities.BUILD_ARMIES)
                  ? getUnitContainer().getNumSupplies() +
                getSupplyContainer(RockAxeWeapon.class).getNumSupplies() +
                    getSupplyContainer(IronAxeWeapon.class).getNumSupplies() * 3 +
                    getSupplyContainer(RubberAxeWeapon.class).getNumSupplies() * 8
                  : 0;
    }

    public void printDebugInfo() {
        IO.println("-----------------------------------");
        if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            IO.println("Units = " + getUnitContainer().getNumSupplies());
        } else if (getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
            IO.println("Units = " + getUnitContainer().getNumSupplies());
            IO.println("Tree = " + getSupplyContainer(TreeSupply.class).getNumSupplies());
            IO.println("Rock = " + getSupplyContainer(RockSupply.class).getNumSupplies());
            IO.println("Iron = " + getSupplyContainer(IronSupply.class).getNumSupplies());
            IO.println("Rubber = " + getSupplyContainer(RubberSupply.class).getNumSupplies());
            IO.println("Rock Weapons = " + getSupplyContainer(RockAxeWeapon.class).getNumSupplies());
            IO.println("Iron Weapons = " + getSupplyContainer(IronAxeWeapon.class).getNumSupplies());
            IO.println("Rubber Weapons = " + getSupplyContainer(RubberAxeWeapon.class).getNumSupplies());
        }
    }
}
