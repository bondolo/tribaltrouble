package com.oddlabs.tt.simulation.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TurnCommandBufferTest {

  private TurnCommandBuffer buffer;

  @BeforeEach
  void setUp() {
    buffer = new TurnCommandBuffer();
  }

  @Test
  void testEmptyBuffer() {
    assertTrue(buffer.isEmpty());
    assertEquals(0, buffer.getScheduledTickCount());
    assertTrue(buffer.drainCommandsForTick(0).isEmpty());
    assertTrue(buffer.drainCommandsForTick(10).isEmpty());
  }

  @Test
  void testScheduleAndDrainSingleCommand() {
    PlayerCommand cmd = new PlayerCommand.SetPreferredGamespeed(2);
    buffer.schedule(5, 0, cmd);

    assertFalse(buffer.isEmpty());
    assertEquals(1, buffer.getScheduledTickCount());
    assertTrue(buffer.drainCommandsForTick(4).isEmpty());

    List<TurnCommandBuffer.ScheduledCommand> drained = buffer.drainCommandsForTick(5);
    assertEquals(1, drained.size());
    assertEquals(5, drained.getFirst().tick());
    assertEquals(0, drained.getFirst().playerSlot());
    assertEquals(cmd, drained.getFirst().command());

    assertTrue(buffer.drainCommandsForTick(5).isEmpty());
    assertTrue(buffer.isEmpty());
    assertEquals(0, buffer.getScheduledTickCount());
  }

  @Test
  void testScheduleMultipleCommandsSameTickPreservesOrder() {
    PlayerCommand cmd1 = new PlayerCommand.ChangePreferredGamespeed(1);
    PlayerCommand cmd2 = new PlayerCommand.ChangePreferredGamespeed(-1);
    PlayerCommand cmd3 = new PlayerCommand.SetPreferredGamespeed(3);

    buffer.schedule(10, 0, cmd1);
    buffer.schedule(10, 1, cmd2);
    buffer.schedule(10, 0, cmd3);

    assertEquals(1, buffer.getScheduledTickCount());
    List<TurnCommandBuffer.ScheduledCommand> drained = buffer.drainCommandsForTick(10);
    assertEquals(3, drained.size());
    assertEquals(cmd1, drained.get(0).command());
    assertEquals(0, drained.get(0).playerSlot());
    assertEquals(cmd2, drained.get(1).command());
    assertEquals(1, drained.get(1).playerSlot());
    assertEquals(cmd3, drained.get(2).command());
    assertEquals(0, drained.get(2).playerSlot());

    assertTrue(buffer.isEmpty());
  }

  @Test
  void testScheduleMultipleCommandsDifferentTicks() {
    PlayerCommand cmd1 = new PlayerCommand.SetPreferredGamespeed(1);
    PlayerCommand cmd2 = new PlayerCommand.SetPreferredGamespeed(2);
    PlayerCommand cmd3 = new PlayerCommand.SetPreferredGamespeed(3);

    buffer.schedule(1, 0, cmd1);
    buffer.schedule(3, 1, cmd2);
    buffer.schedule(2, 0, cmd3);

    assertEquals(3, buffer.getScheduledTickCount());

    List<TurnCommandBuffer.ScheduledCommand> tick1 = buffer.drainCommandsForTick(1);
    assertEquals(1, tick1.size());
    assertEquals(cmd1, tick1.getFirst().command());
    assertEquals(2, buffer.getScheduledTickCount());

    List<TurnCommandBuffer.ScheduledCommand> tick2 = buffer.drainCommandsForTick(2);
    assertEquals(1, tick2.size());
    assertEquals(cmd3, tick2.getFirst().command());
    assertEquals(1, buffer.getScheduledTickCount());

    List<TurnCommandBuffer.ScheduledCommand> tick3 = buffer.drainCommandsForTick(3);
    assertEquals(1, tick3.size());
    assertEquals(cmd2, tick3.getFirst().command());
    assertTrue(buffer.isEmpty());
  }

  @Test
  void testClear() {
    buffer.schedule(1, 0, new PlayerCommand.SetPreferredGamespeed(1));
    buffer.schedule(2, 1, new PlayerCommand.SetPreferredGamespeed(2));
    assertFalse(buffer.isEmpty());

    buffer.clear();
    assertTrue(buffer.isEmpty());
    assertEquals(0, buffer.getScheduledTickCount());
    assertTrue(buffer.drainCommandsForTick(1).isEmpty());
  }

  @Test
  void testDrainCommandsForTickDrainsEarlierUndrainedTicksInOrder() {
    PlayerCommand cmd1 = new PlayerCommand.SetPreferredGamespeed(1);
    PlayerCommand cmd2 = new PlayerCommand.SetPreferredGamespeed(2);
    PlayerCommand cmd3 = new PlayerCommand.SetPreferredGamespeed(3);

    buffer.schedule(2, 0, cmd1);
    buffer.schedule(4, 1, cmd2);
    buffer.schedule(6, 0, cmd3);

    assertEquals(3, buffer.getScheduledTickCount());

    List<TurnCommandBuffer.ScheduledCommand> drained = buffer.drainCommandsForTick(5);
    assertEquals(2, drained.size());
    assertEquals(2, drained.get(0).tick());
    assertEquals(cmd1, drained.get(0).command());
    assertEquals(4, drained.get(1).tick());
    assertEquals(cmd2, drained.get(1).command());

    assertEquals(1, buffer.getScheduledTickCount());
    assertFalse(buffer.isEmpty());

    List<TurnCommandBuffer.ScheduledCommand> remaining = buffer.drainCommandsForTick(6);
    assertEquals(1, remaining.size());
    assertEquals(6, remaining.getFirst().tick());
    assertEquals(cmd3, remaining.getFirst().command());
    assertTrue(buffer.isEmpty());
  }
}
