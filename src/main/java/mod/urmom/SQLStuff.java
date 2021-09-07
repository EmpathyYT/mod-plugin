package mod.urmom;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLStuff {
    private String host = "jdbc:sqlite::resource:modb.db";
    private String port = "3306";
    private String database = "shop";
    private String username = "root";
    private String password = "";

    private Connection connection;
    
    public boolean isConnected() {
        return (connection != null);
    }
    
    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected()) {
            connection = DriverManager.getConnection("jdbc:sqlite:shop.db");

        }
    }
    public void disconnect() throws ClassNotFoundException, SQLException {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    public Connection getConnection() {
        return  connection;
    }
    public void initializedbs() {
        PreparedStatement ps;
        try {

            ps = getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS urmom " +
                    "(UUID TEXT, INVFO TEXT, ARMOR TEXT)");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
