package net.ucanaccess.jdbc;

import static net.ucanaccess.util.SqlConstants.ORIGINAL_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Index.Column;
import com.healthmarketscience.jackcess.PropertyMap;
import com.healthmarketscience.jackcess.Table;
import net.ucanaccess.test.AccessVersion;
import net.ucanaccess.test.UcanaccessBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class CreateTableTest extends UcanaccessBaseTest {

    @Override
    protected String getAccessPath() {
        return TEST_DB_DIR + "badDb.accdb";
    }

    private void createAsSelect() throws SQLException {
        try (Statement st = ucanaccess.createStatement()) {
            st.executeUpdate("CREATE TABLE AAA_BIS as (SELECT baaaa,a,c FROM AAA) WITH DATA");
            Object[][] ver = {{"33A", 3, "G"}, {"33B", 111, "G"}};
            checkQuery("SELECT * FROM AAA_bis ORDER BY baaaa", ver);
            st.executeUpdate("CREATE TABLE AAA_quadris as (SELECT AAA.baaaa,AAA_BIS.baaaa as xxx FROM AAA,AAA_BIS) WITH DATA");
            dumpQueryResult("SELECT * FROM AAA_quadris ORDER BY baaaa");
        }
    }

    private void createAsSelect2() throws SQLException {
        try (Statement st = ucanaccess.createStatement()) {
            st.executeUpdate("CREATE TABLE AAA_TRIS as (SELECT baaaa,a,c FROM AAA) WITH NO DATA");
            st.execute("INSERT INTO AAA_TRIS SELECT * FROM AAA_bis");
            Object[][] ver = {{"33A", 3, "G"}, {"33B", 111, "G"}};
            checkQuery("SELECT * FROM AAA_tris ORDER BY baaaa", ver);
        }
    }

    private void createPs() throws SQLException {
        try (Statement st = ucanaccess.createStatement()) {
            st.executeUpdate("CREATE \nTABLE BBB (baaaa \nVARCHAR(2) PRIMARY KEY)");
            assertThatThrownBy(() -> st.execute("CREATE TABLE BBB (baaaa TEXT PRIMARY KEY, b TEXT)"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("object name already exists");
        }
    }

    private void createSimple() throws SQLException {
        Statement st = ucanaccess.createStatement();
        st.execute("INSERT INTO AAA(baaaa, c) VALUES ('33A', 'G')");
        st.execute("INSERT INTO AAA(baaaa, a, c) VALUES ('33B', 111, 'G')");
        Object[][] ver = {{"33A", 3, "G"}, {"33B", 111, "G"}};
        checkQuery("SELECT baaaa, a, c FROM AAA ORDER BY baaaa", ver);
        st.close();
    }

    void defaults() throws Exception {
        try (Statement st = ucanaccess.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT D, E FROM AAA");
            while (rs.next()) {
                assertNotNull(rs.getObject(1));
                assertNotNull(rs.getObject(2));
            }
            Database db = ucanaccess.getDbIO();
            Table tb = db.getTable("AAA");
            PropertyMap pm = tb.getColumn("d").getProperties();
            assertEquals("now()", pm.getValue(PropertyMap.DEFAULT_VALUE_PROP));
            PropertyMap pm1 = tb.getColumn("a").getProperties();
            assertEquals(true, pm1.getValue(PropertyMap.REQUIRED_PROP));
            tb = db.getTable("TBL");
            pm = tb.getColumn("NUMBER").getProperties();
            assertEquals("-4.6", pm.getValue(PropertyMap.DEFAULT_VALUE_PROP));
            assertEquals(true, pm.getValue(PropertyMap.REQUIRED_PROP));
            pm = tb.getColumn("BLANK").getProperties();
            assertEquals(" ", pm.getValue(PropertyMap.DEFAULT_VALUE_PROP));
        }
    }

    void setDPK() throws SQLException, IOException {
        Statement st1 = ucanaccess.createStatement();
        st1.execute("create table dkey(c counter , " + "number numeric(23,5) , " + " PRIMARY KEY (C,NUMBER))");
        st1.execute("create table dunique(c text , " + "number numeric(23,5) , " + " unique (C,NUMBER))");
        st1.close();
        ucanaccess.setAutoCommit(false);
        try (Statement st2 = ucanaccess.createStatement()) {
            st2.execute("INSERT INTO dunique values('ddl forces commit',2.3)");
            st2.execute("create table dtrx(c text , " + "number numeric(23,5) , " + " unique (C,NUMBER))");
            st2.execute("INSERT INTO dtrx values('I''ll be forgotten sob sob ',55555.3)");
            st2.execute("alter table dtrx ADD CONSTRAINT pk_dtrx PRIMARY KEY (c,number)");
        } catch (Exception _ex) {
            ucanaccess.rollback();
        }

        try (Statement st3 = ucanaccess.createStatement()) {
            st3.execute("INSERT INTO dtrx values('Hi all',444.3)");
            st3.execute("INSERT INTO dtrx values('Hi all',4454.3)");
        }

        dumpQueryResult("SELECT * FROM dtrx");
        dumpQueryResult("SELECT * FROM dunique");
        ucanaccess.commit();
        checkQuery("SELECT * FROM dunique");
        checkQuery("SELECT * FROM dtrx");
    }

    void setTableProperties() throws SQLException {
        try (Statement st = ucanaccess.createStatement()) {
            st.execute("create table tbl(c counter primary key , " + "number numeric(23,5) default -4.6 not null , "
                    + "txt1 text(23) default 'ciao', blank text default ' ', dt date default date(), txt2 text(33),"
                    + "txt3 text)");
        }
    }

    private void notNullBug() throws SQLException, IOException {
        try (Statement st = ucanaccess.createStatement()) {
            st.execute("create table nnb(c counter primary key , " + "number decimal (23,5) default -4.6 not null , "
                    + "txt1 text(23) not null, blank text , dt date not null, txt2 text ," + "txt3 text not null)");

            checkNotNull("nnb", "number", true);
            checkNotNull("nnb", "txt1", true);
            checkNotNull("nnb", "blank", false);
            checkNotNull("nnb", "dt", true);
            checkNotNull("nnb", "txt2", false);
            checkNotNull("nnb", "txt3", true);
        }
    }

    private void checkNotNull(String tn, String cn, boolean notNull) throws IOException {
        Database db = ucanaccess.getDbIO();
        Table tb = db.getTable(tn);
        PropertyMap pm = tb.getColumn(cn).getProperties();
        assertEquals(notNull, pm.getValue(PropertyMap.REQUIRED_PROP));

    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void testCreate(AccessVersion _accessVersion) throws Exception {
        init(_accessVersion);

        executeStatements(
            "CREATE \nTABLE AAA ( baaaa \ntext PRIMARY KEY,A long default 3 not null, C text(255) not null, "
                + "d DATETIME default now(), e text default 'l''aria')");

        createSimple();
        createPs();
        createAsSelect();
        createAsSelect2();
        setTableProperties();
        setDPK();
        defaults();
        notNullBug();

        dropTable("AAA");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void testNaming(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);

        Statement st = ucanaccess.createStatement();
        st.execute(
            " CREATE TABLE [ggg kk]( [---bgaaf aa] autoincrement PRIMARY KEY, [---bghhaaf b aa] text(222) default 'vvv')");
        st.execute(
            " CREATE TABLE [ggg kkff]( [---bgaaf() aa] autoincrement PRIMARY KEY, [---bghhaaf b aa()] text(222) default 'vvv')");
        st.execute(
            " CREATE TABLE [wHere12]( [where] autoincrement PRIMARY KEY, [---bghhaaf b aa] text(222) default 'vvv')");
        st.execute(" drop table [ggg kk]");
        st.execute(
            " CREATE TABLE [ggg kk]( [---bgaaf aa] autoincrement PRIMARY KEY, [---bghhaaf b aa] numeric(22,6) default 12.99)");
        st.execute(
            " CREATE TABLE kkk ( [---bgaaf aa] autoincrement PRIMARY KEY, [---bghhaaf b aa] text(222) default 'vvv')");
        st.execute(" INSERT INTO kkk([---bgaaf aa],[---bghhaaf b aa]) values(1,'23fff')");
        st.execute(" CREATE TABLE counter ( counter autoincrement PRIMARY KEY, [simple] text(222) default 'vvv')");
        st.close();

        dumpQueryResult("SELECT * FROM counter");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void testCreateWithFK(AccessVersion _accessVersion) throws SQLException, IOException {
        init(_accessVersion);

        try (Statement st = ucanaccess.createStatement()) {
            st.execute("CREATE TABLE Parent(x AUTOINCREMENT PRIMARY KEY, y text(222))");
            st.execute("CREATE TABLE Babe(k LONG, y LONG, PRIMARY KEY(k, y), FOREIGN KEY (y) REFERENCES Parent (x))");
            Database db = ucanaccess.getDbIO();
            Table tb = db.getTable("Babe");
            Table tbr = db.getTable("Parent");
            assertThat(tb.getForeignKeyIndex(tbr).getColumns().stream().map(Column::getName)).contains("y");
            st.execute("CREATE TABLE [1 Parent]( [x 0] long , y long, PRIMARY KEY([x 0],y))");
            st.execute("CREATE TABLE [1 Babe]( k LONG , y LONG, [0 z] LONG, PRIMARY KEY(k,y), FOREIGN KEY (y,[0 z] ) REFERENCES [1 Parent] ( [x 0] , y) )");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void testPs(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);

        PreparedStatement ps = ucanaccess.prepareStatement("CREATE TABLE PS (PS AUTOINCREMENT PRIMARY KEY)");
        ps.execute();
        ps = ucanaccess.prepareStatement("CREATE TABLE PS3 (PS AUTOINCREMENT PRIMARY KEY)", 0);
        ps.execute();
        ps = ucanaccess.prepareStatement("CREATE TABLE PS1 (PS AUTOINCREMENT PRIMARY KEY)", 0, 0);
        ps.execute();
        ps = ucanaccess.prepareStatement("CREATE TABLE PS2 (PS AUTOINCREMENT PRIMARY KEY)", 0, 0, 0);
        ps.execute();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void testPsHyphen(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);

        String ddl = "CREATE TABLE zzzFoo1 ([Req-MTI] TEXT(20))";
        // #9 hyphen in DDL column name confuses PreparedStatement
        PreparedStatement prepStmt = ucanaccess.prepareStatement(ddl);
        prepStmt.executeUpdate();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void testCreateHyperlink(AccessVersion _accessVersion) throws SQLException {
        init(_accessVersion);

        try (Statement st = ucanaccess.createStatement()) {
            st.execute("CREATE TABLE urlTest (id LONG PRIMARY KEY, website HYPERLINK)");
            st.execute("INSERT INTO urlTest (id, website) VALUES (1, '#http://whatever#')");
            st.execute("INSERT INTO urlTest (id, website) VALUES (2, 'example.com#http://example.com#')");
            st.execute("INSERT INTO urlTest (id, website) VALUES (3, 'the works#http://burger#with_bacon#and_cheese')");
            st.execute("INSERT INTO urlTest (id, website) VALUES (4, 'http://bad_link_no_hash_characters')");
            ResultSet rs1 = ucanaccess.getMetaData().getColumns(null, null, "urlTest", "website");
            rs1.next();
            assertEquals("HYPERLINK", rs1.getString(ORIGINAL_TYPE));
            ResultSet rs2 = st.executeQuery("SELECT website FROM urlTest ORDER BY id");
            rs2.next();
            assertEquals("http://whatever", rs2.getURL(1).toString());
            rs2.next();
            assertEquals("http://example.com", rs2.getURL(1).toString());
            rs2.next();
            assertEquals("http://burger#with_bacon", rs2.getURL(1).toString());
            rs2.next();
            assertThatThrownBy(() -> rs2.getURL(1))
                .isInstanceOf(UcanaccessSQLException.class)
                .hasMessageEndingWith("Invalid or unsupported URL format");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = AccessVersion.class, mode=Mode.INCLUDE, names = {"V2010"})
    void tableNameWithUnderscore(AccessVersion _accessVersion) throws SQLException {
        // Ticket #19
        init(_accessVersion);

        Statement st = ucanaccess.createStatement();
        st.execute("CREATE TABLE t01 (id LONG PRIMARY KEY, comments MEMO)");
        st.execute("CREATE TABLE t_1 (id LONG PRIMARY KEY)");
        st.close();
    }

}