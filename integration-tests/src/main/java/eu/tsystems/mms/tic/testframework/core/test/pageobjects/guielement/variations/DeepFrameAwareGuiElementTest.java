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
 package eu.tsystems.mms.tic.testframework.core.test.pageobjects.guielement.variations;

import eu.tsystems.mms.tic.testframework.core.test.TestPage;
import eu.tsystems.mms.tic.testframework.core.test.pageobjects.guielement.AbstractGuiElementNonFunctionalAssertionTest;
import eu.tsystems.mms.tic.testframework.pageobjects.GuiElement;
import eu.tsystems.mms.tic.testframework.pageobjects.Locate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DeepFrameAwareGuiElementTest extends AbstractGuiElementNonFunctionalAssertionTest {

    @Override
    public GuiElement getGuiElementBy(Locate locate) {
        WebDriver driver = getWebDriver();
        GuiElement frame1 = new GuiElement(driver, By.name("frame1")).setName("frame1");
        GuiElement frame12 = new GuiElement(driver, By.name("frame12"), frame1).setName("frame2");
        GuiElement frame123 = new GuiElement(driver, By.name("frame123"), frame12).setName("frame3");
        GuiElement frame1234 = new GuiElement(driver, By.name("InputFrame1234"), frame123).setName("frame4");
        return new GuiElement(driver, locate, frame1234).setName("GuiElementUnderTest");
    }

    @Override
    protected TestPage getTestPage() {
        return TestPage.FRAME_TEST_PAGE;
    }
}