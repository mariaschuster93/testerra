/*
 * (C) Copyright T-Systems Multimedia Solutions GmbH 2018, ..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Peter Lehmann <p.lehmann@t-systems.com>
 *     pele <p.lehmann@t-systems.com>
 */
package eu.tsystems.mms.tic.testframework.internal.utils;

import eu.tsystems.mms.tic.testframework.utils.ThrowableUtils;

/**
 * Created by piet on 11.03.16.
 */
public class CoreExceptionUtils extends ThrowableUtils {

    public static String getSimpleNameFromClassString(final String fullClassName) {
        String[] split = fullClassName.split("\\.");
        if (split.length <= 1) {
            return fullClassName;
        }
        return split[split.length - 1];
    }

    public static int findSubclassCallBackwards(StackTraceElement[] stackTrace, int from, Class clazz, final String methodNameToLookFor) {
        for (int i = from; i >= 0; i--) {
            StackTraceElement stackTraceElement = stackTrace[i];
            String className = stackTraceElement.getClassName();
            String methodName = stackTraceElement.getMethodName();

            boolean foundClass = clazz == null;
            boolean foundMethod = methodNameToLookFor == null;

            // search class
            if (!foundClass) {
                try {
                    Class<?> aClass = Class.forName(className);
                    if (clazz.isAssignableFrom(aClass)) {
                        foundClass = true;
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }

            // search method
            if (foundClass && !foundMethod) {
                if (methodNameToLookFor.equals(methodName)) {
                    foundMethod = true;
                }
            }

            // found everything?
            if (foundClass && foundMethod) {
                return i;
            }
        }
        return -1;
    }
}