package net.ucanaccess.jdbc;

import net.ucanaccess.test.AccessVersion;
import net.ucanaccess.test.UcanaccessBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ConcatNullsTrueTest extends UcanaccessBaseTest {

    ConcatNullsTrueTest() {
        // By default, any null value will cause the function to return null.
        // If the property is set false, then NULL values are replaced with empty strings.
        // see: http://hsqldb.org/doc/guide/builtinfunctions-chapt.html
        appendToJdbcURL(";concatnulls=true");
    }

    @Override
    protected String getAccessPath() {
        return TEST_DB_DIR + "badDb.accdb";
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("net.ucanaccess.test.AccessVersion#getDefaultAccessVersion()")
    void testConcat(AccessVersion _accessVersion) throws Exception {
        init(_accessVersion);
        checkQuery("SELECT 'aa2'& null FROM dual", new Object[][] {{null}});
    }

}