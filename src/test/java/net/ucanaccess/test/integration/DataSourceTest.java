package net.ucanaccess.test.integration;

import net.ucanaccess.jdbc.UcanaccessDataSource;
import net.ucanaccess.test.util.UcanaccessBaseTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

class DataSourceTest extends UcanaccessBaseTest {

    @Test
    void setNewDatabaseVersionBad() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        assertThrows(IllegalArgumentException.class, () -> uds.setNewDatabaseVersion("V200?"));
    }

    @Test
    void setNewDatabaseVersionGood() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        String ver = "V2003";
        uds.setNewDatabaseVersion(ver);
        assertEquals(ver, uds.getNewDatabaseVersion());
    }

    @Test
    void setLobScaleBad() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        assertThrows(IllegalArgumentException.class, () -> uds.setLobScale(3));
    }

    @Test
    void setLobScaleGood() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        Integer val = 4;
        uds.setLobScale(val);
        assertEquals(val, uds.getLobScale());
    }

    @Test
    void createNewDatabase() throws SQLException, IOException {
        File fileMdb = createTempFileName("ucaDataSourceTest", ".mdb");
        assertFalse(fileMdb.exists());

        UcanaccessDataSource uds = new UcanaccessDataSource();
        uds.setAccessPath(fileMdb.getAbsolutePath());
        uds.setNewDatabaseVersion("V2003");
        uds.setImmediatelyReleaseResources(true); // so we can delete it immediately after close

        Connection conn = uds.getConnection();
        assertTrue(fileMdb.exists());
        getLogger().info("DataSource connection successfully created file {}", uds.getAccessPath());
        conn.close();

        Boolean irrEffective = uds.getImmediatelyReleaseResources();
        // Note that a property is returned as null if we haven't explicitly set it in the DataSource
        irrEffective = irrEffective != null && irrEffective;
        if (irrEffective) {
            assertTrue(fileMdb.delete());
            assertFalse(fileMdb.exists());
        } else {
            getLogger().info("(Test database remains on disk)");
        }
    }
}
