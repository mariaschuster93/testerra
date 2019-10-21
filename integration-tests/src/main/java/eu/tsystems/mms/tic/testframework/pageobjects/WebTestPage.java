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
/*
 * Created on 07.01.2013
 *
 * Copyright(c) 2013 - 2013 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, 01129 Dresden
 * All rights reserved.
 */
package eu.tsystems.mms.tic.testframework.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Created with IntelliJ IDEA. User: pele Date: 07.01.13 Time: 14:15 To change this template use File | Settings | File
 * Templates.
 */
public class WebTestPage extends AbstractWebTestPage {

    private final IGuiElement input1 = new GuiElement(driver, By.id("1"));
    /** A button on the page. */
    @Check(nonFunctional = true)
    private final IGuiElement button1 = new GuiElement(driver, By.id("4"));

    @Check
    private IGuiElement specialElementFromVariable;

    /** The output text field. */
    @Check
    private final IGuiElement textOutputField = new GuiElement(driver, By.id("99"));

    public static class MyVariables implements PageVariables {

        int number;

        public MyVariables(int number) {
            this.number = number;
        }
    }

    /**
     * Default Page constructor.
     */
    public WebTestPage(WebDriver driver, MyVariables myVariables) {
        super(driver);

        specialElementFromVariable = new GuiElement(driver, By.id("" + myVariables.number));
    }

    public WebTestPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Test if button works as aspected.
     */
    public void assertFunctionalityOfButton1() {
        final String something = "some";
        input1.type(something);
        textOutputField.asserts().assertTextContains(something);
    }

    /**
     * Click on link to open this page again.
     *
     * @return Instance of the new page.
     */
    public WebTestPage reloadPage() {
        openAgainLink.click();
        return new WebTestPage(driver);
    }

    /**
     * Click on not existing element
     *
     */
    public void gotoHell() {
        IGuiElement IGuiElement = new GuiElement(driver, By.xpath("id('surely_not_existing')"));
        IGuiElement.click();
    }

    /**
     * Proof whether non existing element is present
     *
     */
    public void nonfunctionalAssert() {
        GuiElement IGuiElement = new GuiElement(driver, By.xpath("id('surely_not_existing')"));
        IGuiElement.nonFunctionalAssert.assertIsPresent();
    }

}
