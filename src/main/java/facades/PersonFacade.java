package facades;

import dtos.PersonDTO;
import dtos.PersonsDTO;
import entities.Address;
import entities.Person;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.ws.rs.WebApplicationException;

/**
 *
 * Rename Class to a relevant name Add add relevant facade methods
 */
public class PersonFacade implements IPersonFacade {

    private static PersonFacade instance;
    private static EntityManagerFactory emf;
    
    private PersonFacade() {}
    

    public static PersonFacade getFacadeExample(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new PersonFacade();
        }
        return instance;
    }

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    
    public long getPersonCount(){
        EntityManager em = getEntityManager();
        try{
            long personCount = (long)em.createQuery("SELECT COUNT(r) FROM Person r").getSingleResult();
            return personCount;
        }finally{  
            em.close();
        } 
    }

    @Override
    public PersonDTO addPerson(String fName, String lName, String phone, String street, String zip, String city) throws WebApplicationException {
        
        EntityManager em = getEntityManager();
       
        if ((fName.length() == 0) || (lName.length() == 0)){
           throw new WebApplicationException("First Name and/or Last Name is missing", 400); 
        }
        
        Person person = new Person(fName, lName, phone, street, zip, city);
   
        List<Address> address = getSpecificAddressEntities(em, street, zip, city);

        if (address.size() > 0){
            person.setAddress(address.get(0)); // The address already exists
        }
        
        try {
            em.getTransaction().begin();
                em.persist(person);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return new PersonDTO(person);
    }
    
    @Override
    public PersonDTO editPerson(PersonDTO p) throws WebApplicationException {
        
        EntityManager em = getEntityManager();
        
        if ((p.getfName().length() == 0) || (p.getlName().length() == 0)){
           throw new WebApplicationException("First Name and/or Last Name is missing", 400); 
        }
        
        Person oldPerson = em.find(Person.class, p.getId());
        
        if (oldPerson == null) {
            throw new WebApplicationException(String.format("Person with id: (%d) not found", p.getId()), 400);
        }
        
        oldPerson.setFirstName(p.getfName());
        oldPerson.setLastName(p.getlName());
        oldPerson.setPhone(p.getPhone());
        oldPerson.setLastEdited();
        
        /* 
            * Vi antager at alle adresser er nogle vi tilføjer efterhånden som vi tilføjer personer til systemet.
            * Man må gerne fjerne en adresse hvis den ikke længere er knyttet til en person
            * En adresse må kun findes en gang. (street + zip + city) er unik. Dvs, at hvis flere personer
              bor på samme adresse, deler de adresse.
        
            1) Der er rettet i street, zip eller city og den nye adresse findes ikke i forvejen: 
                - ret direkte i adressen og opdater
            2) Der er rettet i street, zip eller city og den nye adresse findes i forvejen:
                - fjern den gamle adresse hvis den ikke er knyttet til andre
                - Knyt den fundne adresse til i stedet for den gamle (ændre reference)     
        */
        
        Address editAddress = new Address(p.getStreet(), p.getZip(), p.getCity());
        
        boolean same = sameAddress(p, oldPerson);
        List<Address> addressList = getSpecificAddressEntities(em, p.getStreet(), p.getZip(), p.getCity());
        List<Person> personsWithSameOldAddress = lookupPersonsWithSameAddress(em, oldPerson.getAddress().getId());
                
        try {
            em.getTransaction().begin();
          // dvs, at den nye adresse ikke allerede findes i addressListetabellen og at der ikke er andre som har den nuværende adresse
                if (addressList.isEmpty()){
                    if (personsWithSameOldAddress.size() == 1){
                        oldPerson.getAddress().setStreet(p.getStreet());
                        oldPerson.getAddress().setZip(p.getZip());
                        oldPerson.getAddress().setCity(p.getCity());
                    } else { 
                        oldPerson.setAddress(editAddress);       
                    } 
                    // Gitte flytter ind hos Hans
                    // Hans ændrer navn uden at ændre adresse
                } else {
                    if (!same){
                        Address addressToDelete = oldPerson.getAddress();
                        oldPerson.setAddress(addressList.get(0));
                        if (personsWithSameOldAddress.size() == 1){
                            em.remove(addressToDelete);
                    }    
                    }
                  }
                em.merge(oldPerson);   // adresseændringerne bliver gemt
            em.getTransaction().commit();
            return new PersonDTO(oldPerson);
        } finally {  
          em.close();
        }   
    }
    
    private List<Address> getSpecificAddressEntities(EntityManager em, String street, String zip, String city){
        Query query = em.createQuery("SELECT a FROM Address a WHERE a.street = :street AND a.zip = :zip AND a.city = :city");
        query.setParameter("street", street);
        query.setParameter("zip", zip);
        query.setParameter("city", city);
        return query.getResultList();
    }
    
    private List<Person> lookupPersonsWithSameAddress(EntityManager em, Long address_id ){
        Query query = em.createQuery("SELECT p FROM Person p JOIN p.address pa where p.address.id = :id");
        query.setParameter("id", address_id);
        return query.getResultList();
    }
      

    @Override
    public PersonDTO deletePerson(long id) throws WebApplicationException {
         EntityManager em = getEntityManager();
          Person person = em.find(Person.class, id);
          if (person == null) {
            throw new WebApplicationException(String.format("Person with id: (%d) not found", id), 404);
          }
       try {
           em.getTransaction().begin();
             em.remove(person);
           em.getTransaction().commit();
       } finally {
           em.close();
       }
       return new PersonDTO(person);
    }

    @Override
    public PersonDTO getPerson(long id) throws WebApplicationException {
       EntityManager em = getEntityManager();
       try {
           Person person = em.find(Person.class, id);
           if (person == null) {
                throw new WebApplicationException(String.format("Person with id: (%d) not found", id), 404);
            }
           return new PersonDTO(person);
       } finally {
           em.close();
       }
    }

    @Override
    public PersonsDTO getAllPersons() {
      EntityManager em = getEntityManager();
        try{
            return new PersonsDTO(em.createNamedQuery("Person.getAllRows").getResultList());
        }finally{  
            em.close();
        }   
    }

    private boolean sameAddress(PersonDTO p1, Person p2 ) {
         return p1.getStreet().equals(p2.getAddress().getStreet()) 
                 && p1.getZip().equals(p2.getAddress().getZip()) 
                 && p1.getCity().equals(p2.getAddress().getCity());
    }

}
