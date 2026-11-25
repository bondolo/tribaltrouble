package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.tt.simulation.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.LightningCloud;
import com.oddlabs.tt.simulation.model.weapon.PoisonFog;
import com.oddlabs.tt.simulation.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.SonicBlast;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Factory resolving concrete asset keys and creating {@link VisualModel} instances for simulation models.
final class VisualModelFactory {
  private final AudioImplementation audio;
  private final AssetRegistry assets;

  VisualModelFactory(AudioImplementation audio, AssetRegistry assets) {
    this.audio = audio;
    this.assets = assets;
  }

  VisualModel create(Model model) {
    return switch (model) {
      case Building building ->
          new BuildingVisualModel(
              building, audio, assets.getSmokeTextures(), assets.getDamageSmokeTextures(), assets.getWoodFragments());
      case Unit unit -> createUnitVisualModel(unit);
      case LightningCloud cloud ->
          new LightningCloudVisualModel(cloud, audio, assets.getSmokeTextures(), assets.getLightningTexture());
      case SonicBlast blast -> new SonicBlastVisualModel(blast, audio);
      case PoisonFog fog -> new PoisonFogVisualModel(fog, audio, assets.getPoisonTextures());
      case Stun stun -> new StunVisualModel(stun, audio, assets.getNoteTextures());
      case IronSupply iron ->
          new IronSupplyVisualModel(
              iron,
              audio,
              assets.getRockFragmentSprite(iron.getFragmentIndex()),
              assets.getIronFragmentSprite(iron.getFragmentIndex()),
              assets::getSmokeTextures);
      case RockSupply rock ->
          new RockSupplyVisualModel(
              rock, audio, assets.getRockFragmentSprite(rock.getFragmentIndex()), assets::getSmokeTextures);
      case SupplyModel supply -> new GenericSupplyVisualModel(supply, assets.getSupplySprite(supply));
      case DirectedThrowingWeapon spear -> new DirectedThrowingWeaponVisualModel(spear, audio);
      case RotatingThrowingWeapon axe -> new RotatingThrowingWeaponVisualModel(axe, audio);
      default -> new DynamicVisualModel(model);
    };
  }

  private UnitVisualModel createUnitVisualModel(Unit unit) {
    Race race = unit.getOwnerNoCheck().getRaceInfo().getRaceType();
    Map<SupplyType, SpriteKey> carriedSupplies =
        unit.getAbilities().hasAbilities(Abilities.BUILD) ? assets.getCarriedSupplySprites(race) : null;
    SpriteKey weaponSprite = resolveWeaponSprite(unit, race);
    return new UnitVisualModel(unit, carriedSupplies, weaponSprite, assets.getStarTextures());
  }

  private @Nullable SpriteKey resolveWeaponSprite(Unit unit, Race race) {
    if (!unit.getAbilities().hasAbilities(Abilities.THROW)) {
      return null;
    }
    WeaponTier tier = unit.getTemplate().getUnitType().getWeaponTier();
    return tier != null ? assets.getWeaponSprite(race, tier) : null;
  }
}
