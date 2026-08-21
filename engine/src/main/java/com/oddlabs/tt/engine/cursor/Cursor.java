package com.oddlabs.tt.engine.cursor;

import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.base.resource.NativeResource;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.system.MemoryUtil;

import java.util.logging.Logger;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLMouse.SDL_CreateColorCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_DestroyCursor;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ABGR8888;
import static org.lwjgl.sdl.SDLSurface.SDL_CreateSurfaceFrom;
import static org.lwjgl.sdl.SDLSurface.SDL_DestroySurface;

/**
 * SDL Cursor
 */
public final class Cursor extends NativeResource<Cursor.NativeCursor> {
    private static final Logger logger = Logger.getLogger(Cursor.class.getName());
    public static final Cursor NULL_CURSOR = new Cursor(MemoryUtil.NULL);

    static final class NativeCursor extends NativeResource.NativeState {
        private final long cursor;

        NativeCursor(long cursor) {
            this.cursor = cursor;
        }

        /**
         * Create a new cursor instance from an image
         *
         * @param image source cursor image
         * @param xHot x location from top left of cursor hot spot
         * @param yHot y location from top left of cursor hot spot
         */
        NativeCursor(GLImage image, int xHot, int yHot) {
            int width = image.getWidth();
            int height = image.getHeight();

            long nativeCursor = MemoryUtil.NULL;
            SDL_Surface surface = SDL_CreateSurfaceFrom(width, height, SDL_PIXELFORMAT_ABGR8888, image.getPixels(),
                    width * Integer.BYTES);
            if (surface != null) {
                nativeCursor = SDL_CreateColorCursor(surface, xHot, yHot);
                if (nativeCursor == MemoryUtil.NULL) {
                    logger.warning("SDL_CreateColorCursor failed for cursor (" + width + "x" + height + "): "
                            + SDL_GetError());
                }
                SDL_DestroySurface(surface);
            } else {
                logger.warning("SDL_CreateSurfaceFrom failed for cursor (" + width + "x" + height + "): "
                        + SDL_GetError());
            }

            this(nativeCursor);
        }

        @Override
        public void close() {
            if (cursor != MemoryUtil.NULL) {
                SDL_DestroyCursor(cursor);
            }
        }
    }

    private Cursor(long nativeCursor) {
        super(new NativeCursor(nativeCursor));
    }

    /**
     * Create a new cursor instance from an image
     *
     * @param image source cursor image
     * @param xHot x location from top left of cursor hot spot
     * @param yHot y location from top left of cursor hot spot
     */
    public Cursor(GLImage image, int xHot, int yHot) {
        super(new NativeCursor(image, xHot, yHot));
    }

    public long getCursor() {
        return state.cursor;
    }
}
