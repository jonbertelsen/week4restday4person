/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import dtos.PersonDTO;
import dtos.PersonsDTO;
import javax.ws.rs.WebApplicationException;

/**
 *
 * @author jobe
 */
public interface IPersonFacade {
  public PersonDTO addPerson(String fName, String lName, String phone, String street, String zip, String city) throws WebApplicationException;  
  public PersonDTO deletePerson(long id) throws WebApplicationException;
  public PersonDTO getPerson(long id) throws WebApplicationException; 
  public PersonsDTO getAllPersons();  
  public PersonDTO editPerson(PersonDTO p) throws WebApplicationException;  
}

