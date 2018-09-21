package org.hibernate.bugs;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import domain.MyData;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
public class JPAUnitTestCase extends BaseCoreFunctionalTestCase {

    private EntityManagerFactory entityManagerFactory;

    @Before
    public void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
    }

    @After
    public void destroy() {
        entityManagerFactory.close();
    }

    @Test
    public void storeLocalTimeShouldReturnWithAnUtcValue() throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        MyData myData = new MyData();
        myData.setId(1L);
        myData.setLocalTime(LocalTime.of(12, 0));

        entityManager.persist(myData);
        entityManager.getTransaction().commit();
        entityManager.close();

        Session s = openSession();
        Transaction tx = s.beginTransaction();

        s.doWork(connection -> {
            try (Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT local_time FROM avdev_myData")) {
                    while (rs.next()) {
                        String expectedLocalTimeValueInUtc = toUtc(myData.getLocalTime()).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        String storedLocalTimeValue = rs.getString(1);

                        Assert.assertEquals(expectedLocalTimeValueInUtc, storedLocalTimeValue);
                    }
                }
            }
        });

        tx.commit();
        s.close();
    }

    private LocalTime toUtc(LocalTime localTime) {
        return LocalDateTime.of(LocalDate.now(), localTime)
                .atZone(ZoneId.of("Europe/Paris"))
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalTime();
    }
}
