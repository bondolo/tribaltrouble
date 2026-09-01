package com.oddlabs.tt.simulation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RaceData}.
 */
class RaceDataTest {

    @Test
    void testHeadlessRaceData() {
        RaceData raceData = new RaceData();
        assertNotNull(raceData);

        for (Race race : Race.values()) {
            RaceInfo raceInfo = raceData.getRaceInfo(race);
            assertNotNull(raceInfo, "RaceInfo missing for " + race);
            assertEquals(race, raceInfo.getRaceType());
            assertNotNull(raceInfo.getChieftainAI());

            // Validate buildings
            for (BuildingType buildingType : BuildingType.values()) {
                BuildingTemplate buildingTemplate = raceInfo.getBuildingTemplate(buildingType);
                assertNotNull(buildingTemplate, "BuildingTemplate missing for " + buildingType);
                assertEquals(buildingType, buildingTemplate.getBuildingType());
                assertTrue(buildingTemplate.getMaxHitPoints() > 0);
                assertNotNull(buildingTemplate.getUnitContainerFactory());
                assertNotNull(buildingTemplate.getBuiltBounds());
                assertTrue(buildingTemplate.getBuiltBounds().length > 0);
                assertNotNull(buildingTemplate.getHalfbuiltBounds());
                assertTrue(buildingTemplate.getHalfbuiltBounds().length > 0);
                assertNotNull(buildingTemplate.getStartBounds());
                assertTrue(buildingTemplate.getStartBounds().length > 0);
            }

            // Validate units
            for (UnitType unitType : UnitType.values()) {
                UnitTemplate unitTemplate = raceInfo.getUnitTemplate(unitType);
                assertNotNull(unitTemplate, "UnitTemplate missing for " + unitType);
                assertTrue(unitTemplate.getMaxHitPoints() > 0);
                assertTrue(unitTemplate.getMetersPerSecond() > 0);
                assertNotNull(unitTemplate.getBounds());
                assertTrue(unitTemplate.getBounds().length > 0);
                assertNotNull(unitTemplate.getAnimationType(Unit.Animation.IDLING));
                assertNotNull(unitTemplate.getAnimationType(Unit.Animation.MOVING));
                assertNotNull(unitTemplate.getAnimationType(Unit.Animation.DYING));
            }

            // Validate magics
            assertFalse(raceInfo.getMagics().isEmpty());
            for (MagicType magicType : raceInfo.getMagics()) {
                assertNotNull(raceInfo.getMagicFactory(magicType));
            }
        }
    }

    @Test
    void testRaceNames() {
        assertEquals("Natives", RaceData.getRaceName(Race.NATIVES));
        assertEquals("Vikings", RaceData.getRaceName(Race.VIKINGS));
    }
}
