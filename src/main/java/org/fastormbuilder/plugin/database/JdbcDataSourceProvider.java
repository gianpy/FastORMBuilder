package org.fastormbuilder.plugin.database;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.fastormbuilder.plugin.model.ConnectionProfile;
import org.fastormbuilder.plugin.model.DriverType;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class JdbcDataSourceProvider {
    private static final JdbcDataSourceProvider INSTANCE = new JdbcDataSourceProvider();

    public static JdbcDataSourceProvider getInstance() {
        return INSTANCE;
    }

    public DataSource create(ConnectionProfile profile) throws SQLException {
        DriverType type = profile.getDriverType();
        if (DriverType.MySQL.equals(type)) {
            MysqlDataSource ds = new MysqlDataSource();
            ds.setServerName(profile.getHost());
            ds.setPort(profile.getPort());
            ds.setUser(profile.getUserName());
            ds.setPassword(profile.getPassword());
            ds.setDatabaseName(profile.getDatabase());
            ds.setCharacterEncoding("utf-8");
            try {
                ds.setConnectTimeout(5000);
                ds.setAllowPublicKeyRetrieval(true);
            } catch (SQLException ignored) {
            }
            ds.setUseInformationSchema(true);
            ds.setUseSSL(false);
            return ds;
        } else if (DriverType.PostgreSQL.equals(type)) {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setUser(profile.getUserName());
            ds.setPassword(profile.getPassword());
            ds.setServerName(profile.getHost());
            ds.setPortNumber(profile.getPort());
            ds.setDatabaseName(profile.getDatabase());
            ds.setLoginTimeout(5);
            return ds;
        } else {
            String driverClass = (profile.getDriverClass() != null && !profile.getDriverClass().isEmpty())
                    ? profile.getDriverClass() : type.getDriverClass();
            GenericDataSource ds = new GenericDataSource(profile.getDriverLibrary(), driverClass);
            String url = profile.getUrl();
            if (url == null || url.isEmpty()) {
                url = type.getUrlPattern()
                        .replace("${host}", profile.getHost() != null ? profile.getHost() : "")
                        .replace("${port}", String.valueOf(profile.getPort() != null ? profile.getPort() : type.getDefaultPort()))
                        .replace("${db}", profile.getDatabase() != null ? profile.getDatabase() : "");
            }
            ds.setUrl(url);
            ds.setUser(profile.getUserName());
            ds.setPassword(profile.getPassword());
            ds.setLoginTimeout(5);
            return ds;
        }
    }
}
