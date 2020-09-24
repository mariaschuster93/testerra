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

import eu.tsystems.mms.tic.testframework.annotations.PageOptions;
import eu.tsystems.mms.tic.testframework.common.Testerra;
import eu.tsystems.mms.tic.testframework.enums.CheckRule;
import eu.tsystems.mms.tic.testframework.exceptions.PageNotFoundException;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.BasicUiElement;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.action.AbstractCheckFieldAction;
import eu.tsystems.mms.tic.testframework.testing.TestController;
import eu.tsystems.mms.tic.testframework.utils.StringUtils;
import java.lang.reflect.Field;

public class GuiElementCheckFieldAction extends AbstractCheckFieldAction {

    private final static TestController.Overrides overrides = Testerra.injector.getInstance(TestController.Overrides.class);
    private final CheckRule defaultCheckRule;

    public GuiElementCheckFieldAction(
            Field field,
            CheckRule defaultCheckRule,
            AbstractPage declaringPage
    ) {
        super(field, declaringPage);

        this.defaultCheckRule = defaultCheckRule;
    }

    private void pCheckField(
        BasicUiElement guiElement,
        Check check
    ) {

        CheckRule checkRule = check.checkRule();
        if (checkRule == CheckRule.DEFAULT) {
            checkRule = this.defaultCheckRule;
            if (checkRule == CheckRule.DEFAULT) {
                checkRule = CheckRule.valueOf(GuiElement.Properties.CHECK_RULE.asString());
            }
        }

        int useTimeout = check.timeout();
        if (useTimeout < 0) {
            if (declaringPage.getClass().isAnnotationPresent(PageOptions.class)) {
                PageOptions annotation = declaringPage.getClass().getAnnotation(PageOptions.class);
                useTimeout = annotation.elementTimeoutInSeconds();
                if (useTimeout < 0) {
                    useTimeout = overrides.getTimeoutInSeconds();
                }
            }
        }

        int prevTimeout = overrides.setTimeout(useTimeout);

        switch (checkRule) {
            case IS_PRESENT:
                guiElement.present(true);
                break;
            case IS_NOT_PRESENT:
                guiElement.present(false);
                break;
            case IS_NOT_DISPLAYED: {
                guiElement.displayed(false);
            }
            break;
            case DEFAULT:
            case IS_DISPLAYED:
            default: {
                guiElement.displayed(true);
            }
        }
        if (prevTimeout >= 0) {
            overrides.setTimeout(prevTimeout);
        }
    }

    @Override
    protected void checkField(BasicUiElement checkableInstance, Check check) {
        try {
            pCheckField(checkableInstance, check);
        } catch (AssertionError e) {
            final PageNotFoundException pageNotFoundException = new PageNotFoundException(null, e);

                /*
                if @Check has a prioritizedErrorMessage mark, then wrap t's
                 */
            String prioritizedErrorMessage = check.prioritizedErrorMessage();
            if (!StringUtils.isStringEmpty(prioritizedErrorMessage)) {
                throw new AssertionError(prioritizedErrorMessage, pageNotFoundException);
            }
            else {
                throw pageNotFoundException;
            }
        }
    }
//
//    @Override
//    protected void additionalBeforeCheck() {
//        if (field.isAnnotationPresent(IfJavascriptDisabled.class)) {
//
//            if (checkableInstance != null && checkableInstance instanceof GuiElement) {
//                // get the web driver session
//                GuiElement guiElement = (GuiElement) checkableInstance;
//                WebDriver driver = guiElement.getWebDriver();
//                String sessionId = WebDriverManagerUtils.getSessionKey(driver);
//
//                if (!StringUtils.isStringEmpty(sessionId)) {
//                    // do only search for the gui element if JS is disabled
//                    if (WebDriverManager.isJavaScriptActivated(driver)) {
//                        execute = false;
//                    }
//                }
//            }
//        }
//    }

}
