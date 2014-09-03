/**
 * Copyright (C) 2010-2014 eBusiness Information, Excilys Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.androidannotations.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import android.os.PowerManager;

/**
 * <p>
 * Acquires a {@link PowerManager.WakeLock} for this method call.
 * </p>
 * <p>
 * May be used on methods with @Background or @UiThread.
 * </p>
 * <p>
 * <b>NOTE</b>: To use WakeLocks you need the
 * {@link android.permission.WAKE_LOCK} permission.
 * </p>
 * 
 * <b>Example</b>:
 * 
 * <pre>
 * &#064;EActivity
 * public class MyActivity extends Acitivy {
 * 
 * 	&#064;WakeLock
 * 	void backgroundTask() {
 * 		// this code executes with an active WakeLock.
 * 	}
 * }
 * </pre>
 * 
 * @see PowerManager
 * @see PowerManager.WakeLock
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface WakeLock {

	String DEFAULT_TAG = "NO_TAG";

	String tag() default DEFAULT_TAG;

	Level level() default Level.PARTIAL_WAKE_LOCK;

	Flag[] flags() default {};

	public enum Level {
		/**
		 * @see PowerManager.FULL_WAKE_LOCK
		 */
		FULL_WAKE_LOCK,

		/**
		 * @see PowerManager.PARTIAL_WAKE_LOCK
		 */
		PARTIAL_WAKE_LOCK,

		/**
		 * @see PowerManager.SCREEN_BRIGHT_WAKE_LOCK
		 */
		SCREEN_BRIGHT_WAKE_LOCK,

		/**
		 * @see PowerManager.SCREEN_DIM_WAKE_LOCK
		 */
		SCREEN_DIM_WAKE_LOCK;
	}

	public enum Flag {
		/**
		 * @see PowerManager.ACQUIRE_CAUSES_WAKEUP
		 */
		ACQUIRE_CAUSES_WAKEUP,

		/**
		 * @see PowerManager.ON_AFTER_RELEASE
		 */
		ON_AFTER_RELEASE
	}
}
