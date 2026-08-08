package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.engine.resource.GLImage;
import com.oddlabs.tt.engine.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.sdl.SDLMouse.SDL_CreateColorCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_DestroyCursor;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ABGR8888;
import static org.lwjgl.sdl.SDLSurface.SDL_CreateSurfaceFrom;
import static org.lwjgl.sdl.SDLSurface.SDL_DestroySurface;

/**
 * SDL Cursor
 */
public final class Cursor extends NativeResource<Cursor.NativeCursor> {
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
        NativeCursor(@NonNull GLImage image, int xHot, int yHot) {
            int width = image.getWidth();
            int height = image.getHeight();

            long nativeCursor = MemoryUtil.NULL;
            SDL_Surface surface = SDL_CreateSurfaceFrom(width, height, SDL_PIXELFORMAT_ABGR8888, image.getPixels(),
                    width * Integer.BYTES);
            if (surface != null) {
                nativeCursor = SDL_CreateColorCursor(surface, xHot, yHot);
                SDL_DestroySurface(surface);
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
    public Cursor(@NonNull GLImage image, int xHot, int yHot) {
        super(new NativeCursor(image, xHot, yHot));
    }

    @Override
    public void close() {
        Renderer.getLocalInput().getPointerInput().deletingCursor(this);
        super.close();
    }

    public long getCursor() {
        return state.cursor;
    }
}
