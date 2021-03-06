/*
 * <!--
 *    Copyright (C) 2013 - 2015 Alexander "Evisceration" Martinz
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * -->
 */

package org.namelessrom.screencast;

import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

/**
 * A Logging utility
 */
public class Logger {
    /**
     * If disabled nothing will happen at all
     */
    private static boolean DEBUG = false;

    /**
     * Enables or disables logging. If disabled all operations will not do anything and return early.
     *
     * @param enable True to enable, false to disable the logger.
     */
    public static synchronized void setEnabled(final boolean enable) {
        DEBUG = enable;
    }

    /**
     * @return Whether the logger is enabled.
     */
    public static boolean getEnabled() {
        return DEBUG;
    }

    public static void d(final Object object, final String msg) {
        if (DEBUG) Log.d(getTag(object), getMessage(msg));
    }

    public static void d(final Object object, final String msg, final Object... objects) {
        if (DEBUG) Log.d(getTag(object), getMessage(msg, objects));
    }

    public static void d(final Object object, final String msg, final Exception exception) {
        if (DEBUG) Log.d(getTag(object), getMessage(msg), exception);
    }

    public static void e(final Object object, final String msg) {
        if (DEBUG) Log.e(getTag(object), getMessage(msg));
    }

    public static void e(final Object object, final String msg, final Object... objects) {
        if (DEBUG) Log.e(getTag(object), getMessage(msg, objects));
    }

    public static void e(final Object object, final String msg, final Exception exception) {
        if (DEBUG) Log.e(getTag(object), getMessage(msg), exception);
    }

    public static void i(final Object object, final String msg) {
        if (DEBUG) Log.i(getTag(object), getMessage(msg));
    }

    public static void i(final Object object, final String msg, final Object... objects) {
        if (DEBUG) Log.i(getTag(object), getMessage(msg, objects));
    }

    public static void i(final Object object, final String msg, final Exception exception) {
        if (DEBUG) Log.i(getTag(object), getMessage(msg), exception);
    }

    public static void v(final Object object, final String msg) {
        if (DEBUG) Log.v(getTag(object), getMessage(msg));
    }

    public static void v(final Object object, final String msg, final Object... objects) {
        if (DEBUG) Log.v(getTag(object), getMessage(msg, objects));
    }

    public static void v(final Object object, final String msg, final Exception exception) {
        if (DEBUG) Log.v(getTag(object), getMessage(msg), exception);
    }

    public static void w(final Object object, final String msg) {
        if (DEBUG) Log.w(getTag(object), getMessage(msg));
    }

    public static void w(final Object object, final String msg, final Object... objects) {
        if (DEBUG) Log.w(getTag(object), getMessage(msg, objects));
    }

    public static void w(final Object object, final String msg, final Exception exception) {
        if (DEBUG) Log.w(getTag(object), getMessage(msg), exception);
    }

    public static void wtf(final Object object, final String msg) {
        if (DEBUG) Log.wtf(getTag(object), getMessage(msg));
    }

    public static void wtf(final Object object, final String msg, final Object... objects) {
        if (DEBUG) Log.wtf(getTag(object), getMessage(msg, objects));
    }

    public static void wtf(final Object object, final String msg, final Exception exception) {
        if (DEBUG) Log.wtf(getTag(object), getMessage(msg), exception);
    }

    /**
     * Turns the passed in object into a string, representing a "Tag". <br/>
     * If a string is passed in we are returning the string, else we are getting the simple name
     * of the object as "Tag".
     *
     * @param object The object, representing a Tag
     * @return The object as Tag
     */
    public static String getTag(final Object object) {
        if (object instanceof String) {
            return ((String) object);
        } else if (object == null) {
            return "Logger";
        } else {
            return object.getClass().getSimpleName();
        }
    }

    public static String getMessage(final String msg) {
        return String.format("--> %s", msg);
    }

    public static String getMessage(final String msg, final Object... objects) {
        return String.format("--> %s", String.format(msg, objects));
    }

    /**
     * If enabled we are logging every violation at the log. Shall NOT be turned on in production!
     *
     * @param enabled True to enable {@link android.os.StrictMode.ThreadPolicy} and {@link android.os.StrictMode.VmPolicy} detection
     */
    public static void setStrictModeEnabled(boolean enabled) {
        StrictMode.ThreadPolicy.Builder threadBuilder = new StrictMode.ThreadPolicy.Builder();
        StrictMode.VmPolicy.Builder vmBuilder = new StrictMode.VmPolicy.Builder();

        if (enabled) {
            threadBuilder
                    .detectAll()
                    .detectCustomSlowCalls()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyFlashScreen();

            vmBuilder
                    .detectAll()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                vmBuilder.detectLeakedRegistrationObjects();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    vmBuilder.detectFileUriExposure();
                }
            }
        }

        StrictMode.setThreadPolicy(threadBuilder.build());
        StrictMode.setVmPolicy(vmBuilder.build());
    }
}
