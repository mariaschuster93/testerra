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
package eu.tsystems.mms.tic.testframework.core.test.pageobjects.page;

import eu.tsystems.mms.tic.testframework.AbstractTestSitesTest;
import eu.tsystems.mms.tic.testframework.core.test.TestPage;
import eu.tsystems.mms.tic.testframework.core.test.pageobjects.IPageFactoryTest;
import eu.tsystems.mms.tic.testframework.pageobjects.GuiElement;
import eu.tsystems.mms.tic.testframework.pageobjects.WebTestFramedPage;
import eu.tsystems.mms.tic.testframework.pageobjects.WebTestPage;
import eu.tsystems.mms.tic.testframework.pageobjects.factory.PageFactory;
import eu.tsystems.mms.tic.testframework.utils.JSUtils;
import eu.tsystems.mms.tic.testframework.webdrivermanager.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * Created by rnhb on 19.06.2015.
 *
 * Tests for correct execution of checkpage().
 * To test that checkpage() is executed, a not existing, check-annotated element is used.
 *
 */
public class AssertTextPageTest extends AbstractTestSitesTest implements IPageFactoryTest {

    @Override
    protected TestPage getStartPage() {
        return TestPage.FRAME_TEST_PAGE;
    }

    @Override
    public WebTestFramedPage getPage() {
        return PageFactory.create(WebTestFramedPage.class, WebDriverManager.getWebDriver());
    }

    @Test
    public void testT11_assertIsTextPresent() {
        WebTestFramedPage page = getPage();
        page.assertIsTextPresent("Frame1234");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testT12F_assertIsTextPresent() {
        WebTestFramedPage page = getPage();
        page.assertIsTextPresent("Bifi");
    }

    @Test
    public void testT13_assertIsNotTextPresent() {
        WebTestFramedPage page = getPage();
        page.assertIsNotTextPresent("Bifi");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testT14F_assertIsNotTextPresent() {
        WebTestFramedPage page = getPage();
        page.assertIsNotTextPresent("Frame1234");
    }

    @Test
    public void testT21_assertIsTextDisplayed() {
        WebTestFramedPage page = getPage();
        page.assertIsTextDisplayed("Frame1234");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testT22F_assertIsTextDisplayed() {
        WebTestFramedPage page = getPage();
        page.assertIsTextDisplayed("Bifi");
    }

    @Test
    public void testT23_assertIsNotTextDisplayed() {
        WebTestFramedPage page = getPage();
        page.assertIsNotTextDisplayed("Bifi");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testT24F_assertIsNotTextDisplayed() {
        WebTestFramedPage page = getPage();
        page.assertIsNotTextDisplayed("Frame1234");
    }

}
