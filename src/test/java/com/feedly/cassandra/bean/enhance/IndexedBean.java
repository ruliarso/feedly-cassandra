package com.feedly.cassandra.bean.enhance;

import java.lang.reflect.Field;

import com.feedly.cassandra.anno.Column;
import com.feedly.cassandra.anno.ColumnFamily;
import com.feedly.cassandra.anno.RowKey;
import com.feedly.cassandra.bean.BeanUtils;

@ColumnFamily(name="indexedbean")
public class IndexedBean implements Comparable<IndexedBean>
{
    @RowKey
    private Long rowKey;
    
    @Column(col="s", indexed = true)
    private String strVal;
    
    @Column(indexed=true)
    private int intVal;

    @Column
    private long longVal;
    
    public Long getRowKey()
    {
        return rowKey;
    }

    public void setRowKey(Long rowKey)
    {
        this.rowKey = rowKey;
    }

    public String getStrVal()
    {
        return strVal;
    }

    public void setStrVal(String strVal)
    {
        this.strVal = strVal;
    }

    public int getIntVal()
    {
        return intVal;
    }

    public void setIntVal(int intVal)
    {
        this.intVal = intVal;
    }

    public long getLongVal()
    {
        return longVal;
    }

    public void setLongVal(long longVal)
    {
        this.longVal = longVal;
    }
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        for(Field f : getClass().getDeclaredFields())
        {
            try
            {
                b.append(f.getName() + ":" + f.get(this)).append(" ");
            }
            catch(Exception e)
            {
                return "error";
            }
        }
        
        return b.toString();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof IndexedBean)
            return BeanUtils.beanFieldsEqual(this, obj);

        return false;
    }

    @Override
    public int compareTo(IndexedBean o)
    {
        return rowKey.compareTo(o.rowKey);
    }

}