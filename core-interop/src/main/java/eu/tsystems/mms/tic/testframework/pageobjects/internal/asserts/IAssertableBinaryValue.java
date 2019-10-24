package eu.tsystems.mms.tic.testframework.pageobjects.internal.asserts;

public interface IAssertableBinaryValue<T, E> {
    T actual();
    E isTrue();
    E isFalse();
    E isTrue(final String errorMessage);
    E isFalse(final String errorMessage);
}