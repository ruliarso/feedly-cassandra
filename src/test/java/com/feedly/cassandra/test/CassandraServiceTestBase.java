package com.feedly.cassandra.test;

import java.io.IOException;
import java.util.Collections;

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;

import org.apache.thrift.transport.TTransportException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.feedly.cassandra.PersistenceManager;

/**
 * runs an embedded cassandra instance.
 * 
 * @author kireet
 *
 */
public class CassandraServiceTestBase
{
    public static final String KEYSPACE = "TestKeyspace";

    private static EmbeddedCassandraService cassandra;
    protected static Cluster cluster;
    protected static Keyspace keyspace;
    protected static String snapshotFile;
    
    /**
     * Set embedded cassandra up and spawn it in a new thread.
     * 
     * @throws TTransportException
     * @throws IOException
     * @throws InterruptedException
     */
    @BeforeClass
    public static void beforeClass() throws TTransportException, IOException, InterruptedException
    {
        try
        {
            cassandra = new EmbeddedCassandraService();
            boolean started = cassandra.cleanStart(KEYSPACE, snapshotFile);
            
            if (started)
            {
                int retries = 5;
                
                boolean bootstrapped = false;
                while(!bootstrapped)
                {
                    try
                    {
                        cluster = HFactory.getOrCreateCluster("test-cluster", "localhost:8160");
                        KeyspaceDefinition keyspaceDefn = HFactory.createKeyspaceDefinition(KEYSPACE,
                                                                                            ThriftKsDef.DEF_STRATEGY_CLASS,
                                                                                            1,
                                                                                            Collections.<ColumnFamilyDefinition> emptyList());
                        
                        cluster.addKeyspace(keyspaceDefn, true);
                        keyspace = HFactory.createKeyspace(KEYSPACE, cluster);
                        
                        bootstrapped = true;
                    }
                    catch(HectorException ex)
                    {
                        if(retries == 0)
                            throw ex;
                        
                        retries--; //pause and then try again
                        Thread.sleep(1000);
                    }
                }
                
            }
        }
        catch(RuntimeException re)
        {
            re.printStackTrace();
            throw re;
        }
    }

    @AfterClass
    public static void afterClass()
    {
        cluster.getConnectionManager().shutdown();
        cassandra.stop();
    }
    
    
    public static void configurePersistenceManager(PersistenceManager pm)
    {
        pm.setClusterName(cluster.getName());
        pm.setKeyspaceName(KEYSPACE);
        pm.setHostConfiguration(new CassandraHostConfigurator("localhost:9160"));
    }

    public static void createColumnFamily(String family)
    {
        createColumnFamily(family, ComparatorType.ASCIITYPE);
    }
    
    public static void dropColumnFamily(String family)
    {
        cluster.dropColumnFamily(KEYSPACE, family, true);
    }
    
    public static void createColumnFamily(String family, ComparatorType ctype)
    {
        KeyspaceDefinition kdef = cluster.describeKeyspace(KEYSPACE);

        for(ColumnFamilyDefinition cdef : kdef.getCfDefs())
        {
            if(cdef.getName().equals(family))
            {
                if(cdef.getComparatorType().equals(ctype))
                    return;
                else
                    throw new IllegalStateException(String.format("Column Family %s exists, but existing comparator type %s doesn't match parameter %s", 
                                                                  family, cdef.getComparatorType(), ctype));
            }
        }

        ColumnFamilyDefinition cfDef = HFactory.createColumnFamilyDefinition(KEYSPACE, family, ctype);
        cluster.addColumnFamily(cfDef);    
    }
    
    @After
    public void deleteAll()
    {
        for(ColumnFamilyDefinition defn : cluster.describeKeyspace(KEYSPACE).getCfDefs())
            dropColumnFamily(defn.getName());
    }
    
}