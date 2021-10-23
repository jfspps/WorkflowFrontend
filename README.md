# Workflow frontend prototype #

This originally serves as a Spring-Security backed frontend to the Workflow engine (such as Camunda) though can be modified to interface with other web services as desired.

## HTML5 PUT/DELETE ##

HTML5 does not handle PUT and DELETE methods (only GET and POST). See note [here](https://linuxtut.com/en/b8b9965fddf9c5507517/) regarding
PUT/DELETE methods via Spring Boot 2.2. A filter is needed though this will reduce performance when certain content types are processed.

## Project structure ##

### Database info ###

There are two implementations of the JPA service: an in-memory H2 database with profile "H2" (needed to inject the database services and start the Dataloader) and a postgres datasource 
"postgres". To enable connection to PostgreSQL, mark the [application.properties](/src/main/resources/application.properties) file with the 'H2' and 'postgres'.

### Database tables and records ###

The data models are defined [here](src/main/java/company/model). 
The service methods are declared [here](src/main/java/company/services) 
interface, and then defined via JPA [here](src/main/java/company/services/springDataJPA/security).

A BootStrap class which populates the database is provided by [DataLoader](src/main/java/company/bootstrap/security).
 
### Security configuration ###

Initial security options (credentials, authorisation, session cookies and duration) are set in [/config/SecurityConfiguration](src/main/java/company/config/SecurityConfiguration.java) and the aforementioned DataLoader class.
 
PostGreSQL database network port, table, and other credentials are located in [application-postgres.yml](/src/main/resources/application-postgres.yml). This is enabled by setting @Profile to 'postgres'.