package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.Magic;
import com.oddlabs.tt.model.weapon.MagicFactory;

public final strictfp class MagicBehaviour implements Behaviour {
    private enum MagicState {
        PREPARING, CASTING, ENDING
    };

	private final Unit unit;
	private final MagicFactory magic_factory;
	private final MagicController controller;
	private Magic magic;

	private float anim_time;
	private MagicState state;

	public MagicBehaviour(Unit unit, MagicFactory magic_factory, MagicController controller) {
		this.unit = unit;
		this.magic_factory = magic_factory;
		this.controller = controller;
		init();
	}

    @Override
	public boolean isBlocking() {
		return true;
	}

    @Override
	public int animate(float t) {
		anim_time -= t;
		switch (state) {
			case PREPARING:
				if (anim_time <= 0) {
					state = MagicState.CASTING;
					magic = magic_factory.execute(unit);
					anim_time += magic_factory.getSecondsPerRelease() - magic_factory.getSecondsPerInit();
				}
				return Selectable.UNINTERRUPTIBLE;
			case CASTING:
				if (anim_time <= 0) {
					state = MagicState.ENDING;
					unit.getOwner().getWorld().getAnimationManagerGameTime().registerAnimation(magic);
					anim_time += magic_factory.getSecondsPerAnim() - magic_factory.getSecondsPerRelease();
				}
				return Selectable.UNINTERRUPTIBLE;
			case ENDING:
				if (anim_time > 0)
					return Selectable.UNINTERRUPTIBLE;
				else {
					controller.popNextTime();
					return Selectable.DONE;
				}
			default:
				throw new IllegalStateException("Invalid state: " + state);
		}
	}

	private void init() {
		state = MagicState.PREPARING;
		anim_time = magic_factory.getSecondsPerInit();
		unit.switchAnimation(1f/magic_factory.getSecondsPerAnim(), Unit.ANIMATION_MAGIC);
	}

    @Override
	public void forceInterrupted() {
		if (magic != null)
			magic.interrupt();
	}
}
