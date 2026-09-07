package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.NonFocusGroup;
import com.oddlabs.tt.gui.NonFocusIconButton;
import com.oddlabs.tt.gui.Placement;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.simulation.model.BuildingType;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.client.camera.GameCamera;
import com.oddlabs.tt.client.delegate.CameraDelegate;
import com.oddlabs.tt.client.delegate.PlacingDelegate;
import com.oddlabs.tt.client.delegate.RallyPointDelegate;
import com.oddlabs.tt.client.delegate.TargetDelegate;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.SupplyCounter;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.controller.ActionContext;
import com.oddlabs.tt.client.controller.ActionController;
import com.oddlabs.tt.client.controller.ActionControllerStack;
import com.oddlabs.tt.client.controller.ArmoryActionController;
import com.oddlabs.tt.client.controller.ArmorySubmenuController;
import com.oddlabs.tt.client.controller.QuartersActionController;
import com.oddlabs.tt.client.controller.SubmenuType;
import com.oddlabs.tt.client.controller.TowerActionController;
import com.oddlabs.tt.client.controller.UnitActionController;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.client.viewer.WorldViewer;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * A GUI panel that displays action and building construction buttons for the player.
 */
public final class ActionButtonPanel extends GUIObject implements Animated, ActionContext {
    private static final int GROUP_LEFT_OFFSET = 10;
    private static final int GROUP_BOTTOM_OFFSET = 10;
    private static final int GROUP_RIGHT_OFFSET = 10;
    private static final int GROUP_TOP_OFFSET = 20;

    private static final ResourceBundle bundle = ResourceBundle.getBundle(ActionButtonPanel.class.getName());
    private final ActionControllerStack controllerStack = new ActionControllerStack();
    private final Group unit_group = new NonFocusGroup();
    private final Group peon_group = new NonFocusGroup();
    private final Group chieftain_group = new NonFocusGroup();
    private final Group tower_group = new NonFocusGroup();
    private final Group quarters_status_group = new NonFocusGroup();
    private final Group quarters_group = new NonFocusGroup();
    private final Group status_group = new NonFocusGroup();
    private final Group armory_group = new NonFocusGroup();
    private final Group harvest_group = new NonFocusGroup();
    private final Group build_group = new NonFocusGroup();
    private final Group army_group = new NonFocusGroup();
    private final Group transport_group = new NonFocusGroup();
    private final NonFocusIconButton tower_attack_button;
    private final NonFocusIconButton tower_exit_button;
    //	private boolean tower_exit_button_disabled;
    private final NonFocusIconButton move_button;
    private final NonFocusIconButton attack_button;
    private final NonFocusIconButton gather_repair_button;
    private final NonFocusIconButton quarters_button;
    //	private boolean quarters_button_disabled;
    private final RechargeButton magic1_button;
    private final RechargeButton magic2_button;
    private final NonFocusIconButton armory_button;
    //	private boolean armory_button_disabled;
    private final NonFocusIconButton tower_button;
    //	private boolean tower_button_disabled;
    private final NonFocusIconButton harvest_button;
    private final NonFocusIconButton build_button;
    private final NonFocusIconButton army_button;
    private final NonFocusIconButton transport_button;
    private final StatusIcon unit_status;
    private final StatusIcon weapon_rock_status;
    private final StatusIcon weapon_iron_status;
    private final StatusIcon weapon_rubber_status;
    private final StatusIcon tree_status;
    private final StatusIcon rock_status;
    private final StatusIcon iron_status;
    private final StatusIcon rubber_status;
    private final WatchStatusIcon quarters_unit_status;
    private final DeploySpinner quarters_peon_button;
    private final ChieftainButton quarters_chieftain_button;
    private final DeploySpinner harvest_tree_button;
    private final DeploySpinner harvest_rock_button;
    private final DeploySpinner harvest_iron_button;
    private final DeploySpinner harvest_rubber_button;
    private final BuildSpinner build_weapon_rock_button;
    private final BuildSpinner build_weapon_iron_button;
    private final BuildSpinner build_weapon_rubber_button;
    private final DeploySpinner army_peon_button;
    private final DeploySpinner army_warrior_rock_button;
    private final DeploySpinner army_warrior_iron_button;
    private final DeploySpinner army_warrior_rubber_button;
    private final DeploySpinner transport_tree_button;
    private final DeploySpinner transport_rock_button;
    private final DeploySpinner transport_iron_button;
    private final DeploySpinner transport_rubber_button;
    private final GameCamera camera;
    private final WorldViewer viewer;
    private @Nullable Group current_submenu = null;
    private boolean update = false;
    private boolean current_quarters = false;
    private boolean current_armory = false;
    private @Nullable Building current_building;
    private boolean current_unit = false;
    private boolean current_peon = false;
    private @Nullable Unit current_chieftain;
    private boolean current_tower = false;

    public ActionButtonPanel(WorldViewer viewer, GameCamera camera) {
        this(viewer, camera, viewer.getGUIRoot().getWidth(), viewer.getGUIRoot().getHeight());
    }

    public ActionButtonPanel(final WorldViewer viewer, GameCamera camera, int width, int height) {
        this.viewer = viewer;
        this.camera = camera;
        controllerStack.addListener(this::onControllerChanged);
        GUIIcons icons = GUIIcons.getIcons();

        var race_icons = switch (viewer.getLocalPlayer().getRaceInfo().getRaceType()) {
            case Race.VIKINGS -> icons.getVikingIcons();
            case Race.NATIVES -> icons.getNativeIcons();
        };
        Skin skin = Skin.getSkin();
        String widest_char = new String(Character.toChars(skin.getEditFont().getWidestCodepoint("0123456789")));
        int label_width = skin.getEditFont().getWidth(widest_char + widest_char + widest_char);

        move_button = new NonFocusIconButton(
                race_icons.moveIcon(), GameAction.UNIT_MOVE,
                () -> i18n("move_tip", getBinding(GameAction.UNIT_MOVE))
        );
        move_button.setIconDisabler(() -> !viewer.getLocalPlayer().canMove());
        bindAction(move_button, UnitActionController.class, UnitActionController::executeMove);
        unit_group.addChild(move_button);

        attack_button = new NonFocusIconButton(
                race_icons.attackIcon(), GameAction.UNIT_ATTACK,
                () -> i18n("attack_tip", getBinding(GameAction.UNIT_ATTACK))
        );
        attack_button.setIconDisabler(() -> !viewer.getLocalPlayer().canAttack());
        bindAction(attack_button, UnitActionController.class, UnitActionController::executeAttack);
        unit_group.addChild(attack_button);

        move_button.place();
        attack_button.place(move_button, Placement.BOTTOM_MID);
        unit_group.compileCanvas(GROUP_LEFT_OFFSET, 0, GROUP_RIGHT_OFFSET, GROUP_BOTTOM_OFFSET);

        gather_repair_button = new NonFocusIconButton(
                race_icons.gatherRepairIcon(), GameAction.UNIT_GATHER,
                () -> i18n("gather_repair_tip", getBinding(GameAction.UNIT_GATHER))
        );
        peon_group.addChild(gather_repair_button);
        bindAction(gather_repair_button, UnitActionController.class, UnitActionController::executeGatherRepair);
        gather_repair_button.setIconDisabler(() -> !viewer.getLocalPlayer().canRepair());
        quarters_button = new NonFocusIconButton(
                race_icons.quartersIcon(), GameAction.UNIT_BUILD_QUARTERS,
                () -> i18n("quarters_tip", getBinding(GameAction.UNIT_BUILD_QUARTERS))
        );
        peon_group.addChild(quarters_button);
        bindAction(quarters_button, UnitActionController.class, u -> u.executeBuild(BuildingType.QUARTERS));
        quarters_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuild(BuildingType.QUARTERS));
        armory_button = new NonFocusIconButton(
                race_icons.armoryIcon(), GameAction.UNIT_BUILD_ARMORY,
                () -> i18n("armory_tip", getBinding(GameAction.UNIT_BUILD_ARMORY))
        );
        peon_group.addChild(armory_button);
        bindAction(armory_button, UnitActionController.class, u -> u.executeBuild(BuildingType.ARMORY));
        armory_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuild(BuildingType.ARMORY));
        tower_button = new NonFocusIconButton(
                race_icons.towerIcon(), GameAction.UNIT_BUILD_TOWER,
                () -> i18n("tower_tip", getBinding(GameAction.UNIT_BUILD_TOWER))
        );
        peon_group.addChild(tower_button);
        bindAction(tower_button, UnitActionController.class, u -> u.executeBuild(BuildingType.TOWER));
        tower_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuild(BuildingType.TOWER));
        gather_repair_button.place();
        quarters_button.place(gather_repair_button, Placement.BOTTOM_MID);
        armory_button.place(quarters_button, Placement.BOTTOM_MID);
        tower_button.place(armory_button, Placement.BOTTOM_MID);
        peon_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, 0);

        PlayerInterface player_interface = viewer.getPeerHub().getPlayerInterface();
        MagicType magic1Type = viewer.getLocalPlayer().getRaceInfo().getMagicType(0);
        magic1_button = new RechargeButton(
                player_interface, race_icons.magic1Icon(), GameAction.MAGIC_1,
                () -> getMagicTooltip(magic1Type), magic1Type
        );
        chieftain_group.addChild(magic1_button);
        bindAction(magic1_button, UnitActionController.class, u -> u.executeMagic(0));
        MagicType magic2Type = viewer.getLocalPlayer().getRaceInfo().getMagicType(1);
        magic2_button = new RechargeButton(
                player_interface, race_icons.magic2Icon(), GameAction.MAGIC_2,
                () -> getMagicTooltip(magic2Type), magic2Type
        );
        chieftain_group.addChild(magic2_button);
        bindAction(magic2_button, UnitActionController.class, u -> u.executeMagic(1));
        magic1_button.place();
        magic2_button.place(magic1_button, Placement.BOTTOM_MID);
        chieftain_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, 0);

        tower_attack_button = new NonFocusIconButton(
                race_icons.attackIcon(),
                GameAction.UNIT_ATTACK, () -> i18n("attack_tip", getBinding(GameAction.UNIT_ATTACK))
        );
        tower_group.addChild(tower_attack_button);
        bindAction(tower_attack_button, TowerActionController.class, TowerActionController::executeTowerAttack);
        tower_exit_button = new NonFocusIconButton(
                race_icons.towerExitIcon(), GameAction.UNIT_EXIT_TOWER,
                () -> i18n("exit_tip", getBinding(GameAction.UNIT_EXIT_TOWER))
        );
        tower_group.addChild(tower_exit_button);
        bindAction(tower_exit_button, TowerActionController.class, TowerActionController::executeExitTower);
        tower_attack_button.place();
        tower_exit_button.place(tower_attack_button, Placement.BOTTOM_MID);
        tower_group.compileCanvas();

        unit_status = new StatusIcon(label_width, race_icons.unitStatusIcon(), i18n("units_tip"));
        status_group.addChild(unit_status);
        weapon_rock_status = new StatusIcon(label_width, race_icons.weaponRockStatusIcon(), i18n("rock_weapons_tip"));
        status_group.addChild(weapon_rock_status);
        weapon_iron_status = new StatusIcon(label_width, race_icons.weaponIronStatusIcon(), i18n("iron_weapons_tip"));
        status_group.addChild(weapon_iron_status);
        weapon_rubber_status = new StatusIcon(
                label_width, race_icons.weaponRubberStatusIcon(), i18n(
                        "chicken_weapons_tip")
        );
        status_group.addChild(weapon_rubber_status);
        tree_status = new StatusIcon(label_width, icons.getTreeStatusIcon(), i18n("tree_resources_tip"));
        status_group.addChild(tree_status);
        rock_status = new StatusIcon(label_width, icons.getRockStatusIcon(), i18n("rock_resources_tip"));
        status_group.addChild(rock_status);
        iron_status = new StatusIcon(label_width, icons.getIronStatusIcon(), i18n("iron_resources_tip"));
        status_group.addChild(iron_status);
        rubber_status = new StatusIcon(label_width, icons.getRubberStatusIcon(), i18n("chicken_resources_tip"));
        status_group.addChild(rubber_status);
        unit_status.place();
        weapon_rock_status.place(unit_status, Placement.BOTTOM_MID);
        weapon_iron_status.place(weapon_rock_status, Placement.BOTTOM_MID);
        weapon_rubber_status.place(weapon_iron_status, Placement.BOTTOM_MID);
        tree_status.place(unit_status, Placement.LEFT_MID, 5);
        rock_status.place(tree_status, Placement.BOTTOM_MID);
        iron_status.place(rock_status, Placement.BOTTOM_MID);
        rubber_status.place(iron_status, Placement.BOTTOM_MID);
        status_group.compileCanvas(5, 5, 5, 5);

        quarters_unit_status = new WatchStatusIcon(label_width, race_icons.unitStatusIcon(), i18n("units_tip"));
        quarters_status_group.addChild(quarters_unit_status);
        quarters_unit_status.place();
        quarters_status_group.compileCanvas(5, 5, 5, 5);

        quarters_peon_button = new DeploySpinner(
                viewer, player_interface, race_icons.peonIcon(),
                i18n("deploy_peon_tip"),
                List.of(race_icons.unitStatusIcon()), GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC
        );
        quarters_group.addChild(quarters_peon_button);
        quarters_chieftain_button = new ChieftainButton(viewer, player_interface, race_icons.chieftainIcon());
        quarters_group.addChild(quarters_chieftain_button);
        bindAction(quarters_chieftain_button, QuartersActionController.class,
                QuartersActionController::executeTrainChieftain);
        var quarters_rally_point_button = new NonFocusIconButton(
                race_icons.rallyPointIcon(),
                GameAction.UNIT_SET_RALLY, () -> i18n("rally_point_tip", getBinding(GameAction.UNIT_SET_RALLY))
        );
        quarters_group.addChild(quarters_rally_point_button);
        bindAction(quarters_rally_point_button, QuartersActionController.class,
                QuartersActionController::executeSetRallyPoint);
        quarters_peon_button.place();
        quarters_chieftain_button.place(quarters_peon_button, Placement.BOTTOM_MID);
        quarters_rally_point_button.place(quarters_chieftain_button, Placement.BOTTOM_MID);
        quarters_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        harvest_button = new NonFocusIconButton(
                icons.getHarvestIcon(), GameAction.PROD_HARVEST,
                () -> i18n("gather_resources_tip", getBinding(GameAction.PROD_HARVEST))
        );
        harvest_button.setIconDisabler(() -> !viewer.getLocalPlayer().canHarvest());
        armory_group.addChild(harvest_button);
        bindAction(harvest_button, ArmoryActionController.class, a -> a.openSubmenu(SubmenuType.HARVEST));
        build_button = new NonFocusIconButton(
                race_icons.buildWeaponsIcon(), GameAction.PROD_WEAPONS,
                () -> i18n("produce_weapons_tip", getBinding(GameAction.PROD_WEAPONS))
        );
        build_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuildWeapons());
        armory_group.addChild(build_button);
        bindAction(build_button, ArmoryActionController.class, a -> a.openSubmenu(SubmenuType.WEAPONS));
        army_button = new NonFocusIconButton(
                race_icons.armyIcon(), GameAction.PROD_ARMY,
                () -> i18n("deploy_army_tip", getBinding(GameAction.PROD_ARMY))
        );
        army_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuildArmies());
        armory_group.addChild(army_button);
        bindAction(army_button, ArmoryActionController.class, a -> a.openSubmenu(SubmenuType.ARMY));
        transport_button = new NonFocusIconButton(
                race_icons.transportIcon(), GameAction.PROD_TRANSPORT,
                () -> i18n("transport_resources_tip", getBinding(GameAction.PROD_TRANSPORT))
        );
        armory_group.addChild(transport_button);
        bindAction(transport_button, ArmoryActionController.class, a -> a.openSubmenu(SubmenuType.TRANSPORT));
        var rally_point_button = new NonFocusIconButton(
                race_icons.rallyPointIcon(), GameAction.UNIT_SET_RALLY,
                () -> i18n("rally_point_tip", getBinding(GameAction.UNIT_SET_RALLY))
        );
        rally_point_button.setIconDisabler(() -> !viewer.getLocalPlayer().canSetRallyPoints());
        armory_group.addChild(rally_point_button);
        bindAction(rally_point_button, ArmoryActionController.class, ArmoryActionController::executeSetRallyPoint);
        harvest_button.place();
        build_button.place(harvest_button, Placement.BOTTOM_MID);
        army_button.place(build_button, Placement.BOTTOM_MID);
        transport_button.place(army_button, Placement.BOTTOM_MID);
        rally_point_button.place(transport_button, Placement.BOTTOM_MID);
        armory_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        harvest_tree_button = new DeploySpinner(
                viewer, player_interface, icons.getTreeIcon(),
                i18n("harvest_tree_tip"), List.of(race_icons.unitStatusIcon()),
                GameAction.RES_TREE, GameAction.RES_TREE_DEC
        );
        harvest_group.addChild(harvest_tree_button);
        harvest_rock_button = new DeploySpinner(
                viewer, player_interface, icons.getRockIcon(),
                i18n("harvest_rock_tip"), List.of(race_icons.unitStatusIcon()),
                GameAction.RES_ROCK, GameAction.RES_ROCK_DEC
        );
        harvest_group.addChild(harvest_rock_button);
        harvest_iron_button = new DeploySpinner(
                viewer, player_interface, icons.getIronIcon(),
                i18n("harvest_iron_tip"), List.of(race_icons.unitStatusIcon()),
                GameAction.RES_IRON, GameAction.RES_IRON_DEC
        );
        harvest_group.addChild(harvest_iron_button);
        harvest_rubber_button = new DeploySpinner(
                viewer, player_interface, icons.getRubberIcon(), i18n(
                        "harvest_chicken_tip"), List.of(race_icons.unitStatusIcon()),
                GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC
        );
        harvest_group.addChild(harvest_rubber_button);
        var harvest_back_button = new NonFocusIconButton(
                skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK))
        );
        bindAction(harvest_back_button, controllerStack::pop);
        harvest_group.addChild(harvest_back_button);
        harvest_tree_button.place();
        harvest_rock_button.place(harvest_tree_button, Placement.BOTTOM_MID);
        harvest_iron_button.place(harvest_rock_button, Placement.BOTTOM_MID);
        harvest_rubber_button.place(harvest_iron_button, Placement.BOTTOM_MID);
        harvest_back_button.place(harvest_rubber_button, Placement.BOTTOM_MID);
        harvest_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        build_weapon_rock_button = new BuildSpinner(
                viewer, player_interface, race_icons.buildWeaponRockIcon(),
                i18n("build_rock_tip"), GUIIcons.toIconList(Building.COST_ROCK_WEAPON),
                GameAction.RES_ROCK, GameAction.RES_ROCK_DEC
        );
        build_group.addChild(build_weapon_rock_button);
        build_weapon_iron_button = new BuildSpinner(
                viewer, player_interface, race_icons.buildWeaponIronIcon(),
                i18n("build_iron_tip"), GUIIcons.toIconList(Building.COST_IRON_WEAPON),
                GameAction.RES_IRON, GameAction.RES_IRON_DEC
        );
        build_group.addChild(build_weapon_iron_button);
        build_weapon_rubber_button = new BuildSpinner(
                viewer, player_interface, race_icons.buildWeaponRubberIcon(),
                i18n("build_chicken_tip"), GUIIcons.toIconList(Building.COST_RUBBER_WEAPON),
                GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC
        );
        build_group.addChild(build_weapon_rubber_button);
        var build_back_button = new NonFocusIconButton(
                skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK))
        );
        bindAction(build_back_button, controllerStack::pop);
        build_group.addChild(build_back_button);
        build_weapon_rock_button.place();
        build_weapon_iron_button.place(build_weapon_rock_button, Placement.BOTTOM_MID);
        build_weapon_rubber_button.place(build_weapon_iron_button, Placement.BOTTOM_MID);
        build_back_button.place(build_weapon_rubber_button, Placement.BOTTOM_MID);
        build_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        army_peon_button = new DeploySpinner(
                viewer, player_interface, race_icons.peonIcon(),
                i18n("deploy_peon_tip"), List.of(race_icons.unitStatusIcon()),
                GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC
        );
        army_group.addChild(army_peon_button);
        army_warrior_rock_button = new DeploySpinner(
                viewer, player_interface, race_icons.warriorRockIcon(),
                i18n("deploy_rock_tip"), List.of(race_icons.unitStatusIcon(), race_icons.weaponRockStatusIcon()),
                GameAction.RES_ROCK, GameAction.RES_ROCK_DEC
        );
        army_group.addChild(army_warrior_rock_button);

        army_warrior_iron_button = new DeploySpinner(
                viewer, player_interface, race_icons.warriorIronIcon(),
                i18n("deploy_iron_tip"), List.of(race_icons.unitStatusIcon(), race_icons.weaponIronStatusIcon()),
                GameAction.RES_IRON, GameAction.RES_IRON_DEC
        );
        army_group.addChild(army_warrior_iron_button);

        army_warrior_rubber_button = new DeploySpinner(
                viewer, player_interface, race_icons.warriorRubberIcon(),
                i18n("deploy_chicken_tip"), List.of(race_icons.unitStatusIcon(), race_icons.weaponRubberStatusIcon()),
                GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC
        );
        army_group.addChild(army_warrior_rubber_button);

        var army_back_button = new NonFocusIconButton(
                skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK))
        );
        bindAction(army_back_button, controllerStack::pop);
        army_group.addChild(army_back_button);
        army_peon_button.place();
        army_warrior_rock_button.place(army_peon_button, Placement.BOTTOM_MID);
        army_warrior_iron_button.place(army_warrior_rock_button, Placement.BOTTOM_MID);
        army_warrior_rubber_button.place(army_warrior_iron_button, Placement.BOTTOM_MID);
        army_back_button.place(army_warrior_rubber_button, Placement.BOTTOM_MID);
        army_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        transport_tree_button = new DeploySpinner(
                viewer, player_interface, icons.getTreeIcon(),
                i18n("transport_tree_tip"), List.of(race_icons.unitStatusIcon(), icons.getTreeStatusIcon()),
                GameAction.RES_TREE, GameAction.RES_TREE_DEC
        );
        transport_group.addChild(transport_tree_button);
        transport_rock_button = new DeploySpinner(
                viewer, player_interface, icons.getRockIcon(),
                i18n("transport_rock_tip"), List.of(race_icons.unitStatusIcon(), icons.getRockStatusIcon()),
                GameAction.RES_ROCK, GameAction.RES_ROCK_DEC
        );
        transport_group.addChild(transport_rock_button);
        transport_iron_button = new DeploySpinner(
                viewer, player_interface, icons.getIronIcon(),
                i18n("transport_iron_tip"), List.of(race_icons.unitStatusIcon(), icons.getIronStatusIcon()),
                GameAction.RES_IRON, GameAction.RES_IRON_DEC
        );
        transport_group.addChild(transport_iron_button);
        transport_rubber_button = new DeploySpinner(
                viewer, player_interface, icons.getRubberIcon(),
                i18n("transport_chicken_tip"), List.of(race_icons.unitStatusIcon(), icons.getRubberStatusIcon()),
                GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC
        );
        transport_group.addChild(transport_rubber_button);
        var transport_back_button = new NonFocusIconButton(
                skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK))
        );
        bindAction(transport_back_button, controllerStack::pop);
        transport_group.addChild(transport_back_button);
        transport_tree_button.place();
        transport_rock_button.place(transport_tree_button, Placement.BOTTOM_MID);
        transport_iron_button.place(transport_rock_button, Placement.BOTTOM_MID);
        transport_rubber_button.place(transport_iron_button, Placement.BOTTOM_MID);
        transport_back_button.place(transport_rubber_button, Placement.BOTTOM_MID);
        transport_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        setCanFocus(true);
        displayChangedNotify(width, height);
    }

    public static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private String getBinding(GameAction action) {
        return viewer.getInputManager().getBindingString(action);
    }

    private String getMagicTooltip(MagicType magicType) {
        return switch (magicType) {
            case STUN -> i18n("terrifying_toot", getBinding(GameAction.MAGIC_1));
            case SONIC_BLAST -> i18n("ravaging_roar", getBinding(GameAction.MAGIC_2));
            case POISON_FOG -> i18n("stinking_stew", getBinding(GameAction.MAGIC_1));
            case LIGHTNING_CLOUD -> i18n("crackling_cloud", getBinding(GameAction.MAGIC_2));
        };
    }

    @Override
    public void doAdd() {
        super.doAdd();
        viewer.getAnimationManagerLocal().registerAnimation(this);
        GUIRoot root = getParentGUIRoot();
        if (root != null) {
            displayChangedNotify(root.getWidth(), root.getHeight());
        }
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        viewer.getAnimationManagerLocal().removeAnimation(this);
    }

    @Override
    public void animate(float t) {
        Building new_building = viewer.getSelection().getCurrentSelection().getBuilding().orElse(null);
        boolean different_building = new_building != current_building;
        current_building = new_building;
        viewer.getRenderer().setSelectedBuilding(new_building);

        Unit new_chieftain = viewer.getSelection().getCurrentSelection().getChieftain().orElse(null);
        boolean different_chieftain = new_chieftain != current_chieftain;
        current_chieftain = new_chieftain;

        int current_num_units = viewer.getSelection().getCurrentSelection().getNumUnits();
        int current_num_peons = viewer.getSelection().getCurrentSelection().getNumBuilders();

        boolean new_quarters = current_building != null && current_building.getAbilities().hasAbilities(
                Abilities.REPRODUCE);
        boolean new_armory = current_building != null && current_building.getAbilities().hasAbilities(
                Abilities.BUILD_ARMIES);
        boolean new_unit = current_num_units > 0;
        boolean new_peon = current_num_peons > 0;
        boolean new_tower = current_building != null && current_building.getAbilities().hasAbilities(Abilities.ATTACK);
        boolean selection_changed = different_building || different_chieftain || new_quarters != current_quarters
                || new_armory != current_armory || new_unit != current_unit || new_peon != current_peon || new_tower
                        != current_tower;
        if (selection_changed) {
            current_quarters = new_quarters;
            current_armory = new_armory;
            current_tower = new_tower;
            current_unit = new_unit;
            current_peon = new_peon;
            update = false;

            if (current_unit) {
                controllerStack.setRoot(new UnitActionController(this, current_peon, current_chieftain));
            } else if (current_tower && current_building != null) {
                controllerStack.setRoot(new TowerActionController(this, current_building));
            } else if (current_quarters && current_building != null) {
                controllerStack.setRoot(new QuartersActionController(this, current_building));
            } else if (current_armory && current_building != null) {
                controllerStack.setRoot(new ArmoryActionController(this, current_building, controllerStack));
            } else {
                controllerStack.clear();
            }
        } else if (update) {
            update = false;
            onControllerChanged(controllerStack.getActiveController());
        }
        updateButtons();
    }

    private void onControllerChanged(@Nullable ActionController active) {
        removeGroups();

        if (active instanceof UnitActionController u) {
            addChild(unit_group);
            if (u.isPeon()) {
                addChild(peon_group);
            }
            Unit chieftain = u.getChieftain();
            if (chieftain != null) {
                addChild(chieftain_group);
                updateGroups();
                Player player = viewer.getLocalPlayer();
                if (player.canDoMagic(0)) {
                    magic1_button.setUnit(chieftain);
                    magic1_button.setIconDisabler(() -> !chieftain.canDoMagic(0));
                    chieftain_group.addChild(magic1_button);
                } else {
                    magic1_button.remove();
                }
                if (player.canDoMagic(1)) {
                    magic2_button.setUnit(chieftain);
                    magic2_button.setIconDisabler(() -> !chieftain.canDoMagic(1));
                    chieftain_group.addChild(magic2_button);
                } else {
                    magic2_button.remove();
                }
            }
        } else if (active instanceof TowerActionController t) {
            addChild(tower_group);
            Building b = t.getBuilding();
            tower_attack_button.setIconDisabler(() -> !b.getAbilities().hasAbilities(Abilities.ATTACK));
            tower_exit_button.setIconDisabler(() -> !b.canExitTower());
        } else if (active instanceof QuartersActionController q) {
            addChild(quarters_status_group);
            addChild(quarters_group);
            Building b = q.getBuilding();
            SupplyCounter unit_counter = new SupplyCounter(b, Unit.class);
            quarters_unit_status.setCounter(unit_counter);
            quarters_unit_status.setUnitContainerBuilding(b);
            quarters_peon_button.setContainers(b, DeployType.PEON, null);
            quarters_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
            quarters_chieftain_button.setIconDisabler(() -> !b.canBuildChieftain() && !b.canStopChieftain());
            quarters_chieftain_button.setBuilding(b);
        } else if (active instanceof ArmoryActionController) {
            addChild(status_group);
            addChild(armory_group);
            if (viewer.getLocalPlayer().canUseRubber()) {
                build_group.addChild(build_weapon_rubber_button);
                army_group.addChild(army_warrior_rubber_button);
            } else {
                build_weapon_rubber_button.remove();
                army_warrior_rubber_button.remove();
            }
            updateCounters();
        } else if (active instanceof ArmorySubmenuController s) {
            addChild(status_group);
            Group group = switch (s.type()) {
                case HARVEST -> harvest_group;
                case WEAPONS -> build_group;
                case ARMY -> army_group;
                case TRANSPORT -> transport_group;
            };
            addChild(group);
            current_submenu = group;
            if (viewer.getLocalPlayer().canUseRubber()) {
                build_group.addChild(build_weapon_rubber_button);
                army_group.addChild(army_warrior_rubber_button);
            } else {
                build_weapon_rubber_button.remove();
                army_warrior_rubber_button.remove();
            }
            updateCounters();
        }
        updateGroups();
    }

    private void updateButtons() {
        if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.ATTACK)) {
            tower_attack_button.doUpdate();
            tower_exit_button.doUpdate();
        } else if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
            unit_status.doUpdate();
            weapon_rock_status.doUpdate();
            weapon_iron_status.doUpdate();
            weapon_rubber_status.doUpdate();
            tree_status.doUpdate();
            rock_status.doUpdate();
            iron_status.doUpdate();
            rubber_status.doUpdate();

            harvest_button.doUpdate();
            build_button.doUpdate();
            army_button.doUpdate();

            harvest_tree_button.doUpdate();
            harvest_rock_button.doUpdate();
            harvest_iron_button.doUpdate();
            harvest_rubber_button.doUpdate();
            build_weapon_rock_button.doUpdate();
            build_weapon_iron_button.doUpdate();
            build_weapon_rubber_button.doUpdate();
            army_warrior_rubber_button.doUpdate();
            army_warrior_iron_button.doUpdate();
            army_warrior_rock_button.doUpdate();
            army_peon_button.doUpdate();
            transport_tree_button.doUpdate();
            transport_rock_button.doUpdate();
            transport_iron_button.doUpdate();
            transport_rubber_button.doUpdate();
        } else if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            quarters_unit_status.doUpdate();

            quarters_peon_button.doUpdate();
            quarters_chieftain_button.doUpdate();
        } else if (current_peon) {
            quarters_button.doUpdate();
            armory_button.doUpdate();
            tower_button.doUpdate();
        }
        if (current_unit) {
            move_button.doUpdate();
            attack_button.doUpdate();
            gather_repair_button.doUpdate();
        }
        if (current_chieftain != null) {
            magic1_button.doUpdate();
            magic2_button.doUpdate();
        }
    }

    private void removeGroups() {
        unit_group.remove();
        peon_group.remove();
        chieftain_group.remove();
        tower_group.remove();
        quarters_status_group.remove();
        quarters_group.remove();
        status_group.remove();
        armory_group.remove();
        harvest_group.remove();
        build_group.remove();
        army_group.remove();
        transport_group.remove();
        current_submenu = null;
    }

    private void updateCounters() {
        assert current_building != null : "Building is null";
        SupplyCounter unit_counter = new SupplyCounter(current_building, Unit.class);
        unit_status.setCounter(unit_counter);
        SupplyCounter weapon_rock_counter = new SupplyCounter(current_building, RockAxeWeapon.class);
        weapon_rock_status.setCounter(weapon_rock_counter);
        SupplyCounter weapon_iron_counter = new SupplyCounter(current_building, IronAxeWeapon.class);
        weapon_iron_status.setCounter(weapon_iron_counter);
        SupplyCounter weapon_rubber_counter = new SupplyCounter(current_building, RubberAxeWeapon.class);
        weapon_rubber_status.setCounter(weapon_rubber_counter);
        SupplyCounter tree_counter = new SupplyCounter(current_building, TreeSupply.class);
        tree_status.setCounter(tree_counter);
        SupplyCounter rock_counter = new SupplyCounter(current_building, RockSupply.class);
        rock_status.setCounter(rock_counter);
        SupplyCounter iron_counter = new SupplyCounter(current_building, IronSupply.class);
        iron_status.setCounter(iron_counter);
        SupplyCounter rubber_counter = new SupplyCounter(current_building, RubberSupply.class);
        rubber_status.setCounter(rubber_counter);

        harvest_tree_button.setContainers(current_building, DeployType.PEON_HARVEST_TREE, null);
        harvest_tree_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        harvest_rock_button.setContainers(current_building, DeployType.PEON_HARVEST_ROCK, null);
        harvest_rock_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        harvest_iron_button.setContainers(current_building, DeployType.PEON_HARVEST_IRON, null);
        harvest_iron_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        harvest_rubber_button.setContainers(current_building, DeployType.PEON_HARVEST_RUBBER, null);
        harvest_rubber_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);

        build_weapon_rock_button.setBuildSupplyContainer(current_building, RockAxeWeapon.class);
        build_weapon_iron_button.setBuildSupplyContainer(current_building, IronAxeWeapon.class);
        build_weapon_rubber_button.setBuildSupplyContainer(current_building, RubberAxeWeapon.class);

        army_peon_button.setContainers(current_building, DeployType.PEON, null);
        army_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        army_warrior_rock_button.setContainers(current_building, DeployType.ROCK_WARRIOR, RockAxeWeapon.class);
        army_warrior_rock_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rock_counter));
        army_warrior_iron_button.setContainers(current_building, DeployType.IRON_WARRIOR, IronAxeWeapon.class);
        army_warrior_iron_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_iron_counter));
        army_warrior_rubber_button.setContainers(current_building, DeployType.RUBBER_WARRIOR, RubberAxeWeapon.class);
        army_warrior_rubber_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rubber_counter));

        transport_tree_button.setContainers(current_building, DeployType.PEON_TRANSPORT_TREE, TreeSupply.class);
        transport_tree_button.setIconDisabler(() -> suppliesEmpty(unit_counter, tree_counter));
        transport_rock_button.setContainers(current_building, DeployType.PEON_TRANSPORT_ROCK, RockSupply.class);
        transport_rock_button.setIconDisabler(() -> suppliesEmpty(unit_counter, rock_counter));
        transport_iron_button.setContainers(current_building, DeployType.PEON_TRANSPORT_IRON, IronSupply.class);
        transport_iron_button.setIconDisabler(() -> suppliesEmpty(unit_counter, iron_counter));
        transport_rubber_button.setContainers(current_building, DeployType.PEON_TRANSPORT_RUBBER, RubberSupply.class);
        transport_rubber_button.setIconDisabler(() -> suppliesEmpty(unit_counter, rubber_counter));
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);
        updateGroups();
    }

    private void updateGroups() {
        int width = getWidth();
        int height = getHeight();
        unit_group.setPos(width - unit_group.getWidth(), height - unit_group.getHeight());
        peon_group.setPos(width - peon_group.getWidth(), unit_group.getY() - peon_group.getHeight());
        if (current_peon)
            chieftain_group.setPos(width - chieftain_group.getWidth(), peon_group.getY() - chieftain_group.getHeight());
        else
            chieftain_group.setPos(width - chieftain_group.getWidth(), unit_group.getY() - chieftain_group.getHeight());
        tower_group.setPos(width - tower_group.getWidth(), height - tower_group.getHeight());
        quarters_status_group.setPos(
                width - quarters_status_group.getWidth(), height - quarters_status_group
                        .getHeight()
        );
        quarters_group.setPos(
                width - quarters_group.getWidth(), quarters_status_group.getY() - quarters_group
                        .getHeight()
        );
        status_group.setPos(width - status_group.getWidth(), height - status_group.getHeight());
        armory_group.setPos(width - armory_group.getWidth(), status_group.getY() - armory_group.getHeight());
        harvest_group.setPos(width - harvest_group.getWidth(), status_group.getY() - harvest_group.getHeight());
        build_group.setPos(width - build_group.getWidth(), status_group.getY() - build_group.getHeight());
        army_group.setPos(width - army_group.getWidth(), status_group.getY() - army_group.getHeight());
        transport_group.setPos(width - transport_group.getWidth(), status_group.getY() - transport_group.getHeight());
    }

    @Override
    public void handleInput(InputEvent event) {
        controllerStack.handleInput(event);
    }

    @Override
    public boolean canHoverBehind() {
        return true;
    }

    public boolean inHarvestMenu() {
        return controllerStack.getActiveController() instanceof ArmorySubmenuController s && s.type()
                == SubmenuType.HARVEST;
    }

    public boolean inBuildMenu() {
        return controllerStack.getActiveController() instanceof ArmorySubmenuController s && s.type()
                == SubmenuType.WEAPONS;
    }

    public boolean inArmyMenu() {
        return controllerStack.getActiveController() instanceof ArmorySubmenuController s && s.type()
                == SubmenuType.ARMY;
    }

    public boolean inTransportMenu() {
        return controllerStack.getActiveController() instanceof ArmorySubmenuController s && s.type()
                == SubmenuType.TRANSPORT;
    }

    @Override
    public void mouseDragged(
            MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x,
            int absolute_y
    ) {
        if (getParent() != null)
            getParent().mouseDraggedAll(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
    }

    @Override
    public void pushDelegate(CameraDelegate<?> delegate) {
        var root = viewer.getGUIRoot();
        var current = root.getDelegate();
        if (current instanceof TargetDelegate || current instanceof PlacingDelegate
                || current instanceof RallyPointDelegate) {
            root.removeDelegate(current);
        }
        root.pushDelegate(delegate);
    }

    @Override
    public WorldViewer getViewer() {
        return viewer;
    }

    @Override
    public GameCamera getCamera() {
        return camera;
    }

    @Override
    public void adjustQuartersPeon(boolean pressed, boolean decrement, boolean batch) {
        if (pressed) {
            quarters_peon_button.shortcutPressed(decrement, batch);
        } else {
            quarters_peon_button.shortcutReleased(decrement, batch);
        }
    }

    @Override
    public void adjustArmorySupply(SubmenuType type, SupplyType supply, boolean pressed, boolean decrement,
            boolean batch) {
        IconSpinner target = switch (type) {
            case HARVEST -> switch (supply) {
                case WOOD -> harvest_tree_button;
                case ROCK -> harvest_rock_button;
                case IRON -> harvest_iron_button;
                case RUBBER -> harvest_rubber_button;
            };
            case WEAPONS -> switch (supply) {
                case ROCK -> build_weapon_rock_button;
                case IRON -> build_weapon_iron_button;
                case RUBBER -> build_weapon_rubber_button;
                case WOOD -> null;
            };
            case ARMY -> switch (supply) {
                case ROCK -> army_warrior_rock_button;
                case IRON -> army_warrior_iron_button;
                case RUBBER -> army_warrior_rubber_button;
                case WOOD -> null;
            };
            case TRANSPORT -> switch (supply) {
                case WOOD -> transport_tree_button;
                case ROCK -> transport_rock_button;
                case IRON -> transport_iron_button;
                case RUBBER -> transport_rubber_button;
            };
        };

        if (target != null) {
            if (pressed) {
                target.shortcutPressed(decrement, batch);
            } else {
                target.shortcutReleased(decrement, batch);
            }
        }
    }

    @Override
    public void adjustArmoryPeon(boolean pressed, boolean decrement, boolean batch) {
        if (pressed) {
            army_peon_button.shortcutPressed(decrement, batch);
        } else {
            army_peon_button.shortcutReleased(decrement, batch);
        }
    }

    @Override
    public void markNeedsUpdate() {
        removeGroups();
        update = true;
    }

    public ActionControllerStack getControllerStack() {
        return controllerStack;
    }

    /**
     * Closes the active armory submenu if open, consuming cancel actions to avoid triggering the pause menu.
     */
    public boolean tryCloseSubmenu(InputEvent event) {
        if (controllerStack.getActiveController() instanceof ArmorySubmenuController) {
            event.consumeAction(GameAction.GLOBAL_MENU);
            event.consumeAction(GameAction.UI_CANCEL);
            controllerStack.pop();
            return true;
        }
        return false;
    }

    private <T extends ActionController> void bindAction(
            ButtonObject button, Class<T> controllerClass, Consumer<T> action) {
        button.addClickListener(controllerStack.bind(controllerClass, action));
    }

    private void bindAction(ButtonObject button, Runnable action) {
        button.addClickListener(action);
    }

    private boolean suppliesEmpty(SupplyCounter... counters) {
        return Arrays.stream(counters).anyMatch(c -> c.getNumSupplies() == 0);
    }
}
