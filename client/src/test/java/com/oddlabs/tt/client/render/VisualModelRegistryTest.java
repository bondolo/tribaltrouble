package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class VisualModelRegistryTest {

    private World world;

    @BeforeEach
    void setUp() {
        int gridSize = 32;
        Landscape landscape = FlatLandscape.create(gridSize);
        WorldParameters worldParams = new WorldParameters("1", 5, 20);
        List<PlayerInfo> playerInfos = List.of(new PlayerInfo(0, Race.VIKINGS, "Player"));
        Color.Linear[] playerColors = new Color.Linear[]{new Color.Linear(1f, 0f, 0f, 1f)};
        this.world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
    }

    private static AudioImplementation createDummyAudio() {
        return (AudioImplementation) Proxy.newProxyInstance(
                AudioImplementation.class.getClassLoader(),
                new Class<?>[]{AudioImplementation.class},
                (_, _, _) -> null
        );
    }

    private static class DummyModel extends Model {
        DummyModel(World world) {
            super(world);
        }

        @Override
        protected BoundingBox @Nullable [] getLocalBounds() {
            return null;
        }
    }

    private static final class SubDummyModel extends DummyModel {
        SubDummyModel(World world) {
            super(world);
        }
    }

    @Test
    void defaultRegistryResolvesDynamicVisualModelForUnknownModel() {
        AudioImplementation audio = createDummyAudio();
        DummyModel model = new DummyModel(world);

        VisualModel vm = VisualModelRegistry.getDefault().createVisualModel(model, audio);
        assertNotNull(vm);
        assertInstanceOf(DynamicVisualModel.class, vm);
        assertSame(model, vm.getModel());
    }

    @Test
    void registryResolvesSubclassViaHierarchyFallback() {
        AudioImplementation audio = createDummyAudio();
        SubDummyModel subModel = new SubDummyModel(world);

        VisualModelRegistry registry = VisualModelRegistry.createDefaultRegistry();
        DynamicVisualModel customModel = new DynamicVisualModel(subModel);
        registry.register(DummyModel.class, (m, a) -> customModel);

        VisualModel resolved = registry.createVisualModel(subModel, audio);
        assertSame(customModel, resolved);
    }

    @Test
    void customRegistrationTakesPrecedence() {
        AudioImplementation audio = createDummyAudio();
        DummyModel model = new DummyModel(world);

        VisualModelRegistry registry = VisualModelRegistry.createDefaultRegistry();
        DynamicVisualModel customModel = new DynamicVisualModel(model);
        registry.register(DummyModel.class, (m, a) -> customModel);

        VisualModel resolved = registry.createVisualModel(model, audio);
        assertSame(customModel, resolved);
    }

    @Test
    void modelDirectClientModelBinding() {
        AudioImplementation audio = createDummyAudio();
        DummyModel model = new DummyModel(world);
        assertNull(model.getClientModel());

        VisualModel vm = VisualModelRegistry.getDefault().createVisualModel(model, audio);
        model.setClientModel(vm);
        assertSame(vm, model.getClientModel());

        model.setClientModel(null);
        assertNull(model.getClientModel());
    }
}
