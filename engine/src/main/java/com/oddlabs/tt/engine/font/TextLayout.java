package com.oddlabs.tt.engine.font;


import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles text layout operations such as word wrapping and hit testing.
 */
public final class TextLayout {

    public record Line(CharSequence content, int startIndex) {
    }

    private final Font font;
    private final CharSequence text;
    private final int wrapWidth;
    private final List<Line> lines;
    private final int totalHeight;

    public TextLayout(Font font, CharSequence text, int wrapWidth) {
        this.font = font;
        this.text = text;
        this.wrapWidth = wrapWidth;
        this.lines = calculateWordWrap();
        this.totalHeight = lines.size() * font.getHeight();
    }

    public Font getFont() {
        return font;
    }

    public List<Line> getLines() {
        return lines;
    }

    public int getTextHeight() {
        return totalHeight;
    }

    public int getCursorLine(int index) {
        if (index < 0 || index > text.length()) {
            return 0;
        }
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int lineEnd = line.startIndex() + line.content().length();
            if (index >= line.startIndex() && index <= lineEnd) {
                return i;
            }
        }
        return lines.size() - 1;
    }

    public int getCursorX(int index) {
        if (index < 0 || index > text.length()) {
            return 0;
        }

        for (Line line : lines) {
            int lineEnd = line.startIndex() + line.content().length();
            if (index >= line.startIndex() && index <= lineEnd) {
                int indexInLine = index - line.startIndex();
                return font.getWidth(line.content().subSequence(0, indexInLine));
            }
        }
        // Should not be reached if index is valid
        return 0;
    }

    public int getLineIndexAtY(float y, float textBlockHeight) {
        if (lines.isEmpty()) return 0;
        int lineHeight = font.getHeight();
        // Y is usually from top, so invert for line index from top
        int lineIndex = (int) ((textBlockHeight - y) / lineHeight);
        return Math.clamp(lineIndex, 0, lines.size() - 1);
    }

    public int getCharacterIndexAt(float x, float y, float textBlockHeight) {
        if (lines.isEmpty()) return 0;

        int lineIndex = getLineIndexAtY(y, textBlockHeight);
        Line targetLine = lines.get(lineIndex);

        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(targetLine.content().toString());
        int start = iterator.first();
        int charIndexInLine = 0;
        float currentWidth = 0;
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String grapheme = targetLine.content().subSequence(start, end).toString();
            float charWidth = font.getWidth(grapheme);
            if (x >= currentWidth && x < currentWidth + charWidth) {
                charIndexInLine = start;
                break;
            }
            currentWidth += charWidth;
            charIndexInLine = end; // If click is past last char, set to end of line
        }
        return targetLine.startIndex() + charIndexInLine;
    }

    public int getLineStartCharIndex(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return 0;
        }
        return lines.get(lineIndex).startIndex();
    }

    public int getLineEndCharIndex(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return text.length();
        }
        Line line = lines.get(lineIndex);
        return line.startIndex() + line.content().length();
    }

    public int getLineNumberForCharIndex(int charIndex) {
        if (charIndex < 0 || charIndex > text.length()) {
            return 0;
        }
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (charIndex >= line.startIndex() && charIndex <= line.startIndex() + line.content().length()) {
                return i;
            }
        }
        return 0; // Should not happen for valid charIndex
    }

    private List<Line> calculateWordWrap() {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        List<Line> calculatedLines = new ArrayList<>();
        int lineStart = 0;

        while (lineStart < text.length()) {
            int lineEnd = findLineEnd(lineStart);
            calculatedLines.add(new Line(text.subSequence(lineStart, lineEnd).toString(), lineStart));
            lineStart = lineEnd;

            // Skip the newline or space that caused the break
            if (lineStart < text.length()) {
                int cp = Character.codePointAt(text, lineStart);
                if (cp == '\n' || cp == ' ') {
                    lineStart += Character.charCount(cp);
                }
            }
        }

        return Collections.unmodifiableList(calculatedLines);
    }

    private int findLineEnd(int lineStart) {
        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(text.toString());
        if (lineStart > 0) {
            iterator.following(lineStart - 1);
        } else {
            iterator.first();
        }

        int lastSpace = -1;
        int current = lineStart;
        int next;
        while ((next = iterator.next()) != BreakIterator.DONE) {
            String grapheme = text.subSequence(current, next).toString();
            if (grapheme.equals("\n")) {
                return current; // Forced line break
            }

            int firstCp = grapheme.codePointAt(0);
            if (Character.isWhitespace(firstCp)) {
                lastSpace = current;
            }

            int currentWidth = font.getWidth(text.subSequence(lineStart, next));
            if (currentWidth > wrapWidth) {
                // Word is longer than the line, break mid-word
                return lastSpace != -1 ? lastSpace : current; // Break at the last known space
            }
            current = next;
        }
        return text.length(); // End of text
    }
}
