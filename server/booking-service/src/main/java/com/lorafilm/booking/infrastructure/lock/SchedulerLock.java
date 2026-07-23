package com.lorafilm.booking.infrastructure.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that require a distributed scheduler lock.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchedulerLock {
    /**
     * Unique name of the lock.
     */
    String name();

    /**
     * Duration in seconds for which the lock is held at most (to prevent deadlocks if the node crashes).
     * Default is 5 minutes (300 seconds).
     */
    long lockAtMostForSeconds() default 300;
}
