package net.ucanaccess.test.integration;

import net.ucanaccess.test.util.AccessVersion;
import net.ucanaccess.test.util.UcanaccessTestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.Statement;

class WeirdObjectNamesTest extends UcanaccessTestBase {

    @Override
    protected String getAccessPath() {
        return TEST_DB_DIR + "weirdObjectNames.mdb";
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class)
    void testTableNameEndsInQuestionMarks(AccessVersion _accessVersion) throws Exception {
        init(_accessVersion);
        Statement st = ucanaccess.createStatement();
        checkQuery("SELECT * FROM [19 MB 01 BEZAHLT ???]");
        st.close();
    }

}
