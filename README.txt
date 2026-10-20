AGRICULTURAL EQUIPMENT RENTAL MANAGEMENT SYSTEM (JAVA CONSOLE + MYSQL)

Requirements:
- JDK 8 or later
- MySQL Server
- MySQL Connector/J (JDBC driver)

SETUP:
1. Open MySQL Workbench and run schema.sql.
2. Download MySQL Connector/J from:
   https://dev.mysql.com/downloads/connector/j/
3. Add the connector JAR to your Java project's classpath.
4. Open DBConnection.java and set your MySQL username/password.
5. Compile and run Main.java with the JDBC JAR on the classpath.

The console app supports:
- Register farmer/owner
- Add equipment
- List equipment
- Book equipment with date-overlap checking
- View bookings
- Approve/reject booking
- Record payment
- Complete rental
- Add maintenance record

For a college demo, passwords are stored as plain text only to keep the JDBC example simple.
Do not use this authentication approach in a real application; use password hashing.
