package net.ucanaccess.test.util;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Database.FileFormat;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import net.ucanaccess.complex.ComplexBase;
import net.ucanaccess.console.Main;
import net.ucanaccess.jdbc.UcanaccessConnection;
import net.ucanaccess.jdbc.UcanaccessDriver;
import org.junit.jupiter.api.AfterEach;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class UcanaccessBaseTest extends AbstractBaseTest {

    protected static final String TEST_DB_DIR   = "testdbs/";
    private static final File TEST_TEMP_DIR = createTempDir("ucanaccess-test");

    static {
        Main.setBatchMode(true);
    }

    private File fileAccDb;
    private String password          = "";
    private AccessVersion accessVersion;
    // CHECKSTYLE:OFF
    protected UcanaccessConnection ucanaccess;
    // CHECKSTYLE:ON
    private String user              = "ucanaccess";
    private Connection verifyConnection;
    private Boolean ignoreCase;

    private long inactivityTimeout = -1;
    private String columnOrder;
    private String append2JdbcURL    = "";
    private Boolean showSchema;

    protected UcanaccessBaseTest() {
    }

    protected final void setAccessVersion(AccessVersion _accessVersion) {
        accessVersion = _accessVersion;
    }

    protected void init(AccessVersion _accessVersion) throws SQLException {
        accessVersion = _accessVersion;
        try {
            Class.forName(UcanaccessDriver.class.getName());
        } catch (ClassNotFoundException _ex) {
            throw new RuntimeException(_ex);
        }
        ucanaccess = getUcanaccessConnection();
    }

    protected final AccessVersion getAccessVersion() {
        return accessVersion;
    }

    protected final FileFormat getFileFormat() {
        return accessVersion.getFileFormat();
    }

    protected final File getFileAccDb() {
        return fileAccDb;
    }

    public void checkQuery(String _query, Object[][] _expected) throws SQLException {
        try (Statement st = ucanaccess.createStatement();
            ResultSet rs = st.executeQuery(_query)) {
            diff(rs, _expected, _query);
        }
    }

    public void checkQuery(String _query) throws SQLException, IOException {
        initVerifyConnection();
        try (Statement st1 = ucanaccess.createStatement();
             Statement st2 = verifyConnection.createStatement()) {
            
            ResultSet firstRs = st1.executeQuery(_query);
            ResultSet verifyRs = st2.executeQuery(_query);

            diffResultSets(firstRs, verifyRs, _query);
        } finally {
            if (verifyConnection != null) {
                verifyConnection.close();
                verifyConnection = null;
            }
        }
    }

    public void checkQuery(String _query, Object... _expected) throws SQLException, IOException {
        checkQuery(_query, new Object[][] {_expected});
    }

    private void diff(ResultSet _resultSet, Object[][] _expectedResults, String _expression) throws SQLException {
        ResultSetMetaData rsMetaData = _resultSet.getMetaData();
        int mycolmax = rsMetaData.getColumnCount();
        if (_expectedResults.length > 0) {
            assertEquals(mycolmax, _expectedResults[0].length);
        }
        int j = 0;
        while (_resultSet.next()) {
            for (int i = 0; i < mycolmax; ++i) {
                assertTrue(j < _expectedResults.length, "Matrix with different length was expected: " + _expectedResults.length + " not" + j);
                Object actualObj = _resultSet.getObject(i + 1);
                Object expectedObj = _expectedResults[j][i];
                if (expectedObj == null) {
                    assertNull(actualObj);
                } else {
                    if (actualObj instanceof Blob) {

                        byte[] bt = getByteArray((Blob) actualObj);
                        byte[] btMtx = (byte[]) expectedObj;
                        for (int y = 0; y < btMtx.length; y++) {
                            assertEquals(btMtx[y], bt[y]);
                        }
                    } else {
                        if (actualObj instanceof Number && expectedObj instanceof Number) {
                            BigDecimal ob1b = new BigDecimal(actualObj.toString());
                            BigDecimal ob2b = new BigDecimal(expectedObj.toString());
                            actualObj = ob1b.doubleValue();
                            expectedObj = ob2b.doubleValue();
                        }
                        if (actualObj instanceof Date && expectedObj instanceof Date) {
                            actualObj = ((Date) actualObj).getTime();
                            expectedObj = ((Date) expectedObj).getTime();
                        }
                        assertEquals(expectedObj, actualObj, "Expected ob2 and ob1 to be equal in [" + _expression + "]");
                    }
                }
            }
            j++;
        }
        assertEquals(_expectedResults.length, j, "Matrix with different length was expected");
    }

    public void diffResultSets(ResultSet _resultSet, ResultSet _verifyResultSet, String _query) throws SQLException, IOException {
        ResultSetMetaData msMetaData = _resultSet.getMetaData();
        int mycolmax = msMetaData.getColumnCount();
        ResultSetMetaData verifyRsMetaData = _verifyResultSet.getMetaData();
        int jocolmax = verifyRsMetaData.getColumnCount();
        assertEquals(jocolmax, mycolmax);
        StringBuilder log = new StringBuilder("{");
        int row = 0;
        while (next(_verifyResultSet, _resultSet)) {
            row++;
            if (log.length() > 1) {
                log.append(",");
            }
            log.append("{");
            for (int i = 0; i < mycolmax; ++i) {
                if (i > 0) {
                    log.append(",");
                }
                Object ob1 = _resultSet.getMetaData().getColumnType(i + 1) == Types.BOOLEAN
                    ? _resultSet.getBoolean(i + 1)
                    : _resultSet.getObject(i + 1);
                Object ob2 = _verifyResultSet.getMetaData().getColumnType(i + 1) == Types.BOOLEAN
                    ? _verifyResultSet.getBoolean(i + 1)
                    : _verifyResultSet.getObject(i + 1);

                if (ob1 == null && ob2 == null) {
                    // both null, ok
                    assertNull(ob1);
                } else if (ob1 == null) {
                    assertNull(ob2, "Object in verify set at row:col " + row + ":" + (i + 1) + " should be null, but was: " + ob2 + " in [" + _query + "]");
                } else {
                    if (ob1 instanceof Blob) {
                        byte[] bt = getByteArray((Blob) ob1);
                        byte[] btodbc = getByteArray((Blob) ob1);
                        for (int y = 0; y < btodbc.length; y++) {
                            assertEquals(btodbc[y], bt[y]);
                        }
                    } else if (ob1 instanceof ComplexBase[] && ob2 instanceof ComplexBase[]) {
                        assertArrayEquals((ComplexBase[]) ob1, (ComplexBase[]) ob2);
                    } else {
                        if (ob1 instanceof Number && ob2 instanceof Number) {
                            BigDecimal ob1b = new BigDecimal(ob1.toString());
                            BigDecimal ob2b = new BigDecimal(ob2.toString());
                            ob1 = ob1b.doubleValue();
                            ob2 = ob2b.doubleValue();
                        }
                        if (ob1 instanceof Date && ob2 instanceof Date) {
                            ob1 = ((Date) ob1).getTime();
                            ob2 = ((Date) ob2).getTime();
                        }
                        assertEquals(ob1, ob2);
                    }
                }
            }
            log.append("}");
        }
        log.append("}");

    }

    public void dumpQueryResult(ResultSet _resultSet) throws SQLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true)) {
            new Main(ucanaccess, null).consoleDump(_resultSet, ps);
            getLogger().info("dumpQueryResult: {}", baos);
        }
    }

    public void dumpQueryResult(String _query) throws SQLException {
        try (Statement st = ucanaccess.createStatement()) {
            ResultSet resultSet = st.executeQuery(_query);
            dumpQueryResult(resultSet);
        }
    }

    protected final void dumpVerify(String _expression) throws SQLException {
        try (Statement st = verifyConnection.createStatement()) {
            ResultSet rs = st.executeQuery(_expression);
            dumpQueryResult(rs);
        }
    }

    /**
     * Subclasses may provide their own test database by overriding this method.
     *
     * @return valid classpath reference to a test database
     */
    protected String getAccessPath() {
        return null;
    }

    protected final String getAccessTempPath() {
        String accessPath = getAccessPath();
        if (accessPath == null) {
            fileAccDb = createTempFile(getClass().getSimpleName() + '-');
            createNewDatabase(getFileFormat(), fileAccDb);
        } else {
            fileAccDb = copyResourceToTempFile(accessPath);
            if (fileAccDb == null) {
                fileAccDb = new File(TEST_TEMP_DIR, accessPath);
                if (!fileAccDb.exists()) {
                    createNewDatabase(getFileFormat(), fileAccDb);
                    fileAccDb.deleteOnExit();
                }
            }
        }

        return fileAccDb.getAbsolutePath();
    }

    /**
     * Unique string based on current date/time to be used in names of temporary files.
     */
    private static final class TempFileNameString {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        private static final AtomicInteger COUNTER   = new AtomicInteger(1);
        private final String name;

        private TempFileNameString() {
            name = LocalDateTime.now().format(FORMATTER) + '_' + String.format("%03d", COUNTER.getAndIncrement());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Creates a unique temporary file name using the given prefix and suffix, but does not create the file.
     * @param _prefix file name prefix
     * @param _suffix file name suffix
     * @return temporary file object
     */
    protected static File createTempFileName(String _prefix, String _suffix) {
        String name = Optional.ofNullable(_prefix).map(p -> p.replace(File.separatorChar, '_')).orElse("");
        if (!name.isBlank() && !name.endsWith("-")) {
            name += "-";
        }
        name += new TempFileNameString() + _suffix;
        return new File(TEST_TEMP_DIR, name);
    }

    /**
     * Creates a unique temporary file name using the given prefix, but does not create the file.
     * @param _prefix file name prefix
     * @return temporary file object
     */
    protected File createTempFileName(String _prefix) {
        return createTempFileName(_prefix, getFileFormat().getFileExtension());
    }

    /**
     * Creates a unique temporary file using the given prefix.<p>
     * The file is marked for deletion on JVM exit.
     *
     * @param _prefix file name prefix
     * @return temporary file object
     */
    protected File createTempFile(String _prefix) {
        File f = createTempFileName(_prefix);

        try {
            Files.createFile(f.toPath());
            f.deleteOnExit();
            return f;
        } catch (IOException _ex) {
            throw new UncheckedIOException(_ex);
        }
    }

    void createNewDatabase(FileFormat _fileFormat, File _dbFile) {
        try (Database db = DatabaseBuilder.create(_fileFormat, _dbFile)) {
            db.flush();
            getLogger().info("Access {} file created: {}", _fileFormat.name(), _dbFile.getAbsolutePath());
        } catch (IOException _ex) {
            throw new UncheckedIOException(_ex);
        }
    }

    protected File copyResourceToTempFile(String _resourcePath) {
        File resourceFile = new File(_resourcePath);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(_resourcePath)) {
            if (is == null) {
                getLogger().warn("Resource {} not found in classpath", _resourcePath);
                return null;
            }
            File tempFile = createTempFile(resourceFile.getName().replace(".", "_"));
            getLogger().info("Copying resource '{}' to '{}'", _resourcePath, tempFile.getAbsolutePath());
            copyFile(is, tempFile.toPath());
            return tempFile;
        } catch (IOException _ex) {
            throw new UncheckedIOException(_ex);
        }
    }

    public int getCount(String _sql) throws SQLException {
        return getCount(_sql, true);
    }

    public int getCount(String _sql, boolean _equals) throws SQLException {
        initVerifyConnection();
        Statement st = verifyConnection.createStatement();
        ResultSet joRs = st.executeQuery(_sql);
        joRs.next();
        int count = joRs.getInt(1);
        st = ucanaccess.createStatement();
        ResultSet myRs = st.executeQuery(_sql);
        myRs.next();
        int myCount = myRs.getInt(1);
        if (_equals) {
            assertEquals(count, myCount);
        } else {
            assertNotEquals(count, myCount);
        }
        return count;
    }

    public String getName() {
        return getClass().getSimpleName() + " ver " + getFileFormat();
    }

    protected String getPassword() {
        return password;
    }

    protected UcanaccessConnection getUcanaccessConnection() throws SQLException {
        return getUcanaccessConnection(UcanaccessDriver.URL_PREFIX, getAccessTempPath());
    }

    protected UcanaccessConnection getUcanaccessConnection(String _dbPath) throws SQLException {
        return getUcanaccessConnection(UcanaccessDriver.URL_PREFIX, _dbPath);
    }

    private UcanaccessConnection getUcanaccessConnection(String _urlPrefix, String _dbPath) throws SQLException {
        if (_dbPath == null) {
            _dbPath = getAccessTempPath();
        }
        String url = _urlPrefix + _dbPath;
        if (ignoreCase != null) {
            url += ";ignoreCase=" + ignoreCase;
        }
        if (inactivityTimeout != -1) {
            url += ";inactivityTimeout=" + inactivityTimeout;
        } else {
            url += ";immediatelyreleaseresources=true";
        }
        if (columnOrder != null) {
            url += ";columnOrder=" + columnOrder;
        }
        if (showSchema != null) {
            url += ";showSchema=" + showSchema;
        }
        url += append2JdbcURL;
        return (UcanaccessConnection) DriverManager.getConnection(url, user, password);
    }

    protected void appendToJdbcURL(String s) {
        append2JdbcURL += s;
    }

    protected void initVerifyConnection() throws SQLException {
        File tempVerifyFile = createTempFile(fileAccDb.getName().replace(".", "_") + "_verify");
        copyFile(fileAccDb.toPath(), tempVerifyFile.toPath());

        if (verifyConnection != null) {
            verifyConnection.close();
            verifyConnection = null;
        }
        verifyConnection = getUcanaccessConnection(UcanaccessDriver.URL_PREFIX, tempVerifyFile.getAbsolutePath());
    }

    private boolean next(ResultSet _joRs, ResultSet _myRs) throws SQLException {
        boolean b1 = _joRs.next();
        boolean b2 = _myRs.next();
        assertEquals(b1, b2);
        return b1;
    }

    public void setInactivityTimeout(long _inactivityTimeout) {
        inactivityTimeout = _inactivityTimeout;
    }

    protected void setPassword(String _password) {
        password = _password;
    }

    protected void setColumnOrder(String _columnOrder) {
        columnOrder = _columnOrder;
    }

    public void setIgnoreCase(boolean _ignoreCase) {
        ignoreCase = _ignoreCase;
    }

    protected final void dropTable(String _tableName) throws SQLException {
        executeStatements("DROP TABLE " + _tableName);
    }

    protected final void executeStatements(String... _sqls) throws SQLException {
        try (Statement st = ucanaccess.createStatement()) {
            for (String sql : _sqls) {
                st.execute(sql);
            }
        }
    }

    /**
     * Execute the specified sql on the given statement logging the root cause of an exception encountered.
     */
    protected void executeStatement(Statement _statement, String _sql) {
        try {
            _statement.execute(_sql);
        } catch (SQLException _ex) {
            getLogger().warn("Exception executing [" + _sql + "].", _ex.getCause() != null ? _ex.getCause() : _ex);
        }
    }

    @AfterEach
    protected final void afterTestCaseBase() {
        if (ucanaccess != null) {
            try {
                if (!ucanaccess.isClosed()) {
                    ucanaccess.close();
                }
            } catch (SQLException _ex) {
                getLogger().warn("Database {} already closed: {}", fileAccDb, _ex);
            }
        }

        if (verifyConnection != null) {
            try {
                if (!verifyConnection.isClosed()) {
                    verifyConnection.close();
                }
            } catch (SQLException _ex) {
                getLogger().warn("Verify connection {} already closed: {}", verifyConnection, _ex);
            }
        }
    }

    private byte[] getByteArray(Blob _blob) throws SQLException {
        try (InputStream bs = _blob.getBinaryStream()) {
            return bs.readAllBytes();
        } catch (IOException _ex) {
            throw new UncheckedIOException("Failed to get byte array", _ex);
        }
    }
}
