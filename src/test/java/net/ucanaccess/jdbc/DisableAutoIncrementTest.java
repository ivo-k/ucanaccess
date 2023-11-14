package net.ucanaccess.jdbc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.ucanaccess.test.AccessVersion;
import net.ucanaccess.test.UcanaccessBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;

class DisableAutoIncrementTest extends UcanaccessBaseTest {

    @Override
    protected void init(AccessVersion _accessVersion) throws SQLException {
        super.init(_accessVersion);
        executeStatements("CREATE TABLE CT (id COUNTER PRIMARY KEY, descr TEXT) ",
            "CREATE TABLE [C T] (id COUNTER PRIMARY KEY, descr TEXT)");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("net.ucanaccess.test.AccessVersion#getDefaultAccessVersion()")
    void testGuid(AccessVersion _accessVersion) throws SQLException, IOException {
        init(_accessVersion);
        try (Statement st = ucanaccess.createStatement()) {
            executeStatements(st,
            "CREATE TABLE CT1 (id GUID PRIMARY KEY, descr TEXT)",
            "INSERT INTO CT1 (descr) VALUES ('CIAO')");

            checkQuery("SELECT * FROM CT1");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("net.ucanaccess.test.AccessVersion#getDefaultAccessVersion()")
    void testDisable(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);
        try (Statement st = ucanaccess.createStatement()) {
            executeStatements(st,
                "INSERT INTO CT (descr) VALUES ('CIAO')",
                "DISABLE AUTOINCREMENT ON CT");
            assertThatThrownBy(() -> st.execute("INSERT INTO CT (descr) VALUES ('CIAO')"))
                .isInstanceOf(UcanaccessSQLException.class)
                .hasMessageContaining("integrity constraint violation: NOT NULL check constraint");
            executeStatements(st,
                "ENABLE AUTOINCREMENT ON CT",
                "INSERT INTO CT (descr) VALUES ('CIAO')",
                "DISABLE AUTOINCREMENT ON[C T]");
        }
    }

}
