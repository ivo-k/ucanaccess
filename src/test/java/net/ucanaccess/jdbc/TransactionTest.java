package net.ucanaccess.jdbc;

import net.ucanaccess.test.AccessVersion;
import net.ucanaccess.test.UcanaccessBaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

class TransactionTest extends UcanaccessBaseTest {

    @Override
    protected void init(AccessVersion _accessVersion) throws SQLException {
        super.init(_accessVersion);
        executeStatements("CREATE TABLE T4 (id LONG, descr text(200))");
    }

    @AfterEach
    void afterEachTest() throws SQLException {
        executeStatements("DROP TABLE T4");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class)
    void testCommit(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);
        ucanaccess.setAutoCommit(false);
        try (Statement st = ucanaccess.createStatement()) {
            int i = getCount("SELECT COUNT(*) FROM T4");
            st.execute("INSERT INTO T4 (id, descr) VALUES(6666554, 'nel mezzo del cammin di nostra vita')");
            assertEquals(i, getCount("SELECT COUNT(*) FROM T4", false));
            ucanaccess.commit();
            assertEquals(i + 1, getCount("SELECT COUNT(*) FROM T4"));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class)
    void testSavepoint(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);
        int count = getCount("SELECT COUNT(*) FROM T4");
        ucanaccess.setAutoCommit(false);
        try (Statement st = ucanaccess.createStatement()) {
            st.execute("INSERT INTO T4 (id, descr) VALUES(1, 'nel mezzo del cammin di nostra vita')");
            Savepoint sp = ucanaccess.setSavepoint();
            assertEquals(count, getCount("SELECT COUNT(*) FROM T4", false));
            st.execute("INSERT INTO T4 (id, descr) VALUES(2, 'nel mezzo del cammin di nostra vita')");
            ucanaccess.rollback(sp);
            ucanaccess.commit();
            assertEquals(count + 1, getCount("SELECT COUNT(*) FROM T4"));
            ucanaccess.setAutoCommit(false);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class)
    void testSavepoint2(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);
        int count = getCount("SELECT COUNT(*) FROM T4");
        ucanaccess.setAutoCommit(false);
        try (Statement st = ucanaccess.createStatement()) {
            st.execute("INSERT INTO T4 (id, descr) VALUES(1, 'nel mezzo del cammin di nostra vita')");
            Savepoint sp = ucanaccess.setSavepoint("Gord svp");
            assertEquals(count, getCount("SELECT COUNT(*) FROM T4", false));
            st.execute("INSERT INTO T4 (id, descr) VALUES(2, 'nel mezzo del cammin di nostra vita')");
            ucanaccess.rollback(sp);
            ucanaccess.commit();
            assertEquals(count + 1, getCount("SELECT COUNT(*) FROM T4"));
            ucanaccess.setAutoCommit(false);
        }
    }

}
