package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.engine.render.HeightMapVisual;
import com.oddlabs.tt.engine.render.VisualModel;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.LightningCloud;
import com.oddlabs.tt.simulation.model.weapon.PoisonFog;
import com.oddlabs.tt.simulation.model.weapon.SonicBlast;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import com.oddlabs.tt.simulation.model.weapon.ThrowingWeapon;


/**
 * Initializes client-side visual model and heightmap factories for the simulation layer.
 */
public final class ClientStateInitializer {
    private ClientStateInitializer() {
    }

    /**
     * Registers client state factories for simulation models and heightmaps.
     *
     * @param audio the active audio implementation
     */
    public static void init(AudioImplementation audio) {
        Model.setClientStateFactory(model -> {
            VisualModel visualModel = new VisualModel(model, audio);
            switch (model) {
                case Unit unit -> {
                    if (unit.getAbilities().hasAbilities(Abilities.BUILD)) {
                        visualModel.getAccessories().add(new CarriedResourceAccessory(unit));
                    }
                }
                case Building building -> {
                    float hitOffsetZ = building.getHitOffsetZ();
                    visualModel.getAccessories().add(new BuildingDamagedAccessory(building, hitOffsetZ, audio));
                    visualModel.getAccessories().add(new BuildingProductionAccessory(building, audio));
                }
                case IronSupply ironSupply ->
                    visualModel.getAccessories().add(new IronSupplyVisualAccessory(ironSupply, audio));
                case RockSupply rockSupply ->
                    visualModel.getAccessories().add(new RockSupplyVisualAccessory(rockSupply, audio));
                case LightningCloud cloud ->
                    visualModel.getAccessories().add(new LightningCloudVisualAccessory(cloud, audio));
                case PoisonFog fog ->
                    visualModel.getAccessories().add(new PoisonFogVisualAccessory(fog, audio));
                case Stun stun ->
                    visualModel.getAccessories().add(new StunVisualAccessory(stun, audio));
                case SonicBlast blast ->
                    visualModel.getAccessories().add(new SonicBlastVisualAccessory(blast, audio));
                case ThrowingWeapon throwingWeapon ->
                    visualModel.getAccessories().add(new ThrowingWeaponVisualAccessory(throwingWeapon, audio));
                default -> {
                }
            }
            return visualModel;
        });
        HeightMap.setClientStateFactory(HeightMapVisual::new);
        EditLine.setErrorAudioHandler(
                () -> audio.newAudio(0f, 0f, 0f, AudioAssets.ERROR_SOUND));
    }
}
