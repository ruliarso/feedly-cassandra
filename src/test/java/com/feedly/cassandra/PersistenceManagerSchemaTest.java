package com.feedly.cassandra;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import me.prettyprint.hector.api.ddl.ColumnDefinition;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnIndexType;

import org.apache.cassandra.db.compaction.LeveledCompactionStrategy;
import org.apache.cassandra.io.compress.CompressionParameters;
import org.apache.cassandra.io.compress.DeflateCompressor;
import org.apache.cassandra.io.compress.SnappyCompressor;
import org.junit.Test;

import com.feedly.cassandra.anno.ColumnFamily;
import com.feedly.cassandra.entity.enhance.CompositeIndexedBean;
import com.feedly.cassandra.entity.enhance.CounterBean;
import com.feedly.cassandra.entity.enhance.IndexedBean;
import com.feedly.cassandra.entity.enhance.ListBean;
import com.feedly.cassandra.entity.enhance.ParentCounterBean;
import com.feedly.cassandra.entity.enhance.PartitionedIndexBean;
import com.feedly.cassandra.entity.enhance.SampleBean;
import com.feedly.cassandra.entity.enhance.SampleBean2;
import com.feedly.cassandra.entity.enhance.TtlBean;
import com.feedly.cassandra.entity.upd_enhance.SampleBean2Upgrade;
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
    public void testCounterTable()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {SampleBean.class.getPackage().getName()});
        pm.init();
        
        Set<String> counterTables = new HashSet<String>();
        for(ColumnFamilyDefinition cfdef : cluster.describeKeyspace(KEYSPACE).getCfDefs())
        {
            assertFalse(cfdef.getName().equals(CounterBean.class.getAnnotation(ColumnFamily.class).name()));
            if(cfdef.getName().endsWith("_cntr"))
                counterTables.add(cfdef.getName());
        }

        Set<String> expected = new HashSet<String>();
        
        expected.add(CounterBean.class.getAnnotation(ColumnFamily.class).name() + "_cntr");
        expected.add(ParentCounterBean.class.getAnnotation(ColumnFamily.class).name() + "_cntr");

        assertEquals(expected, counterTables);
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
    public void testHashIndexes()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {IndexedBean.class.getPackage().getName()});
        pm.init();
        
        assertHashIndexes();

        /*
         * rerun, make sure 
         */
        pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {IndexedBean.class.getPackage().getName()});
        pm.init();

        assertHashIndexes();
    }
    
    

    private void assertHashIndexes()
    {
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
    public void testRangeIndexes()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {IndexedBean.class.getPackage().getName()});
        pm.init();
        
        String indexBeanName = IndexedBean.class.getAnnotation(ColumnFamily.class).name();
        String ttlBeanName = TtlBean.class.getAnnotation(ColumnFamily.class).name();
        String compositeIndexBeanName = CompositeIndexedBean.class.getAnnotation(ColumnFamily.class).name();
        String partitionedIndexBeanName = PartitionedIndexBean.class.getAnnotation(ColumnFamily.class).name();
        
        boolean foundIndexBeanIdx = false, foundTtlBeanIdx = false, foundCompositeIndexBeanIdx = false, foundPartitionedIndexBeanIdx = false;
        boolean foundWal = false;
        
        for(ColumnFamilyDefinition cfdef : cluster.describeKeyspace(KEYSPACE).getCfDefs())
        {
            String name = cfdef.getName();
            if(name.endsWith("_idx"))
            {
                if(name.equals(indexBeanName + "_idx"))
                    foundIndexBeanIdx = true;
                else if(name.equals(compositeIndexBeanName + "_idx"))
                    foundCompositeIndexBeanIdx = true;
                else if(name.equals(partitionedIndexBeanName + "_idx"))
                    foundPartitionedIndexBeanIdx = true;
                else if(name.equals(ttlBeanName + "_idx"))
                    foundTtlBeanIdx = true;
                else
                    fail("unrecognized index table " + name);
            }
            else if(name.endsWith(PersistenceManager.CF_IDXWAL))
            {
                assertEquals(0, cfdef.getGcGraceSeconds());
                assertEquals(LeveledCompactionStrategy.class.getName(), cfdef.getCompactionStrategy());
                foundWal = true;
            }
        }
        
        assertTrue(foundCompositeIndexBeanIdx);
        assertTrue(foundIndexBeanIdx);
        assertTrue(foundTtlBeanIdx);
        assertTrue(foundWal);
        assertTrue(foundPartitionedIndexBeanIdx);
    }
    
    @Test
    public void testUpdate()
    {
        PersistenceManager pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        pm.setPackagePrefixes(new String[] {SampleBean2.class.getPackage().getName()});
        pm.init();
        
        pm = new PersistenceManager();
        configurePersistenceManager(pm);
        
        //points to same physical table as sample bean, should convert table to compressed and add an index on strVal
        pm.setPackagePrefixes(new String[] {SampleBean2Upgrade.class.getPackage().getName()});
        pm.init();
        
        String expected = SampleBean2.class.getAnnotation(ColumnFamily.class).name();
        for(ColumnFamilyDefinition cfdef : cluster.describeKeyspace(KEYSPACE).getCfDefs())
        {
            if(cfdef.getName().equals(expected))
            {
                 assertEquals(SnappyCompressor.class.getName(), cfdef.getCompressionOptions().get(CompressionParameters.SSTABLE_COMPRESSION));
                 assertEquals(3, cfdef.getColumnMetadata().size());
                 assertEquals(ColumnIndexType.KEYS, cfdef.getColumnMetadata().get(0).getIndexType());

                return;
            }
        }
    }

}
