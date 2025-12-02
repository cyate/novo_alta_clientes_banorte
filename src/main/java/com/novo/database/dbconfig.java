//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.novo.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class dbconfig {
    String dbConfigFile = "";
    String dbName = "";
    String dbStatus = "NO";
    String connInit = "";
    String connHost = "";
    String connPort = "";
    String connDB = "";
    String connUser = "";
    String connPassword = "";
    String driverName = "";
    String connTypeDrv = "";
    String connParm = "";
    String connTimeOut = "";
    String dbLoadConfigTime = "";
    String dbLoadConfigError = "";
    String propFileName = "paths.properties";

    public dbconfig(String DirName, String Name) {
        this.dbName = Name;
        this.dbConfigFile = DirName + Name + ".properties";
        this.loadDbConfig();
    }

    public dbconfig(String PathConfig) {
        this.dbName = PathConfig;
        this.dbConfigFile = PathConfig;
        this.loadDbConfig();
    }

    public boolean dbConfigInit() {
        return this.dbStatus.equals("OK");
    }

    public int loadDbConfig() {
        int rc = 0;

        try {
            Path currentRelativePath = Paths.get("");
            String sep = System.getProperty("file.separator");
            String propertiesPath = currentRelativePath.toAbsolutePath().toString() + sep + ".." + sep + "parametros" + sep + this.propFileName;

            // 1) Si existe parametros\\paths.properties, usamos su "Path"
            if ((new File(propertiesPath)).exists()) {
                FileInputStream fileIn = new FileInputStream(propertiesPath);
                Properties props = new Properties();
                props.load(fileIn);
                String base = props.getProperty("Path", "./").trim();
                // Normalizamos el separador entre base y archivo
                if (!base.endsWith(sep)) {
                    base = base + sep;
                }
                propertiesPath = base + this.dbConfigFile;
            } else {
                // 2) Fallbacks robustos: intentamos en el directorio actual primero
                String candidate1 = currentRelativePath.toAbsolutePath().toString() + sep + this.dbConfigFile; // .\oracle.properties
                String candidate2 = currentRelativePath.toAbsolutePath().toString() + sep + ".." + sep + this.dbConfigFile; // ..\oracle.properties
                String catalinaHome = System.getProperty("catalina.home", currentRelativePath.toAbsolutePath().toString());
                String candidate3 = catalinaHome + sep + this.dbConfigFile;

                if ((new File(candidate1)).exists()) {
                    propertiesPath = candidate1;
                } else if ((new File(candidate2)).exists()) {
                    propertiesPath = candidate2;
                } else if ((new File(candidate3)).exists()) {
                    propertiesPath = candidate3;
                } else {
                    // Último recurso: mantener el comportamiento anterior pero corrigiendo separadores duplicados
                    propertiesPath = System.getProperty("catalina.home", "..") + sep + this.dbConfigFile;
                }
            }

            System.out.println("Path :" + propertiesPath);
            FileInputStream fileIn = new FileInputStream(propertiesPath);
            Properties props = new Properties();
            props.load(fileIn);
            this.connInit = props.getProperty("Init", "jdbc:oracle:thin:@").trim();
            this.connHost = props.getProperty("Host", "ORACLESERVER").trim();
            this.connPort = props.getProperty("Port", "1521").trim();
            this.connDB = props.getProperty("DB", "bvc").trim();
            this.connUser = props.getProperty("User", "user").trim();
            this.connPassword = this.decrypt(props.getProperty("Password", "password").trim());
            this.driverName = props.getProperty("driverName", "oracle.jdbc.driver.OracleDriver").trim();
            this.connTypeDrv = props.getProperty("TypeDrv", "2").trim();
            this.connParm = props.getProperty("Parm", "").trim();
            this.connTimeOut = props.getProperty("TimeOut", "11").trim();
            this.dbLoadConfigError = "";
            Locale locale = new Locale("es", "VE");
            DateFormat full = DateFormat.getDateTimeInstance(1, 1, locale);
            this.dbLoadConfigTime = full.format(new Date());
            this.dbStatus = "OK";
        } catch (Exception e) {
            this.dbLoadConfigError = e.getMessage();
            rc = -1;
            this.dbStatus = "ERR";
        }

        return rc;
    }

    private String decrypt(String text) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
        String key = "novopayment02017";
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        cipher.init(2, secretKey);
        byte[] cipherText = DatatypeConverter.parseBase64Binary(text);
        String decryptedString = new String(cipher.doFinal(cipherText), "UTF-8");
        return decryptedString;
    }
}
