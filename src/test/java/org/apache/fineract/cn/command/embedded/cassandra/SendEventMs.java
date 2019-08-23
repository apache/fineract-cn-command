package org.apache.fineract.cn.command.embedded.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.fineract.cn.command.embedded.cassandra.Event;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

//@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration
@EmbeddedCassandra(timeout = 60000)
@CassandraDataSet(value = {"bootstrap_test.cql"}, keyspace = "w_onboarding")
public class SendEventMs {

    public SendEventMs() {
    }

    @Autowired
//    EventRepository eventRepository;



    @BeforeClass
    public static void startCassandraEmbedded() {
        try {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cluster cluster = Cluster.builder()
                .addContactPoints("127.0.0.1").withPort(9142).build();
        Session session = cluster.connect();
    }


    @AfterClass
    public static void stopCassandraEmbedded() {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }


    @Test
    public void saveEvent() {

        Event event = new Event(
                123,
                "test-event",
                "bs as",
                100000D,
                12312333D
        );

//        this.eventRepository.save(event);
    }

}
