package com.feedly.cassandra;

import static org.junit.Assert.*;
import me.prettyprint.hector.api.ddl.ColumnDefinition;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnIndexType;

import org.apache.cassandra.io.compress.CompressionParameters;
import org.apache.cassandra.io.compress.DeflateCompressor;
import org.apache.cassandra.io.compress.SnappyCompressor;
import org.junit.Test;

import com.feedly.cassandra.anno.ColumnFamily;
import com.feedly.cassandra.bean.enhance.CompositeIndexedBean;
import com.feedly.cassandra.bean.enhance.IndexedBean;
import com.feedly.cassandra.bean.enhance.ListBean;
import com.feedly.cassandra.bean.enhance.SampleBean;
import com.feedly.cassandra.bean.upd_enhance.SampleBean2;
import com.feedly.cassandra.test.CassandraServiceTestBase;

public class PersistenceManagerSchemaTest extends CassandraServiceTestBase
{
    @Test
    public void testSimpleTable()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {SampleBean.class.getPackage().getName()});
        pm.init();
        
        String expected = SampleBean.class.getAnnotation(ColumnFamily.class).name();
        for(ColumnFamilyDefinition cfdef : cluster.describeKeyspace(KEYSPACE).getCfDefs())
        {
            if(cfdef.getName().equals(expected))
            {
                 assertTrue(cfdef.getCompressionOptions() == null || cfdef.getCompressionOptions().isEmpty());
                return;
            }
        }
        
        fail("SampleBean's table not found");
    }
    
    @Test
    public void testCompressionOptions()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {ListBean.class.getPackage().getName()});
        pm.init();
        
        String expected = ListBean.class.getAnnotation(ColumnFamily.class).name();
        for(ColumnFamilyDefinition cfdef : cluster.describeKeyspace(KEYSPACE).getCfDefs())
        {
            if(cfdef.getName().equals(expected))
            {
                
                assertEquals("8", cfdef.getCompressionOptions().get(CompressionParameters.CHUNK_LENGTH_KB));
                assertEquals(DeflateCompressor.class.getName(), cfdef.getCompressionOptions().get(CompressionParameters.SSTABLE_COMPRESSION));
                return;
            }
        }
        
        fail("SampleBean's table not found");
    }
    
    @Test
    public void testIndexes()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {IndexedBean.class.getPackage().getName()});
        pm.init();
        
        boolean foundIndexBean = false, foundCompositeIndexBean = false;
        for(ColumnFamilyDefinition cfdef : cluster.describeKeyspace(KEYSPACE).getCfDefs())
        {
            boolean isIndexBean = cfdef.getName().equals(IndexedBean.class.getAnnotation(ColumnFamily.class).name());
            boolean isCompositeIndexBean = cfdef.getName().equals(CompositeIndexedBean.class.getAnnotation(ColumnFamily.class).name());
            
            if(isIndexBean || isCompositeIndexBean)
            {
                assertEquals(2, cfdef.getColumnMetadata().size());
                for(ColumnDefinition col : cfdef.getColumnMetadata())
                {
                    assertEquals(ColumnIndexType.KEYS, col.getIndexType());
                }
            }
            
            foundIndexBean = foundIndexBean || isIndexBean;
            foundCompositeIndexBean = foundCompositeIndexBean || isCompositeIndexBean;
        }

        assertTrue(foundIndexBean);
        assertTrue(foundCompositeIndexBean);
    }

    @Test
    public void testUpdate()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {SampleBean.class.getPackage().getName()});
        pm.init();
        
        pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        //points to same physical table as sample bean, should convert table to compressed and add an index on strVal
        pm.setPackagePrefixes(new String[] {SampleBean2.class.getPackage().getName()});
        pm.init();
        
        String expected = SampleBean.class.getAnnotation(ColumnFamily.class).name();
        for(ColumnFamilyDefinition cfdef : cluster.describeKeyspace(KEYSPACE).getCfDefs())
        {
            if(cfdef.getName().equals(expected))
            {
                 assertEquals(SnappyCompressor.class.getName(), cfdef.getCompressionOptions().get(CompressionParameters.SSTABLE_COMPRESSION));
                 assertEquals(1, cfdef.getColumnMetadata().size());
                 assertEquals(ColumnIndexType.KEYS, cfdef.getColumnMetadata().get(0).getIndexType());

                return;
            }
        }
    }

}