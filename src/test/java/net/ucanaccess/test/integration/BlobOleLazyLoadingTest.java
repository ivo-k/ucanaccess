package net.ucanaccess.test.integration;

import net.ucanaccess.test.util.AccessVersion;
import net.ucanaccess.test.util.UcanaccessBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

class BlobOleLazyLoadingTest extends UcanaccessBaseTest {

    @Override
    protected String getAccessPath() {
        return TEST_DB_DIR + "blobOleLazyLoading.accdb";
    }

    // It only works with JRE 1.6 and later (JDBC 3)
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void testBlobOLE(AccessVersion _accessVersion) throws SQLException, IOException {
        init(_accessVersion);

        final long binaryFileSize = 32718;
        byte[] initialBlobBytes = getBlobBytes();
        getLogger().info("BLOB size in backing database before retrieval: {} bytes", initialBlobBytes.length);
        assertTrue(initialBlobBytes.length < binaryFileSize);
        Statement st = ucanaccess.createStatement();
        ResultSet rs = st.executeQuery("SELECT Ole FROM OleTable ORDER BY ID");
        File fl = createTempFileName("Copied", ".jpeg");
        rs.next();
        @SuppressWarnings("unused")
        Object obj = rs.getObject(1);
        InputStream isDB = rs.getBlob(1).getBinaryStream();
        OutputStream outFile = new FileOutputStream(fl);
        byte[] ba = new byte[4096];
        int len;
        while ((len = isDB.read(ba)) != -1) {
            outFile.write(ba, 0, len);
        }
        outFile.flush();
        outFile.close();
        assertEquals(fl.length(), binaryFileSize);
        getLogger().info("File was created in {}, size: {} bytes", fl.getAbsolutePath(), fl.length());
        fl.delete();
        byte[] finalBlobBytes = getBlobBytes();
        getLogger().info("BLOB size in backing database after retrieval: {} bytes", finalBlobBytes.length);
        if (!Arrays.equals(initialBlobBytes, finalBlobBytes)) {
            getLogger().warn("Simply retrieving BLOB changed byte data in backing database. Problem?");
        }
    }

    private byte[] getBlobBytes() throws SQLException {
        Statement hsqlSt = ucanaccess.getHSQLDBConnection().createStatement();
        ResultSet hsqlRs = hsqlSt.executeQuery("SELECT OLE FROM OLETABLE ORDER BY ID");
        hsqlRs.next();
        byte[] blobBytes = hsqlRs.getBytes(1);
        hsqlRs.close();
        hsqlSt.close();
        return blobBytes;
    }

}
