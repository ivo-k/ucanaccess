package net.ucanaccess.jdbc;

import net.ucanaccess.commands.DDLCommandEnlist;
import net.ucanaccess.converters.Metadata;
import net.ucanaccess.converters.SQLConverter;
import net.ucanaccess.converters.SQLConverter.DDLType;
import net.ucanaccess.jdbc.UcanaccessSQLException.ExceptionMessages;
import net.ucanaccess.util.HibernateSupport;
import net.ucanaccess.util.UcanaccessRuntimeException;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public abstract class AbstractExecute {
    protected enum CommandType {
        BATCH,
        NO_ARGUMENTS,
        PREPARED_STATEMENT,
        UPDATABLE_RESULTSET,
        WITH_AUTO_GENERATED_KEYS,
        WITH_COLUMNS_NAME,
        WITH_INDEXES
    }

    // CHECKSTYLE:OFF
    protected int               autoGeneratedKeys;
    protected String[]          columnNames;
    protected CommandType       commandType;
    protected int[]             indexes;
    protected String            sql;
    // CHECKSTYLE:ON
    private UcanaccessResultSet resultSet;

    private UcanaccessStatement statement;

    protected AbstractExecute(UcanaccessPreparedStatement _statement) {
        statement = _statement;
        commandType = CommandType.PREPARED_STATEMENT;
    }

    protected AbstractExecute(UcanaccessResultSet _resultSet) {
        resultSet = _resultSet;
        statement = _resultSet.getWrappedStatement();
        commandType = CommandType.UPDATABLE_RESULTSET;
    }

    protected AbstractExecute(UcanaccessStatement _statement) {
        statement = _statement;
    }

    protected AbstractExecute(UcanaccessStatement _statement, String _sql) {
        this(_statement);
        commandType = CommandType.NO_ARGUMENTS;
        sql = _sql;
    }

    protected AbstractExecute(UcanaccessStatement _statement, String _sql, int _autoGeneratedKeys) {
        this(_statement, _sql);
        autoGeneratedKeys = _autoGeneratedKeys;
        commandType = CommandType.WITH_AUTO_GENERATED_KEYS;
    }

    protected AbstractExecute(UcanaccessStatement _statement, String _sql, int[] _indexes) {
        this(_statement, _sql);
        indexes = _indexes;
        commandType = CommandType.WITH_INDEXES;
    }

    protected AbstractExecute(UcanaccessStatement _statement, String _sql, String[] _columnNames) {
        this(_statement, _sql);
        columnNames = _columnNames;
        commandType = CommandType.WITH_COLUMNS_NAME;
    }

    private Object enableDisable(DDLType _ddlType) throws SQLException, IOException {
        String tableName = _ddlType.getDBObjectName();
        if (tableName.startsWith("[") && tableName.endsWith("]")) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }
        UcanaccessConnection conn = statement.getConnection();
        Metadata mtd = new Metadata(conn.getHSQLDBConnection());
        String rtn = mtd.getTableName(tableName);
        if (rtn == null) {
            throw new UcanaccessSQLException(ExceptionMessages.TABLE_DOES_NOT_EXIST, tableName);
        }
        boolean enableAutoIncr = _ddlType.equals(DDLType.ENABLE_AUTOINCREMENT);
        conn.getDbIO().getTable(rtn).setAllowAutoNumberInsert(!enableAutoIncr);
        if (this instanceof Execute) {
            return false;
        } else {
            return 0;
        }
    }

    private int count(String tableName) throws SQLException {
        UcanaccessConnection conn = statement.getConnection();
        try (Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private SQLException checkDdlException() {
        UcanaccessConnection conn = statement.getConnection();
        try (PreparedStatement ps = conn.getHSQLDBConnection().prepareStatement(SQLConverter.convertSQL(sql).getSql())) {
            // hsqldb as parser by using an unexecuted PreparedStatement: my latest trick
            throw UcanaccessRuntimeException.featureNotSupported();
        } catch (SQLException _ex) {
            return _ex;
        }
    }

    private Object addDdlCommand() throws SQLException {
        Object ret;
        try {
            DDLType ddlType = SQLConverter.getDDLType(sql);
            if (ddlType == null) {
                throw checkDdlException();
            }

            if (DDLType.DROP_FOREIGN_KEY.equals(ddlType) && !HibernateSupport.isActive()) {
                throw new UnsupportedOperationException(
                    "DROP CONSTRAINT is only supported for Hibernate hbm2ddl.auto \"create\"");
            }

            if (DDLType.ADD_COLUMN.equals(ddlType)
                && SQLConverter.couldNeedDefault(ddlType.getColumnDefinition())) {
                String cn = ddlType.getSecondDBObjectName();
                String tn = ddlType.getDBObjectName();
                int count = count(ddlType.getDBObjectName());
                if (count > 0) {
                    throw new UcanaccessSQLException(ExceptionMessages.DEFAULT_NEEDED, cn, tn, count);
                }

            }

            String sql0 = ddlType.equals(DDLType.ADD_COLUMN)
                ? SQLConverter.convertSQL(SQLConverter.convertAddColumn(ddlType.getDBObjectName(),
                    ddlType.getSecondDBObjectName(), ddlType.getColumnDefinition())).getSql()
                : SQLConverter.convertSQL(sql).getSql();

            boolean enDis = ddlType.in(DDLType.ENABLE_AUTOINCREMENT, DDLType.DISABLE_AUTOINCREMENT);
            statement.setEnableDisable(enDis);
            if (enDis) {
                return enableDisable(ddlType);
            }

            String ddlExpr = null;
            if (ddlType.in(DDLType.CREATE_TABLE, DDLType.CREATE_TABLE_AS_SELECT)) {
                ddlExpr = SQLConverter.convertCreateTable(sql0);
            } else if (ddlType.equals(DDLType.CREATE_FOREIGN_KEY)) {
                String constraintName = ddlType.getSecondDBObjectName();
                if (constraintName == null) {
                    ddlExpr = sql0;
                } else {
                    if (constraintName.startsWith("[") && constraintName.endsWith("]")) {
                        constraintName = constraintName.substring(1, constraintName.length() - 1);
                    }
                    String tableName = ddlType.getDBObjectName();
                    if (tableName.startsWith("[") && tableName.endsWith("]")) {
                        tableName = tableName.substring(1, tableName.length() - 1);
                    }
                    UcanaccessConnection conn = statement.getConnection();
                    Metadata mtd = new Metadata(conn.getHSQLDBConnection());
                    tableName = mtd.getEscapedTableName(tableName);
                    if (tableName == null) {
                        throw new UcanaccessSQLException(ExceptionMessages.TABLE_DOES_NOT_EXIST, tableName);
                    }
                    ddlExpr = sql0.replaceFirst("(?i)\\s+ADD\\s+CONSTRAINT\\s+.*\\s+FOREIGN\\s+KEY\\s+",
                        " ADD CONSTRAINT \"" + tableName + "_" + constraintName.toUpperCase(Locale.US)
                            + "\" FOREIGN KEY ");
                }
            } else if (ddlType.equals(DDLType.DROP_FOREIGN_KEY)) {
                String constraintName = ddlType.getSecondDBObjectName();
                if (constraintName == null) {
                    throw new UcanaccessSQLException();
                } else {
                    if (constraintName.startsWith("[") && constraintName.endsWith("]")) {
                        constraintName = constraintName.substring(1, constraintName.length() - 1);
                    }
                    String tableName = ddlType.getDBObjectName();
                    if (tableName.startsWith("[") && tableName.endsWith("]")) {
                        tableName = tableName.substring(1, tableName.length() - 1);
                    }
                    UcanaccessConnection conn = statement.getConnection();
                    Metadata mtd = new Metadata(conn.getHSQLDBConnection());
                    tableName = mtd.getEscapedTableName(tableName);
                    if (tableName == null) {
                        throw new UcanaccessSQLException(ExceptionMessages.TABLE_DOES_NOT_EXIST, tableName);
                    }
                    ddlExpr = sql0.replaceFirst("(?i)\\s+DROP\\s+CONSTRAINT\\s+.*",
                        " DROP CONSTRAINT \"" + tableName + "_" + constraintName.toUpperCase(Locale.US) + "\"");
                }
            } else {
                ddlExpr = sql0;
            }

            ret = this instanceof Execute ? statement.getWrapped().execute(ddlExpr)
                : statement.getWrapped().executeUpdate(ddlExpr);

            DDLCommandEnlist ddle = new DDLCommandEnlist();
            ddle.enlistDDLCommand(SQLConverter.restoreWorkAroundFunctions(sql), ddlType);
        } catch (Exception _ex) {
            throw new SQLException(_ex);
        }
        return ret;
    }

    private boolean checkDDL() {
        return SQLConverter.checkDDL(sql);
    }

    public Object executeBase() throws SQLException {
        UcanaccessConnection conn = statement.getConnection();
        UcanaccessConnection.setCtxConnection(conn);

        if (commandType.equals(CommandType.BATCH)) {
            UcanaccessConnection.setCtxExecId(UcanaccessConnection.BATCH_ID);
        } else {
            UcanaccessConnection.setCtxExecId(Math.random() + "");
        }
        Object retv;

        if (checkDDL()) {
            retv = addDdlCommand();
        } else {
            try {
                retv = executeWrapped();
            } catch (SQLException _ex) {
                if (conn.getAutoCommit()) {
                    conn.rollback();
                }
                throw _ex;
            }
        }
        if (conn.getAutoCommit()) {
            conn.commit();
        }

        return retv;
    }

    public abstract Object executeWrapped() throws SQLException;

    ResultSet getWrappedResultSet() {
        return resultSet.getWrapped();
    }

    Statement getWrappedStatement() {
        return statement.getWrapped();
    }

    void setStatement(UcanaccessStatement _statement) {
        statement = _statement;
    }
}
