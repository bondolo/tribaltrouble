package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.simulation.model.Army;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class Selection {
    private final @Nullable Army[] shortcut_armies = new Army[10];
    private final Player local_player;
    private SelectionArmy current_selection;

    public Selection(Player local_player) {
        this.local_player = local_player;
        clearSelection();
    }

    public SelectionArmy getCurrentSelection() {
        return current_selection;
    }

    public void clearSelection() {
        current_selection = new SelectionArmy(local_player);
    }

    public void clearShortcutArmies() {
        Arrays.fill(shortcut_armies, null);
    }

    void removeFromArmies(Selectable<?> selectable) {
        current_selection.remove(selectable);
        for (Army shortcut_armie : shortcut_armies) {
            if (shortcut_armie != null) {
                shortcut_armie.remove(selectable);
            }
        }
    }

    public void setShortcutArmy(int index) {
        if (shortcut_armies[index] != null)
            shortcut_armies[index].clear();
        else
            shortcut_armies[index] = new Army();

        shortcut_armies[index].addAll(current_selection.getSet());
    }

    public boolean enableShortcutArmy(int index) {
        if (shortcut_armies[index] != null) {
            var set = shortcut_armies[index].getSet();
            current_selection.clear();
            if (!set.isEmpty()) {
                current_selection.addAll(set);
                return true;
            }
        }
        return false;
    }
}
