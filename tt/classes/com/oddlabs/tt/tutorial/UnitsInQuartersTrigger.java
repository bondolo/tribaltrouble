package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Unit;

public final strictfp class UnitsInQuartersTrigger extends TutorialTrigger {
	public UnitsInQuartersTrigger() {
		super(1f, 0f, "units_in_quarters");
	}

    @Override
	protected void run(Tutorial tutorial) {
		boolean housed = tutorial.getViewer().getLocalPlayer().getUnits().getSet().stream()
                .noneMatch(s -> s instanceof Unit);

        if (housed)
			tutorial.next(new RallyPointTrigger());
	}
}
