package facades;

import dtos.PersonDTO;
import entities.Person;
import utils.EMF_Creator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//Uncomment the line below, to temporarily disable this test
//@Disabled
public class PersonFacadeTest {
    
    private static Person p1, p2, p3;

    private static EntityManagerFactory emf;
    private static PersonFacade facade;

    public PersonFacadeTest() {
    }

    @BeforeAll
    public static void setUpClass() {
       emf = EMF_Creator.createEntityManagerFactoryForTest();
       facade = PersonFacade.getFacadeExample(emf);
    }

    @AfterAll
    public static void tearDownClass() {
//        Clean up database after test is done or use a persistence unit with drop-and-create to start up clean on every test
    }

    // Setup the DataBase in a known state BEFORE EACH TEST
    //TODO -- Make sure to change the code below to use YOUR OWN entity class
    @BeforeEach
    public void setUp() {
         EntityManager em = emf.createEntityManager(); 
        p1 = new Person("Jønke", "Jensen", "1212122","Ndr Frihavnsgade 29","2100","Kbh Ø");
        p2 = new Person("Jørgen", "Fehår", "3232222","Ndr Frihavnsgade 29","2100","Kbh Ø");
        p3 = new Person("Blondie", "Jensen", "323232","Storegade 3","3700","Rønne");

        try {
            em.getTransaction().begin();
            em.createNamedQuery("Person.deleteAllRows").executeUpdate(); 
            em.createNamedQuery("Address.deleteAllRows").executeUpdate();
            em.persist(p1);
            em.persist(p2);
            em.persist(p3);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @AfterEach
    public void tearDown() {
//        Remove any data after each test was run
    }

   // TODO: Delete or change this method 
    @Test
    public void testAllFacadeMethod() {
        assertEquals(3, facade.getPersonCount(), "Expects two rows in the database");
    }

    @Test
    public void testGetPerson() throws Exception {
        PersonDTO person = facade.getPerson(p1.getId());
        assertEquals("Jønke", person.getfName(), "Expects to find Jønke");
    }

    @Test
    public void testAddPerson() throws Exception {
        PersonDTO p;
        
        // Method one: testing for a known exception
        try {
            p = facade.addPerson("", "Petersen", "131212","","","");
        } catch (Exception e){
            assertThat(e.getMessage(), is("First Name and/or Last Name is missing"));
        }
        
        // Method two: testing for a known exception with assertion
        Assertions.assertThrows(Exception.class, () -> {
            final PersonDTO person = facade.addPerson("", "Petersen", "131212","","","");
        });
        
        p = facade.addPerson("Jon", "Snow", "2112211","Bygaden 28","2100","Kbh Ø");
        assertNotNull(p.getId());
        EntityManager em = emf.createEntityManager();
        try {
            List<Person> persons = em.createQuery("select p from Person p").getResultList();
            assertEquals(4, persons.size(), "Expects 4 persons in the DB");
        } finally {
            em.close();
        }
    }

    public static void setP1(Person p1) {
        PersonFacadeTest.p1 = p1;
    }

    @Test
    public void testDeletePerson() throws Exception {
        long p1Id = p1.getId();
        long p2Id = p2.getId();
        facade.deletePerson(p1Id);
        EntityManager em = emf.createEntityManager();
        try {
            List<Person> persons = em.createQuery("select p from Person p").getResultList();
            assertEquals(2, persons.size(), "Expects 2 persons in the DB");

            persons = em.createQuery("select p from Person p WHERE p.id = " + p1Id).getResultList();
            assertEquals(0, persons.size(), "Expects 2 persons in the DB");
            Person p = em.find(Person.class, p1Id);
            assertNull(p, "Expects that person is removed and p is null");

            p = em.find(Person.class, p2Id);
            assertNotNull(p, "Expects that person is removed and p is null");
        } finally {
            em.close();
        }
    }
    
    @Test
    public void testEditPerson() throws Exception {
        p3.setLastName("Hansen");
        PersonDTO p1New = facade.editPerson(new PersonDTO(p3));
        assertEquals(p1New.getlName(), p3.getLastName());
        assertNotEquals(p3.getLastName(),"Jensen");
    }
    
}
