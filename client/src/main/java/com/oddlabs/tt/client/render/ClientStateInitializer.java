package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.SupplyModel;
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
     * Creates and decorates a VisualModel instance for a simulation Model.
     *
     * @param model the simulation model
     * @param audio the active audio implementation
     * @return decorated VisualModel instance
     */
    public static VisualModel createVisualModel(Model model, AudioImplementation audio) {
        return switch (model) {
            case Building building -> new BuildingVisualModel(building, audio);
            case LightningCloud cloud -> new LightningCloudVisualModel(cloud, audio);
            case SonicBlast blast -> new SonicBlastVisualModel(blast, audio);
            case Unit unit -> new UnitVisualModel(unit);
            case IronSupply ironSupply -> new IronSupplyVisualModel(ironSupply, audio);
            case RockSupply rockSupply -> new RockSupplyVisualModel(rockSupply, audio);
            case SupplyModel supplyModel -> new GenericSupplyVisualModel(supplyModel);
            case PoisonFog fog -> new PoisonFogVisualModel(fog, audio);
            case Stun stun -> new StunVisualModel(stun, audio);
            case ThrowingWeapon throwingWeapon -> new ThrowingWeaponVisualModel(throwingWeapon, audio);
            default -> new DynamicVisualModel(model);
        };
    }
}
