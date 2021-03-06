package com.feedly.cassandra.entity.upd_enhance;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.feedly.cassandra.anno.Column;
import com.feedly.cassandra.anno.ColumnFamily;
import com.feedly.cassandra.anno.RowKey;
import com.feedly.cassandra.anno.UnmappedColumnHandler;
import com.feedly.cassandra.entity.EntityUtils;
import com.feedly.cassandra.entity.enhance.SampleBean;

@ColumnFamily(name="sample2", compressed=true, forceCompositeColumns=true)
public class SampleBean2Upgrade implements Cloneable, Comparable<SampleBean2Upgrade>
{
    @RowKey
    private Long rowKey;
    
    @Column(name="s", hashIndexed=true)
    private String strVal;
    
    @Column(name="b", hashIndexed=true)
    private boolean boolVal2;
    
    @Column
    private int intVal;
    
    @Column(name="l")
    private long longVal;

    @Column
    private float floatVal;

    @Column(name="d")
    private double doubleVal;
    
    @Column
    private char charVal;
    
    @Column
    private boolean boolVal;

    @Column
    private Date dateVal;
    
    private int notSaved; //transient
    
    @UnmappedColumnHandler
    Map<String, Object> unmapped;
    
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

    public float getFloatVal()
    {
        return floatVal;
    }

    public void setFloatVal(float floatVal)
    {
        this.floatVal = floatVal;
    }

    public double getDoubleVal()
    {
        return doubleVal;
    }

    public void setDoubleVal(double doubleVal)
    {
        this.doubleVal = doubleVal;
    }

    public boolean getBoolVal()
    {
        return boolVal;
    }

    public void setBoolVal(boolean boolVal)
    {
        this.boolVal = boolVal;
    }

    public Date getDateVal()
    {
        return dateVal;
    }

    public void setDateVal(Date dateVal)
    {
        this.dateVal = dateVal;
    }

    public int getNotSaved()
    {
        return notSaved;
    }

    public void setNotSaved(int notSaved)
    {
        this.notSaved = notSaved;
    }

    public char getCharVal()
    {
        return charVal;
    }

    public void setCharVal(char charVal)
    {
        this.charVal = charVal;
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
        if(obj instanceof SampleBean)
            return EntityUtils.beanFieldsEqual(this, obj);

        return false;
    }

    @Override
    public int compareTo(SampleBean2Upgrade o)
    {
        return rowKey.compareTo(o.rowKey);
    }

    public Map<String, Object> getUnmapped()
    {
        return unmapped;
    }

    public void setUnmapped(Map<String, Object> unmapped)
    {
        this.unmapped = unmapped;
    }
    
    public Object clone() throws CloneNotSupportedException
    {
        SampleBean2Upgrade clone = (SampleBean2Upgrade) super.clone();
        clone.unmapped = new HashMap<String, Object>(unmapped);
        clone.dateVal = (Date) dateVal.clone();
               
        return clone;
    }

    public boolean getBoolVal2()
    {
        return boolVal2;
    }

    public void setBoolVal2(boolean boolVal2)
    {
        this.boolVal2 = boolVal2;
    }

}
