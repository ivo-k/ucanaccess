package net.ucanaccess.commands;

import net.ucanaccess.converters.Persist2Jet;
import net.ucanaccess.jdbc.UcanaccessSQLException;

import java.io.IOException;
import java.sql.SQLException;

public class CreatePrimaryKeyCommand implements ICommand {
    private String tableName;
    private String execId;

    public CreatePrimaryKeyCommand(String _tableName, String _execId) {
        tableName = _tableName;
        execId = _execId;
    }

    @Override
    public String getExecId() {
        return execId;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TYPES getType() {
        return TYPES.DDL;
    }

    @Override
    public IFeedbackAction persist() throws SQLException {
        try {
            Persist2Jet p2a = new Persist2Jet();
            p2a.createPrimaryKey(tableName);
        } catch (IOException _ex) {
            throw new UcanaccessSQLException(_ex);
        }
        return null;
    }

    @Override
    public IFeedbackAction rollback() {
        return null;
    }
}
