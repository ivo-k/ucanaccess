package net.ucanaccess.jdbc;

import net.ucanaccess.test.AccessVersionSource;
import net.ucanaccess.test.UcanaccessBaseFileTest;
import net.ucanaccess.type.AccessVersion;
import org.junit.jupiter.params.ParameterizedTest;

import java.sql.SQLException;

class BooleanTest extends UcanaccessBaseFileTest {

    @Override
    protected void init(AccessVersion _accessVersion) throws SQLException {
        super.init(_accessVersion);
        executeStatements(
            "CREATE TABLE tblMain (id INT NOT NULL PRIMARY KEY, company TEXT NOT NULL, closed YESNO)",
            "INSERT INTO tblMain (id, company) VALUES(1, 'pippo')",
            "UPDATE tblMain SET closed=yes",
            "INSERT INTO t (pk) VALUES('pippo')");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @AccessVersionSource
    void testCreate(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);
        dumpQueryResult("SELECT * FROM tblMain");
        dumpQueryResult("SELECT * FROM t");
        checkQuery("SELECT * FROM tblMain");
        checkQuery("SELECT * FROM t");
    }

}
