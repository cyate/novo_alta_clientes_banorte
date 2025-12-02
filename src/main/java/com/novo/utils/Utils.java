/*
 * Decompiled with CFR 0.151.
 * 
 * Could not load the following classes:
 *  org.apache.commons.logging.Log
 *  org.apache.commons.logging.LogFactory
 */
package com.novo.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Utils {
    private static String VERSION = "20200101";
    private static Log log = LogFactory.getLog((String)(Utils.class.getName() + "." + VERSION));
    private static final String MSG_ERROR_CONSULTA = "Error al ejecutar query";

    public static Properties getProperties(String nameFile) throws IOException {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config/" + nameFile));
        }
        catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return properties;
    }
}

