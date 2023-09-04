/*
Copyright (c) 2012 Marco Amadei.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package net.ucanaccess.test.integration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;

import net.ucanaccess.jdbc.UcanaccessDataSource;
import net.ucanaccess.test.util.AbstractTestBase;

public class DataSourceTest extends AbstractTestBase {
    @Test
    public void setNewDatabaseVersionBad() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        assertThrows(IllegalArgumentException.class, () -> uds.setNewDatabaseVersion("V200?"));
    }

    @Test
    public void setNewDatabaseVersionGood() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        String ver = "V2003";
        uds.setNewDatabaseVersion(ver);
        assertEquals(ver, uds.getNewDatabaseVersion());
    }

    @Test
    public void setLobScaleBad() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        assertThrows(IllegalArgumentException.class, () -> uds.setLobScale(3));
    }

    @Test
    public void setLobScaleGood() {
        UcanaccessDataSource uds = new UcanaccessDataSource();
        Integer val = 4;
        uds.setLobScale(val);
        assertEquals(val, uds.getLobScale());
    }

    @Test
    public void createNewDatabase() throws SQLException, IOException {
        File fileMdb;
        fileMdb = File.createTempFile("ucaDataSourceTest", ".mdb");
        fileMdb.delete(); // delete the 0-byte file created above
        assertFalse(fileMdb.exists());

        UcanaccessDataSource uds = new UcanaccessDataSource();
        uds.setAccessPath(fileMdb.getAbsolutePath());
        uds.setNewDatabaseVersion("V2003");
        uds.setImmediatelyReleaseResources(true); // so we can delete it immediately after close

        Connection conn = uds.getConnection();
        assertTrue(fileMdb.exists());
        getLogger().info("DataSource connection successfully created the file: {}", uds.getAccessPath());
        conn.close();

        Boolean irrEffective = uds.getImmediatelyReleaseResources();
        // Note that a property is returned as null if we haven't explicitly set it in the DataSource
        irrEffective = (irrEffective == null ? false : irrEffective);
        if (irrEffective) {
            assertTrue(fileMdb.delete());
            assertFalse(fileMdb.exists());
        } else {
            getLogger().info("(Test database remains on disk.)");
        }
    }
}
