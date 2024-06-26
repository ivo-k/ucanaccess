package net.ucanaccess.jdbc;

import net.ucanaccess.test.AccessVersionSource;
import net.ucanaccess.test.UcanaccessBaseTest;
import net.ucanaccess.type.AccessVersion;
import org.junit.jupiter.params.ParameterizedTest;

import java.sql.SQLException;

class ByteTest extends UcanaccessBaseTest {

    @Override
    protected void init(AccessVersion _accessVersion) throws SQLException {
        super.init(_accessVersion);
        executeStatements(
            "CREATE TABLE t_byte (ID int NOT NULL PRIMARY KEY, company TEXT NOT NULL, Closed byte); ",
            "INSERT INTO t_byte (id,company) VALUES(1, 'pippo')",
            "UPDATE t_byte SET closed=255");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @AccessVersionSource
    void testCreate(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);
        dumpQueryResult("SELECT * FROM t_byte");
        checkQuery("SELECT * FROM t_byte");
    }

}
