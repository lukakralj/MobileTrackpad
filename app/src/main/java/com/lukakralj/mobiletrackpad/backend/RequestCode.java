package com.lukakralj.mobiletrackpad.backend;

/**
 * All the possible request codes that the server can interpret.
 *
 *  @author Luka Kralj
 *  @version 1.0
 */
public enum RequestCode {
        MOUSE_DELTA,
        LEFT_CLICK,
        RIGHT_CLICK,
        LEFT_DOWN,
        LEFT_UP,
        SCROLL_UP,
        SCROLL_DOWN
}