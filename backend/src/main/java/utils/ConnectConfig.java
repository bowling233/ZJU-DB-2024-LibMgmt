package utils;

import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public final class ConnectConfig {

    private final String host;
    private final String port;
    private final String user;
    private final String password;
    private final String db;
    private final DatabaseType type;

    public ConnectConfig() throws NullPointerException, ClassNotFoundException, FileNotFoundException {
//        URL res = ConnectConfig.class.getClassLoader().getResource("application.yaml");
//        if (res == null) {
//            throw new NullPointerException();
//        }
//        BufferedReader br = new BufferedReader(new FileReader(res.getPath()));

        InputStream stream = ConnectConfig.class.getClassLoader().getResourceAsStream("application.yaml");
        if (stream == null) {
            throw new FileNotFoundException();
        }
        InputStreamReader isr = new InputStreamReader(stream);

        Yaml yaml = new Yaml();
        Map<String, Object> objectMap = yaml.load(isr);
        /* initialize all configures */
        host = (String) objectMap.getOrDefault("host", "localhost");
        port = (String) objectMap.getOrDefault("port", "3306");
        user = (String) objectMap.getOrDefault("user", "root");
        password = (String) objectMap.getOrDefault("password", "");
        db = (String) objectMap.getOrDefault("db", "library");
        type = DatabaseType.instance((String) objectMap.getOrDefault("type", "mysql"));
        /* load database connect driver */
        Class.forName(type.getDriverName());
    }

    @Override
    public String toString() {
        return "utils.ConnectConfig: {" + "host='" + host + '\'' +
                ", port='" + port + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", db='" + db + '\'' +
                ", type='" + type.toString() + '\'' +
                '}';
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDB() {
        return db;
    }

    public DatabaseType getType() {
        return type;
    }
}
