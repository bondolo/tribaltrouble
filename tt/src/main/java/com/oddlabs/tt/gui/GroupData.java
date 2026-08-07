package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;

/**
 * Styling and layout data for bordered GUI groups.
 */
public record GroupData(Box group, int captionLeft, int captionY, int captionOffset, Font captionFont) {
}
