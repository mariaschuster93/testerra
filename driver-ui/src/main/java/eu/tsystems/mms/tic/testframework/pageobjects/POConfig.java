/*
 * Testerra
 *
 * (C) 2020, Peter Lehmann, T-Systems Multimedia Solutions GmbH, Deutsche Telekom AG
 *
 * Deutsche Telekom AG and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
 package eu.tsystems.mms.tic.testframework.pageobjects;

import eu.tsystems.mms.tic.testframework.common.Testerra;
import eu.tsystems.mms.tic.testframework.enums.CheckRule;

@Deprecated
public final class POConfig {

    private static final PageOverrides pageOverrides = Testerra.injector.getInstance(PageOverrides.class);

    /** Private Constructor, cause this is a util class. */
    private POConfig() { }

    public static int getUiElementTimeoutInSeconds() {
        return pageOverrides.getTimeout();
    }

    public static void setUiElementTimeoutInSeconds(int uiElementTimeoutInSeconds) {
        pageOverrides.setTimeout(uiElementTimeoutInSeconds);
    }

    public static CheckRule getGuiElementCheckRule() {
        return pageOverrides.getCheckRule();
    }

    public static void setGuiElementCheckRule(CheckRule guiElementCheckRule) {
        pageOverrides.setCheckRule(guiElementCheckRule);
    }

    public static void setThreadLocalUiElementTimeoutInSeconds(int value) {
        pageOverrides.setTimeout(value);
    }

    public static void removeThreadLocalUiElementTimeout() {

    }

    public static void executeWithExplicitUiElementTimeout(int value, Runnable runnable) {
        int timeoutBefore = pageOverrides.getTimeout();
        pageOverrides.setTimeout(value);
        runnable.run();
        pageOverrides.setTimeout(timeoutBefore);
    }
}
