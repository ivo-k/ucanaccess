package net.ucanaccess.complex;

import com.healthmarketscience.jackcess.complex.ComplexDataType;
import com.healthmarketscience.jackcess.complex.ComplexValue;
import com.healthmarketscience.jackcess.complex.ComplexValueForeignKey;
import com.healthmarketscience.jackcess.impl.complex.ComplexColumnInfoImpl;
import net.ucanaccess.jdbc.UcanaccessSQLException;
import net.ucanaccess.jdbc.UcanaccessSQLException.ExceptionMessages;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public abstract class ComplexBase implements Serializable {
    private static final long           serialVersionUID = 1L;
    public static final ComplexValue.Id CREATE_ID        = ComplexColumnInfoImpl.INVALID_ID;

    private int                         id;
    private String                      tableName;
    private String                      columnName;

    protected ComplexBase(ComplexValue.Id _id, String _tableName, String _columnName) {
        id = _id.get();
        tableName = _tableName;
        columnName = _columnName;
    }

    protected ComplexBase(ComplexValue cv) {
        this(cv.getId(), cv.getComplexValueForeignKey().getColumn().getTable().getName(),
            cv.getComplexValueForeignKey().getColumn().getName());
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String _tableName) {
        tableName = _tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String _columnName) {
        columnName = _columnName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (columnName == null ? 0 : columnName.hashCode());
        result = prime * result + id;
        result = prime * result + (tableName == null ? 0 : tableName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ComplexBase other = (ComplexBase) obj;
        if (columnName == null) {
            if (other.columnName != null) {
                return false;
            }
        } else if (!columnName.equals(other.columnName)) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        return tableName == null ? other.tableName == null : tableName.equals(other.tableName);
    }

    public static Object[] convert(ComplexValueForeignKey fk) throws IOException, UcanaccessSQLException {
        if (fk.getComplexType().equals(ComplexDataType.ATTACHMENT)) {
            List<com.healthmarketscience.jackcess.complex.Attachment> lst = fk.getAttachments();
            Attachment[] lat = new Attachment[lst.size()];
            for (int i = 0; i < lat.length; i++) {
                lat[i] = new Attachment(lst.get(i));
            }
            return lat;
        }
        if (fk.getComplexType().equals(ComplexDataType.MULTI_VALUE)) {
            List<com.healthmarketscience.jackcess.complex.SingleValue> lst = fk.getMultiValues();
            SingleValue[] lat = new SingleValue[lst.size()];
            for (int i = 0; i < lat.length; i++) {
                lat[i] = new SingleValue(lst.get(i));
            }
            return lat;
        }

        if (fk.getComplexType().equals(ComplexDataType.VERSION_HISTORY)) {
            List<com.healthmarketscience.jackcess.complex.Version> lst = fk.getVersions();
            Version[] lat = new Version[lst.size()];
            for (int i = 0; i < lat.length; i++) {
                lat[i] = new Version(lst.get(i));
            }
            return lat;
        }
        if (fk.getComplexType().equals(ComplexDataType.UNSUPPORTED)) {
            List<com.healthmarketscience.jackcess.complex.UnsupportedValue> lst = fk.getUnsupportedValues();
            UnsupportedValue[] lat = new UnsupportedValue[lst.size()];
            for (int i = 0; i < lat.length; i++) {
                lat[i] = new UnsupportedValue(lst.get(i));
            }
            return lat;
        }
        throw new UcanaccessSQLException(ExceptionMessages.COMPLEX_TYPE_UNSUPPORTED);
    }

}
