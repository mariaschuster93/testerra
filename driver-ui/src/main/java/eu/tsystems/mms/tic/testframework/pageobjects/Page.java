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
 *     Peter Lehmann
 *     pele
 */
/*
 * Created on 04.01.2013
 *
 * Copyright(c) 2011 - 2012 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, 01129 Dresden
 * All rights reserved.
 */
package eu.tsystems.mms.tic.testframework.pageobjects;

import eu.tsystems.mms.tic.testframework.annotations.Fails;
import eu.tsystems.mms.tic.testframework.common.PropertyManager;
import eu.tsystems.mms.tic.testframework.common.Testerra;
import eu.tsystems.mms.tic.testframework.constants.Browsers;
import eu.tsystems.mms.tic.testframework.constants.GuiElementType;
import eu.tsystems.mms.tic.testframework.constants.TesterraProperties;
import eu.tsystems.mms.tic.testframework.internal.Flags;
import eu.tsystems.mms.tic.testframework.internal.StopWatch;
import eu.tsystems.mms.tic.testframework.pageobjects.filter.WebElementFilter;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.action.FieldAction;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.action.FieldWithActionConfig;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.action.SetGuiElementTimeoutFieldAction;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.action.SetNameFieldAction;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.action.groups.GuiElementGroupAction;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.action.groups.GuiElementGroups;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.asserts.AssertionProvider;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.asserts.PropertyAssertion;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.asserts.ScreenshotAssertion;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.asserts.StringPropertyAssertion;
import eu.tsystems.mms.tic.testframework.report.model.context.MethodContext;
import eu.tsystems.mms.tic.testframework.report.model.context.Screenshot;
import eu.tsystems.mms.tic.testframework.report.utils.ExecutionContextController;
import eu.tsystems.mms.tic.testframework.transfer.ThrowablePackedResponse;
import eu.tsystems.mms.tic.testframework.utils.JSUtils;
import eu.tsystems.mms.tic.testframework.utils.StringUtils;
import eu.tsystems.mms.tic.testframework.utils.Timer;
import eu.tsystems.mms.tic.testframework.utils.TimerUtils;
import eu.tsystems.mms.tic.testframework.utils.UITestUtils;
import eu.tsystems.mms.tic.testframework.webdrivermanager.WebDriverManager;
import eu.tsystems.mms.tic.testframework.webdrivermanager.WebDriverRequest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Page implements the PageObject pattern
 * @see {https://martinfowler.com/bliki/PageObject.html}
 * @author Peter Lehmann
 * @author Mike Reiche
 */
public abstract class Page extends AbstractPage {

    public static final String CHECKPAGE_METHOD_NAME = "checkPage";
    private final GuiElementGroups guiElementGroups;
    private static List<PageLoadHandler> pageLoadHandlers = new LinkedList<>();
    private static final GuiElementFactory guiElementFactory = ioc.getInstance(GuiElementFactory.class);

    private static class FrameFinder implements Finder {
        private final IGuiElement frame;
        private FrameFinder(IGuiElement frame) {
            this.frame = frame;
        }
        public IGuiElement find(Locate locator) {
            return guiElementFactory.createWithFrames(locator, frame);
        }
    }

    protected Finder inFrame(IGuiElement frame) {
        return new FrameFinder(frame);
    }

    /**
     * @deprecated This constructor should be protected
     */
    @Deprecated
    public Page(WebDriver webDriver) {
        if (webDriver == null) {
            throw new IllegalArgumentException("The driver object must not be null");
        }
        this.driver = webDriver;

        // webdriver based waitForPageToLoad
        waitForPageToLoad();

        // performance test stop timer
        perfTestExtras();

        // call page load handlers
        for (PageLoadHandler pageLoadHandler : pageLoadHandlers) {
            pageLoadHandler.run(this);
        }

        // initialize ge groups
        guiElementGroups = new GuiElementGroups();
    }

    public static void registerPageLoadHandler(PageLoadHandler h) {
        pageLoadHandlers.add(h);
    }

    /**
     * Execute loadtestspecific
     */
    private void perfTestExtras() {
        StopWatch.stopPageLoad(driver, this.getClass());

        if (UITestUtils.getGuiElementType() == GuiElementType.perf) {
            executeThinkTime();
        }

        // activate the PERF_STOP_WATCH Flag if not already, to assure storing of pageLoadInfos only in case of perf test
        if (!Flags.PERF_STOP_WATCH_ACTIVE) {
            Flags.PERF_STOP_WATCH_ACTIVE = true;
        }
    }

    /**
     * executes think time
     */
    private void executeThinkTime() {
        // Thinktime in Properties
        int thinkTime = PropertyManager.getIntProperty(TesterraProperties.PERF_PAGE_THINKTIME_MS, 0);
        // timeOut for Threadsleep
        int timeToWait = 0;
        /*
        Zufallsabweichung für Thinktimes > 0
         */
        if (thinkTime > 0) {
            int randomDelta = 2000;
            Random r = new Random();
            int randomValue = r.nextInt(randomDelta * 2);

            timeToWait = thinkTime + (randomValue - randomDelta);
            if (timeToWait < 0) {
                timeToWait = 0;
            }
        }
        // wait Thinktime + Offset
        logger.info("Waiting a Thinktime of " + timeToWait + " milliseconds");
        TimerUtils.sleep(timeToWait);
    }

    @Override
    public void waitForPageToLoad() {
    }

    @Override
    protected void handleDemoMode(WebDriver webDriver) {
        if (Testerra.Properties.DEMO_MODE.asBool() && webDriver != null) {
            JSUtils.turnOnDemoModeForCurrentPage(webDriver);
        }
    }

    @Override
    protected List<FieldAction> getFieldActions(List<FieldWithActionConfig> fields, AbstractPage declaringPage) {
        List<FieldAction> fieldActions = new ArrayList<>();
        for (FieldWithActionConfig field : fields) {
            GuiElementCheckFieldAction guiElementCheckFieldAction = new GuiElementCheckFieldAction(field, declaringPage);
            SetNameFieldAction setNameFieldAction = new SetNameFieldAction(field.field, declaringPage);
            GuiElementGroupAction guiElementGroupAction = new GuiElementGroupAction(field.field, declaringPage, guiElementGroups);

            /*
            Priority List!!
             */
            fieldActions.add(setNameFieldAction);
            fieldActions.add(new SetGuiElementTimeoutFieldAction(field.field, declaringPage));
            fieldActions.add(guiElementGroupAction);
            fieldActions.add(guiElementCheckFieldAction);
        }
        return fieldActions;
    }

    /**
     * Send F5 to the browser.
     */
    public Page refresh() {
        return refresh(false);
    }

    /**
     * Send F5 to the browser.
     */
    public Page refresh(boolean checkPage) {
        driver.navigate().refresh();
        if (checkPage) {
            pCheckPage(false, false, false);
        }
        return this;
    }

    public boolean isTextPresent(String text) {
        driver.switchTo().defaultContent();

        // check frames recursive
        boolean out = pIsTextPresentRecursive(false, text);

        // switch back to default frame
        driver.switchTo().defaultContent();
        return out;
    }

    public boolean isTextDisplayed(String text) {
        driver.switchTo().defaultContent();

        // check frames recursive
        boolean out = pIsTextPresentRecursive(true, text);

        // switch back to default frame
        driver.switchTo().defaultContent();
        return out;
    }

    private static final String TEXT_FINDER_PLACEHOLDER = "###TEXT###";
    private static final String TEXT_FINDER_XPATH = "//text()[contains(., '" + TEXT_FINDER_PLACEHOLDER + "')]/..";

    @Fails(validFor = "unsupportedBrowser=true")
    private boolean pIsTextPresentRecursive(final boolean isDisplayed, final String text) {
        // check for text in current frame
        GuiElement textElement;

        String textFinderXpath = TEXT_FINDER_XPATH.replace(TEXT_FINDER_PLACEHOLDER, text);

        textElement = new GuiElement(driver, By.xpath(textFinderXpath));
        if (isDisplayed) {
            textElement.withWebElementFilter(WebElementFilter.DISPLAYED.is(true));
        }

        textElement.setTimeoutInSeconds(1);
        if (textElement.isPresent()) {
            // highlight
            WebElement webElement = textElement.getWebElement();
            JSUtils.highlightWebElementStatic(driver, webElement, 0, 255, 0);
            return true;
        }

        /*
         scan for frames
          */

        // exit when safari
        WebDriverRequest request = WebDriverManager.getRelatedWebDriverRequest(driver);
        if (Browsers.safari.equalsIgnoreCase(request.browser) || Browsers.phantomjs.equalsIgnoreCase(request.browser)) {
            String msg = "Recursive Page Scan does not work. Unsupported Browser.";
            logger.error(msg);
            MethodContext methodContext = ExecutionContextController.getCurrentMethodContext();
            if (methodContext != null) {
                methodContext.addPriorityMessage(msg);
            }
            // don't return here, let it run into failure...
        }

        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        for (WebElement iframe : iframes) {
            driver.switchTo().frame(iframe);
            if (pIsTextPresentRecursive(isDisplayed, text)) {
                return true;
            }
            driver.switchTo().parentFrame();
        }

        List<WebElement> frames = driver.findElements(By.tagName("frame"));
        for (WebElement frame : frames) {
            driver.switchTo().frame(frame);
            if (pIsTextPresentRecursive(isDisplayed, text)) {
                return true;
            }
            driver.switchTo().parentFrame();
        }

        return false;
    }

    /**
     * Waits for a text to be not present.
     *
     * @return boolean true if success == text is not present. false otherwise.
     */
    public boolean waitForIsNotTextPresentWithDelay(final String text, final int delayInSeconds) {
        TimerUtils.sleep(delayInSeconds * 1000);
        return waitForIsNotTextPresent(text);
    }

    /**
     * Waits for a text to be not present.
     *
     * @return boolean true if success == text is not present. false otherwise.
     */
    public boolean waitForIsNotTextDisplayedWithDelay(final String text, final int delayInSeconds) {
        TimerUtils.sleep(delayInSeconds * 1000);
        return waitForIsNotTextDisplayed(text);
    }

    /**
     * Waits for a text to be not present.
     *
     * @return boolean true if success == text is not present. false otherwise.
     */
    public boolean waitForIsNotTextPresent(final String text) {
        final Timer timer = new Timer();
        final ThrowablePackedResponse<Boolean> response = timer.executeSequence(new Timer.Sequence<Boolean>() {
            @Override
            public void run() {
                final boolean textPresent = isTextPresent(text);
                final boolean passState = !textPresent;
                setPassState(passState);
                setReturningObject(passState);
            }
        });

        if (response.hasThrowable()) {
            logger.error("waitForIsNotTextPresent ran into an error", response.getThrowable());
        }
        return response.getResponse();
    }

    /**
     * Waits for a text to be not displayed.
     *
     * @return boolean true if success == text is not present. false otherwise.
     */
    public boolean waitForIsNotTextDisplayed(final String text) {
        final Timer timer = new Timer();
        final ThrowablePackedResponse<Boolean> response = timer.executeSequence(new Timer.Sequence<Boolean>() {
            @Override
            public void run() {
                final boolean textDisplayed = isTextDisplayed(text);
                final boolean passState = !textDisplayed;
                setPassState(passState);
                setReturningObject(passState);
            }
        });

        if (response.hasThrowable()) {
            logger.error("waitForIsNotTextDisplayed ran into an error", response.getThrowable());
        }
        return response.getResponse();
    }

    public boolean waitForIsTextPresent(final String text) {
        Timer timer = new Timer(Testerra.Properties.ELEMENT_WAIT_INTERVAL_MS.asLong(), elementTimeoutInSeconds * 1000);
        ThrowablePackedResponse<Boolean> response = timer.executeSequence(new Timer.Sequence<Boolean>() {
            @Override
            public void run() {
                boolean textPresent = isTextPresent(text);
                setPassState(textPresent);
                setReturningObject(textPresent);
            }
        });
        return response.getResponse();
    }

    public boolean waitForIsTextDisplayed(final String text) {
        Timer timer = new Timer(Testerra.Properties.ELEMENT_WAIT_INTERVAL_MS.asLong(), elementTimeoutInSeconds * 1000);
        ThrowablePackedResponse<Boolean> response = timer.executeSequence(new Timer.Sequence<Boolean>() {
            @Override
            public void run() {
                boolean textPresent = isTextDisplayed(text);
                setPassState(textPresent);
                setReturningObject(textPresent);
            }
        });
        return response.getResponse();
    }

    public void assertIsTextPresent(String text, String description) {
        logger.info("assertIsTextPresent '" + text + "' on " + this.getClass().getSimpleName());
        Assert.assertTrue(waitForIsTextPresent(text), "Text '" + text + "' is present on current page. " + description);
    }

    public void assertIsTextDisplayed(String text, String description) {
        logger.info("assertIsTextDisplayed '" + text + "' on " + this.getClass().getSimpleName());
        Assert.assertTrue(waitForIsTextDisplayed(text), "Text '" + text + "' is displayed on current page. " + description);
    }

    public void assertIsTextPresent(String text) {
        logger.info("assertIsTextPresent '" + text + "' on " + this.getClass().getSimpleName());
        Assert.assertTrue(waitForIsTextPresent(text), "Text '" + text + "' is present on current page");
    }

    public void assertIsTextDisplayed(String text) {
        logger.info("assertIsTextDisplayed '" + text + "' on " + this.getClass().getSimpleName());
        Assert.assertTrue(waitForIsTextDisplayed(text), "Text '" + text + "' is displayed on current page");
    }

    public void assertIsNotTextPresent(String text) {
        logger.info("assertIsNotTextPresent '" + text + "' on " + this.getClass().getSimpleName());
        Assert.assertFalse(isTextPresent(text), "Text '" + text + "' is present on current page");
    }

    public void assertIsNotTextDisplayed(String text) {
        logger.info("assertIsNotTextDisplayed '" + text + "' on " + this.getClass().getSimpleName());
        Assert.assertFalse(isTextDisplayed(text), "Text '" + text + "' is displayed on current page");
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public GuiElementGroups getGuiElementGroups() {
        return guiElementGroups;
    }

    /**
     * This method returns the steps string.
     * @param simpleClassName .
     * @param actionName .
     * @return .
     */
    public static String getPageContext(final String simpleClassName, String actionName) {
        if (StringUtils.isStringEmpty(actionName)) {
            return simpleClassName;
        }
        else if ("<init>".equals(actionName)) {
//            actionName = "Initialization";
            return simpleClassName;
        }
        else if (actionName.equals(Page.CHECKPAGE_METHOD_NAME)) {
            return simpleClassName;
        }

        return simpleClassName + " -> " + actionName;
    }

    /**
     * Fluent properties
     */
    public StringPropertyAssertion<String> title() {
        final Page self = this;
        return propertyAssertionFactory.string(new AssertionProvider<String>() {
            @Override
            public String getActual() {
                return driver.getTitle();
            }

            @Override
            public String getSubject() {
                return String.format("%s.title", self);
            }
        });
    }

    public StringPropertyAssertion<String> url() {
        final Page self = this;
        return propertyAssertionFactory.string(new AssertionProvider<String>() {
            @Override
            public String getActual() {
                return driver.getCurrentUrl();
            }

            @Override
            public String getSubject() {
                return String.format("%s.url", self);
            }
        });
    }

    /**
     * Takes a screenshot of the current page
     */
    public ScreenshotAssertion screenshot() {
        final Page self = this;
        final AtomicReference<Screenshot> atomicScreenshot = new AtomicReference<>();

        Screenshot screenshot = new Screenshot(self.toString());
        UITestUtils.takeScreenshot(driver, screenshot);
        atomicScreenshot.set(screenshot);

        return propertyAssertionFactory.screenshot(new AssertionProvider<Screenshot>() {

            @Override
            public Screenshot getActual() {
                return atomicScreenshot.get();
            }

            @Override
            public void failed(PropertyAssertion assertion) {
                // Take new screenshot only if failed
                UITestUtils.takeScreenshot(driver, atomicScreenshot.get());
            }

            @Override
            public String getSubject() {
                return self.toString();
            }
        });
    }

}
