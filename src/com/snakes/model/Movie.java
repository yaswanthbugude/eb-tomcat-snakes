package com.snakes.model;
import java.sql.*;
import java.util.ArrayList;
import java.lang.NullPointerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import java.io.File;
import java.io.IOException;

public class Movie extends Media {
  
  static Connection con = null;
  private String _name = "null";
  private Integer _imdb = null;
  private boolean _snakes;
  private static final Logger logger = LogManager.getLogger("snakes");
  static JsonFactory factory = new JsonFactory();

  public Movie(String name, Integer imdb, boolean snakes) {
     _name = name;
     _imdb = imdb;
     _snakes = snakes;
  }

  public String getName() {
    return this._name;
  }

  public String getImdb() {
    /* Pad integer imdb # with 0s */
    String imdbpadded = String.format("%07d", _imdb);
    return imdbpadded;
  }

  public String getSnakes() {
    String snakes = null;
    if (_snakes) {
      snakes = "Snakes";
    }
    else {
      snakes = "No Snakes";
    }
    return snakes;
  }


  public static Movie[] getMovies() {

    ArrayList<Movie> movies = new ArrayList<Movie>();

    try {
      con = getConnection();
      // If that fails, send dummy entries
      if (con == null) {
      logger.warn("Connection Failed!");
        Movie failed = new Movie("Connection Failed", 99999999, false);
        return new Movie[] { failed };
      }
      Statement stmt = con.createStatement();
      String sql = "SELECT * FROM movies";
      ResultSet rs = stmt.executeQuery(sql);

      while(rs.next()){
        // Retrieve by column name
        String name = rs.getString("name");
        Integer imdb = rs.getInt("imdb");
        boolean snakes = rs.getBoolean("snakes");
        Movie movie = new Movie(name, imdb, snakes);
        movies.add(movie);
      }
    }
    catch (SQLException e) {e.toString();}

    return movies.toArray(new Movie[movies.size()]);
  }

  public static Movie[] getMovies(String title) {

    ArrayList<Movie> movies = new ArrayList<Movie>();

    try {
      con = getConnection();
      // If that fails, send dummy entry
      if (con == null) {
      logger.warn("Connection Failed!");
        Movie failed = new Movie("Connection Failed", 99999999, false);
        return new Movie[] { failed };
      }
      Statement stmt = con.createStatement();
      String sql = null;
      if (title.matches(".*[^a-zA-Z0-9_\\s].*"))
        return new Movie[0];
      else
        sql = "SELECT * FROM movies WHERE UPPER(name) LIKE UPPER('"+title+"')";
      ResultSet rs = stmt.executeQuery(sql);

      while(rs.next()){
        // Retrieve by column name
        String name = rs.getString("name");
        Integer imdb = rs.getInt("imdb");
        boolean snakes = rs.getBoolean("snakes");
        Movie movie = new Movie(name, imdb, snakes);
        movies.add(movie);
      }
    }
    catch (SQLException e) {e.toString();}

    return movies.toArray(new Movie[movies.size()]);
  }

  public static void addMovie(String name, Integer imdb, boolean snakes) {
    con = getConnection();
    // If the connection is null, give up
    if (con == null) {
      return;
    }
    try {
      Statement create = con.createStatement();
      String insertRow1 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('"+name+"', '"+imdb+"', '"+snakes+"');";
      logger.trace("adding movie with statement: "+ insertRow1);
      create.addBatch(insertRow1);
      create.executeBatch();
      create.close();
    }
    catch (SQLException f) {f.toString();}
  }

  public static String deleteDatabase() {
    con = getConnection();
    try {
      logger.warn("Deleting Movie Database");
      Statement stmt = con.createStatement();
      Integer result  = stmt.executeUpdate("DROP TABLE IF EXISTS Movies;");
      logger.warn("Deleted Movie Database");
      return new String("Executed statement: " + result + " rows affected.");
    }
    catch (SQLException f) {f.toString();}
    return new String("Statement execution failed.");
  }

  private static void initDatabase() {
    // Retrieve the connection (it should already exist)
    con = getConnection();
    // If the connection is null, give up
    if (con == null) {
      return;
    }
    // Read movies table
    try {
      Statement stmt = con.createStatement();
      String sql = "SELECT * FROM Movies";
      ResultSet rs = stmt.executeQuery(sql);
    }
    // If the movies table doesn't exist, create it
    catch (SQLException e) {
      try {
        Statement create = con.createStatement();
        logger.warn("Initializing Database");
        String createTable = "CREATE TABLE Movies (Name char(50), IMDB integer, Snakes boolean);";
        String insertRow1 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Anaconda', '0118615', 'true');";
        String insertRow2 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Blade Runner', '0083658', 'true');";
        String insertRow3 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Casablanca', '0034583', 'false');";
        String insertRow4 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Dracula', '0103874', 'true');";
        String insertRow5 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Elysium', '1535108', 'true');";
        String insertRow6 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Fargo', '2802850', 'true');";
        String insertRow7 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Guardians of the Galaxy', '2015381', 'true');";
        String insertRow8 = "INSERT INTO Movies (Name, IMDB, Snakes) VALUES ('Halloween', '0077651', 'true');";

        create.addBatch(createTable);
        create.addBatch(insertRow1);
        create.addBatch(insertRow2);
        create.addBatch(insertRow3);
        create.addBatch(insertRow4);
        create.addBatch(insertRow5);
        create.addBatch(insertRow6);
        create.addBatch(insertRow7);
        create.addBatch(insertRow8);
        create.executeBatch();
        create.close();
        logger.warn("Initialized Database");
      }
      catch (SQLException f) {f.toString();}
    }
  }

  private static Connection getConnection() {
    // Return existing connection after first call
    if (con != null) {
      return con;
    }
    logger.trace("Getting database connection...");
    // Get RDS connection from environment properties provided by Elastic Beanstalk
    con = getRemoteConnection();
    // If that fails, attempt to connect to a local postgres server
    if (con == null) {
      con = getLocalConnection();
    }
    // If that fails, give up
    if (con == null) {
      return null;
    }
    // Attempt to initialize the database on first connection
    initDatabase();
    return con;
  }

  private static Connection getRemoteConnection() {
    /* Read database info from /tmp/database.json (advanced, more secure option)
    * - Requires database.config to be moved into .ebextensions folder and updated to 
    * point to a JSON file in an S3 bucket that the instance profile has permission to read.
    */
    try {
      /* Load the file and create a parser. If the project is not configured to store
      * database credentials in S3, fail out and try the next method.
      */
      File databaseConfig = new File("/tmp/database.json");
      JsonParser parser = factory.createParser(databaseConfig);
      // Load the Postgresql driver class
      Class.forName("org.postgresql.Driver");
      /* Read the first value in the JSON document with Jackson. This must be a full JDBC
      *  connection string a la jdbc:postgresql://hostname:port/dbName?user=userName&password=password
      */
      JsonToken jsonToken = null;
      while ( jsonToken != JsonToken.VALUE_STRING ) 
        jsonToken = parser.nextToken();
      String jdbcUrl = parser.getValueAsString();
      // Connect to the database
      logger.trace("Getting remote connection with url from database config file.");
      Connection con = DriverManager.getConnection(jdbcUrl);
      logger.info("Remote connection successful.");
      return con;
    }
    catch (IOException e) { logger.warn("Database configuration file not found. Checking environment variables.");}
    catch (ClassNotFoundException e) {e.toString();}
    catch (SQLException e) {e.toString();}

    // Read database info from environment variables (standard configration)
    if (System.getProperty("RDS_HOSTNAME") != null) {
      try {
      Class.forName("org.postgresql.Driver");
      String dbName = System.getProperty("RDS_DB_NAME");
      String userName = System.getProperty("RDS_USERNAME");
      String password = System.getProperty("RDS_PASSWORD");
      String hostname = System.getProperty("RDS_HOSTNAME");
      String port = System.getProperty("RDS_PORT");
      String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password;
      logger.trace("Getting remote connection with connection string from environment variables.");
      Connection con = DriverManager.getConnection(jdbcUrl);
      logger.info("Remote connection successful.");
      return con;
    }
    catch (ClassNotFoundException e) {e.toString();}
    catch (SQLException e) {e.toString();}
    }
    return null;
  }

  // Connect to a local database for development purposes
  private static Connection getLocalConnection() {
    try {
      Class.forName("org.postgresql.Driver");
      logger.info("Getting local connection");
      Connection con = DriverManager.getConnection(
            "jdbc:postgresql://localhost/snakes",
            "snakes",
            "sqlpassword");
      logger.info("Local connection successful.");
      return con;
    }
    catch (ClassNotFoundException e) {e.toString();}
    catch (SQLException e) {e.toString();}
    return null;
  }
}
