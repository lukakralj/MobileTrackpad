package com.lukakralj.mobiletrackpad.backend;

/**
 * An event that is triggered when something happens. This event can be subscribed
 * to certain actions and is triggered later on.
 * Interface was declared so tha lambdas can be used in place.
 *
 *  @author Luka Kralj
 *  @version 1.0
 */
public interface SubscriberEvent {
    void triggerEvent();
}