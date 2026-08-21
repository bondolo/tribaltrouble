package com.oddlabs.tt.input;


import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_0;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_1;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_2;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_3;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_4;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_5;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_6;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_7;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_8;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_9;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_A;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_APOSTROPHE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_B;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_BACKSLASH;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_BACKSPACE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_C;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_COMMA;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_D;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_DELETE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_DOWN;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_E;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_END;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_EQUALS;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_ESCAPE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F1;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F10;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F11;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F12;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F2;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F3;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F4;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F5;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F6;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F7;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F8;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F9;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_G;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_GRAVE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_H;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_HOME;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_I;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_INSERT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_J;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_K;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_0;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_1;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_2;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_3;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_4;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_5;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_6;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_7;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_8;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_9;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_DECIMAL;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_DIVIDE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MINUS;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MULTIPLY;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_PLUS;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_L;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LALT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LCTRL;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LEFT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LEFTBRACKET;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LGUI;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LSHIFT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_M;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_MINUS;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_N;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_O;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_P;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_PAGEDOWN;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_PAGEUP;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_PERIOD;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_Q;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_R;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RALT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RCTRL;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RETURN;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RGUI;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RIGHT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RIGHTBRACKET;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RSHIFT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_S;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_SEMICOLON;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_SLASH;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_SPACE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_T;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_TAB;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_U;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_UNKNOWN;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_UP;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_V;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_W;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_X;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_Y;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_Z;

/**
 * Enumeration of physical keyboard keys mapped to SDL scancodes.
 */
public enum Key {
    UP(SDL_SCANCODE_UP),
    DOWN(SDL_SCANCODE_DOWN),
    LEFT(SDL_SCANCODE_LEFT),
    RIGHT(SDL_SCANCODE_RIGHT),
    ESCAPE(SDL_SCANCODE_ESCAPE),
    SPACE(SDL_SCANCODE_SPACE),
    RETURN(SDL_SCANCODE_RETURN),
    TAB(SDL_SCANCODE_TAB),
    F1(SDL_SCANCODE_F1),
    F2(SDL_SCANCODE_F2),
    F3(SDL_SCANCODE_F3),
    F4(SDL_SCANCODE_F4),
    F5(SDL_SCANCODE_F5),
    F6(SDL_SCANCODE_F6),
    F7(SDL_SCANCODE_F7),
    F8(SDL_SCANCODE_F8),
    F9(SDL_SCANCODE_F9),
    F10(SDL_SCANCODE_F10),
    F11(SDL_SCANCODE_F11),
    F12(SDL_SCANCODE_F12),
    A(SDL_SCANCODE_A),
    B(SDL_SCANCODE_B),
    C(SDL_SCANCODE_C),
    D(SDL_SCANCODE_D),
    E(SDL_SCANCODE_E),
    F(SDL_SCANCODE_F),
    G(SDL_SCANCODE_G),
    H(SDL_SCANCODE_H),
    I(SDL_SCANCODE_I),
    J(SDL_SCANCODE_J),
    K(SDL_SCANCODE_K),
    L(SDL_SCANCODE_L),
    M(SDL_SCANCODE_M),
    N(SDL_SCANCODE_N),
    O(SDL_SCANCODE_O),
    P(SDL_SCANCODE_P),
    Q(SDL_SCANCODE_Q),
    R(SDL_SCANCODE_R),
    S(SDL_SCANCODE_S),
    T(SDL_SCANCODE_T),
    U(SDL_SCANCODE_U),
    V(SDL_SCANCODE_V),
    W(SDL_SCANCODE_W),
    X(SDL_SCANCODE_X),
    Y(SDL_SCANCODE_Y),
    Z(SDL_SCANCODE_Z),
    NUMPAD0(SDL_SCANCODE_KP_0),
    NUMPAD1(SDL_SCANCODE_KP_1),
    NUMPAD2(SDL_SCANCODE_KP_2),
    NUMPAD3(SDL_SCANCODE_KP_3),
    NUMPAD4(SDL_SCANCODE_KP_4),
    NUMPAD5(SDL_SCANCODE_KP_5),
    NUMPAD6(SDL_SCANCODE_KP_6),
    NUMPAD7(SDL_SCANCODE_KP_7),
    NUMPAD8(SDL_SCANCODE_KP_8),
    NUMPAD9(SDL_SCANCODE_KP_9),
    MULTIPLY(SDL_SCANCODE_KP_MULTIPLY),
    DIVIDE(SDL_SCANCODE_KP_DIVIDE),
    DECIMAL(SDL_SCANCODE_KP_DECIMAL),
    DELETE(SDL_SCANCODE_DELETE),
    BACK(SDL_SCANCODE_BACKSPACE),
    HOME(SDL_SCANCODE_HOME),
    END(SDL_SCANCODE_END),
    INSERT(SDL_SCANCODE_INSERT),
    PAGE_UP(SDL_SCANCODE_PAGEUP),
    PAGE_DOWN(SDL_SCANCODE_PAGEDOWN),
    LSHIFT(SDL_SCANCODE_LSHIFT),
    RSHIFT(SDL_SCANCODE_RSHIFT),
    LCONTROL(SDL_SCANCODE_LCTRL),
    RCONTROL(SDL_SCANCODE_RCTRL),
    LALT(SDL_SCANCODE_LALT),
    RALT(SDL_SCANCODE_RALT),
    LSUPER(SDL_SCANCODE_LGUI),
    RSUPER(SDL_SCANCODE_RGUI),
    COMMA(SDL_SCANCODE_COMMA),
    PERIOD(SDL_SCANCODE_PERIOD),
    SLASH(SDL_SCANCODE_SLASH),
    BACKSLASH(SDL_SCANCODE_BACKSLASH),
    SEMICOLON(SDL_SCANCODE_SEMICOLON),
    APOSTROPHE(SDL_SCANCODE_APOSTROPHE),
    LBRACKET(SDL_SCANCODE_LEFTBRACKET),
    RBRACKET(SDL_SCANCODE_RIGHTBRACKET),
    GRAVE(SDL_SCANCODE_GRAVE),
    KEY_1(SDL_SCANCODE_1),
    KEY_2(SDL_SCANCODE_2),
    KEY_3(SDL_SCANCODE_3),
    KEY_4(SDL_SCANCODE_4),
    KEY_5(SDL_SCANCODE_5),
    KEY_6(SDL_SCANCODE_6),
    KEY_7(SDL_SCANCODE_7),
    KEY_8(SDL_SCANCODE_8),
    KEY_9(SDL_SCANCODE_9),
    KEY_0(SDL_SCANCODE_0),
    EQUALS(SDL_SCANCODE_EQUALS),
    MINUS(SDL_SCANCODE_MINUS),
    ADD(SDL_SCANCODE_KP_PLUS),
    SUBTRACT(SDL_SCANCODE_KP_MINUS),
    KEY_UNKNOWN(SDL_SCANCODE_UNKNOWN);

    private final int sdlCode;

    private static final Map<Integer, Key> from_sdl_map = Arrays.stream(values()).collect(Collectors.toMap(
            Key::getSdlCode, Function.identity()));

    Key(int sdlCode) {
        this.sdlCode = sdlCode;
    }

    public int getSdlCode() {
        return sdlCode;
    }

    public static Key fromSdlCode(int code) {
        return from_sdl_map.getOrDefault(code, KEY_UNKNOWN);
    }

    public String getDisplayName() {
        String name = name();
        if (name.startsWith("KEY_")) return name.substring(4);
        if (name.startsWith("NUMPAD")) return "Num " + name.substring(6);
        return switch (this) {
            case LSHIFT -> "LShift";
            case RSHIFT -> "RShift";
            case LCONTROL -> "LCtrl";
            case RCONTROL -> "RCtrl";
            case LALT -> "LAlt";
            case RALT -> "RAlt";
            case LSUPER -> "LMeta";
            case RSUPER -> "RMeta";
            case RETURN -> "Enter";
            case BACK -> "Backspace";
            case EQUALS -> "=";
            case MINUS -> "-";
            case COMMA -> ",";
            case PERIOD -> ".";
            case SLASH -> "/";
            case BACKSLASH -> "\\";
            case SEMICOLON -> ";";
            case APOSTROPHE -> "'";
            case LBRACKET -> "[";
            case RBRACKET -> "]";
            case GRAVE -> "`";
            case ADD -> "Num +";
            case SUBTRACT -> "Num -";
            case MULTIPLY -> "Num *";
            case DIVIDE -> "Num /";
            case DECIMAL -> "Num .";
            default -> name;
        };
    }
}
