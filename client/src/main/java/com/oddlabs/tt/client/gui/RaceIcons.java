package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;

/**
 * UI icon collection specific to a playable race.
 */
public record RaceIcons(IconQuad unitStatusIcon,
                        IconQuad weaponRockStatusIcon,
                        IconQuad weaponIronStatusIcon,
                        IconQuad weaponRubberStatusIcon,
                        ModeIconQuads buildWeaponsIcon,
                        ModeIconQuads buildWeaponRockIcon,
                        ModeIconQuads buildWeaponIronIcon,
                        ModeIconQuads buildWeaponRubberIcon,
                        ModeIconQuads armyIcon,
                        ModeIconQuads warriorRockIcon,
                        ModeIconQuads warriorIronIcon,
                        ModeIconQuads warriorRubberIcon,
                        ModeIconQuads peonIcon,
                        ModeIconQuads chieftainIcon,
                        ModeIconQuads transportIcon,
                        ModeIconQuads attackIcon,
                        ModeIconQuads moveIcon,
                        ModeIconQuads gatherRepairIcon,
                        ModeIconQuads quartersIcon,
                        ModeIconQuads armoryIcon,
                        ModeIconQuads towerIcon,
                        ModeIconQuads towerExitIcon,
                        ModeIconQuads rallyPointIcon,
                        ModeIconQuads magic1Icon,
                        ModeIconQuads magic2Icon) {
}
