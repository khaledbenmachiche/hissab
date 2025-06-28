# HISSAB

HISSAB is a Java EE application designed to help primary school children learn mathematics by evaluating arithmetic expressions. The application uses a component-based architecture with EJBs and SOAP web services, deployed on GlassFish with MySQL database support.

## Architecture

### Components:
- **EJB Module** (`hissab-ejb`): Contains business logic EJBs and JPA entities
- **Web Module** (`hissab-web`): SOAP web service endpoints
- **Client Module** (`hissab-client`): Swing GUI client application
- **Database**: MySQL with trace logging
- **Deployment**: Docker Compose with GlassFish and MySQL containers

### Key Features:
- Mathematical expression evaluation (e.g., "2+3*4")
- Simulated OCR processing for image-based math problems
- Calculation history logging with timestamps
- SOAP web service API
- Desktop GUI client
- Dockerized deployment

## Project Structure

```
hissab-app/
├── pom.xml                    # Parent Maven POM
├── docker-compose.yml         # Docker services configuration
├── database/
│   └── init.sql              # MySQL database initialization
├── docker/
│   └── glassfish/            # GlassFish configuration
├── hissab-ejb/               # EJB Module
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hissab/
│       │   ├── ejb/
│       │   │   ├── CalculEJB.java      # Expression evaluation
│       │   │   └── TraceEJB.java       # Database logging
│       │   └── entity/
│       │       └── Trace.java          # JPA entity
│       └── resources/META-INF/
│           └── persistence.xml         # JPA configuration
├── hissab-web/               # Web Module
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hissab/service/
│       │   ├── HissabService.java      # SOAP interface
│       │   └── HissabServiceImpl.java  # SOAP implementation
│       └── webapp/WEB-INF/
│           └── web.xml                 # Web configuration
└── hissab-client/            # Client Module
    ├── pom.xml
    └── src/main/java/com/hissab/client/
        ├── HissabClientGUI.java        # Swing GUI
        └── HissabServiceClient.java    # SOAP client
```

## Technologies Used

- **Java EE 8** - Enterprise application framework
- **EJB 3.2** - Enterprise JavaBeans for business logic
- **JPA 2.2** - Java Persistence API with EclipseLink
- **MySQL 8.0** - Database
- **GlassFish** - Application server
- **Docker & Docker Compose** - Containerization
- **Maven** - Build and dependency management
- **Swing** - Desktop GUI framework

## Setup and Deployment Steps

### Prerequisites:
- Java 11 or higher
- Maven 3.6+
- Docker and Docker Compose
- Git

### Step 1: Clone and Build the Project

##### Build all modules
```bash
mvn clean compile
```

### Step 2: Download MySQL JDBC Driver

##### Download MySQL Connector
```bash
wget https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-8.0.33.tar.gz
tar -xzf mysql-connector-java-8.0.33.tar.gz
cp mysql-connector-java-8.0.33/mysql-connector-java-8.0.33.jar /opt/glassfish
```

### Step 3: Start Docker Services

##### Start MySQL and GlassFish containers
```bash
docker-compose up -d
```

##### Check services status
```bash
docker-compose ps
```

##### View logs
```bash
docker-compose logs -f
```

### Step 4: Build and Package Applications

##### Build EJB module
```bash
cd hissab-ejb
mvn clean package
cd ..
```

##### Build Web module
```bash
cd hissab-web
mvn clean package
cd ..
```


### Step 5: Configure GlassFish

1. **Access GlassFish Admin Console:**
   - URL: http://localhost:4848
   - No authentication required for default setup

2. **Create JDBC Connection Pool:**
   - Navigate to: Resources → JDBC → JDBC Connection Pools
   - Click "New"
   - Pool Name: `HissabPool`
   - Resource Type: `javax.sql.DataSource`
   - Database Driver Vendor: `MySQL`
   - Click "Next"
   - Set properties:
     - `serverName`: `mysql`
     - `portNumber`: `3306`
     - `databaseName`: `hissab_db`
     - `user`: `hissab_user`
     - `password`: `hissab_password`
     - `url`: `jdbc:mysql://mysql:3306/hissab_db?useSSL=false&serverTimezone=UTC`
   - Click "Finish"

3. **Create JDBC Resource:**
   - Navigate to: Resources → JDBC → JDBC Resources
   - Click "New"
   - JNDI Name: `jdbc/hissabDS`
   - Pool Name: `HissabPool`
   - Click "OK"

4. **Deploy Applications:**
   - Navigate to: Applications
   - Click "Deploy"
   - Select and deploy `hissab-ejb-1.0-SNAPSHOT.jar`
   - Select and deploy `hissab-web-1.0-SNAPSHOT.war`

### Step 6: Verify Web Service

##### Check WSDL availability
```bash
curl http://localhost:8085/hissab/HissabService?wsdl
```

##### Test health check (if WSDL is available)
##### This will require a SOAP client or tool like SoapUI

### Step 7: Generate Client Stubs and Run GUI

##### Navigate to client module
```bash
cd hissab-client
```
##### Generate SOAP client stubs (after web service is deployed)
```bash
mvn jaxws:wsimport
```

##### Build client application
```bash
mvn clean compile
```

##### Run the GUI client
```bash
mvn exec:java -Dexec.mainClass="com.hissab.client.HissabClientGUI"
```
##### Or create executable JAR
```bash
mvn package
java -jar target/hissab-client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Testing the Application

### 1. Database Testing:
##### Connect to MySQL container
```bash
docker exec -it hissab-mysql mysql -u hissab_user -p hissab_db
```
##### Check initial data
```sql
SELECT * FROM trace;
```

### 2. SOAP Service Testing:
- Use SoapUI or similar tool to test the SOAP endpoints
- WSDL URL: `http://localhost:8080/hissab-web/HissabService?wsdl`

### 3. GUI Client Testing:
- Launch the Swing application
- Test expression calculation: `2+3*4`
- Test image upload (OCR simulation)
- Verify results are logged in database

## Web Service Operations

### calculateFromString
- **Input**: Mathematical expression string (e.g., "2+3*4")
- **Output**: Calculation result
- **Example**: `calculateFromString("2+3*4")` → `"14"`

### healthCheck
- **Input**: None
- **Output**: Service health status
- **Example**: `"Service is healthy. Test calculation: 1+1 = 2"`

## Database Schema

```sql
CREATE TABLE trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expression VARCHAR(255) NOT NULL,
    result VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Docker Services

- **MySQL**: Port 3306, accessible via phpMyAdmin on port 8081
- **phpMyAdmin**: Port 8081 (optional database management)

## Configuration Files

- `persistence.xml`: JPA configuration with MySQL connection
- `web.xml`: Web application configuration
- `docker-compose.yml`: Multi-container Docker setup
- `init.sql`: Database initialization script
