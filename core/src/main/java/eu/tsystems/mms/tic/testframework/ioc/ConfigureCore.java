package eu.tsystems.mms.tic.testframework.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import eu.tsystems.mms.tic.testframework.execution.testng.DefaultFunctionalAssertion;
import eu.tsystems.mms.tic.testframework.execution.testng.DefaultInstantAssertion;
import eu.tsystems.mms.tic.testframework.execution.testng.DefaultNonFunctionalAssertion;
import eu.tsystems.mms.tic.testframework.execution.testng.DefaultTestAssertion;
import eu.tsystems.mms.tic.testframework.execution.testng.FunctionalAssertion;
import eu.tsystems.mms.tic.testframework.execution.testng.InstantAssertion;
import eu.tsystems.mms.tic.testframework.execution.testng.NonFunctionalAssertion;
import eu.tsystems.mms.tic.testframework.execution.testng.TestAssertion;
import eu.tsystems.mms.tic.testframework.internal.AssertionsCollector;
import eu.tsystems.mms.tic.testframework.internal.CollectedAssertions;

public class ConfigureCore extends AbstractModule {
    protected static boolean assertionsCollectorConfigured = false;
    protected static boolean assertionsConfigured = false;
    protected void configure() {
        if (!assertionsConfigured) {
            configureAssertions();
        }
        if (!assertionsCollectorConfigured) {
            configureAssertionsCollector();
        }
    }

    protected void configureAssertions() {
        bind(FunctionalAssertion.class).to(DefaultFunctionalAssertion.class).in(Scopes.SINGLETON);
        bind(NonFunctionalAssertion.class).to(DefaultNonFunctionalAssertion.class).in(Scopes.SINGLETON);
        bind(InstantAssertion.class).to(DefaultInstantAssertion.class).in(Scopes.SINGLETON);
        bind(TestAssertion.class).to(DefaultTestAssertion.class).in(Scopes.SINGLETON);
        assertionsConfigured = true;
    }

    protected void configureAssertionsCollector() {
        bind(AssertionsCollector.class).to(CollectedAssertions.class).in(Scopes.SINGLETON);
        assertionsCollectorConfigured = true;
    }
}