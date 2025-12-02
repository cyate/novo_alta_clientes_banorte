/*
 * Decompiled with CFR 0.151.
 * 
 * Could not load the following classes:
 *  com.novo.database.dbinterface
 *  com.novo.exceptions.DatabaseErrorException
 *  com.novo.exceptions.InvalidStateException
 *  org.apache.commons.logging.Log
 *  org.apache.commons.logging.LogFactory
 */
package com.novo.main;

import com.novo.bean.Empresa;
import com.novo.bean.Tarjeta;
import com.novo.bean.Usuario;
import com.novo.database.dbinterface;
import com.novo.exceptions.DatabaseErrorException;
import com.novo.exceptions.InvalidStateException;
import com.novo.main.SendMail;
import com.novo.processor.ArchivoProcessor;
import com.novo.processor.CuentaMaestraProcessor;
import com.novo.processor.EmpresaProcessor;
import com.novo.processor.TarjetaProcessor;
import com.novo.processor.UsuarioProcessor;
import com.novo.processor.ValidadorProcessor;
import com.novo.utils.Utils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Main
extends ValidadorProcessor {
    private static Log log = LogFactory.getLog((String)Main.class.getName());
    private static final String CONSTANTS_QUERIES = "constant_queries.properties";
    private static final String CONSTANTS_CONFIG = "constant_config.properties";
    private static final String CONSTANTS_MAIL = "constant_mail.properties";
    // Nombre del archivo de propiedades de Oracle.
    // Nota: dbinterface ya antepone su propia ruta "parametros/"; por eso aquí solo pasamos el nombre.
    // Si se pasara "parametros/oracle.properties", terminaría duplicado como "parametros/parametros/oracle.properties".
    private static final String ORACLE_DESA = "oracle.properties";

    public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException, DatabaseErrorException, InvalidStateException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Properties pSql = Utils.getProperties(CONSTANTS_QUERIES);
        Properties pConfig = Utils.getProperties(CONSTANTS_CONFIG);
        Properties pMail = Utils.getProperties(CONSTANTS_MAIL);
        Properties pGeneral = Utils.getProperties("constant_process.properties");
        CuentaMaestraProcessor objCtaMaestraProcessor = new CuentaMaestraProcessor(pSql, pConfig);
        UsuarioProcessor objUsuarioProcessor = new UsuarioProcessor(pSql, pConfig);
        ArchivoProcessor objArchivoProcessor = new ArchivoProcessor(pSql, pConfig);
        TarjetaProcessor objTarjetaProcessor = new TarjetaProcessor(pSql, pConfig);
        ArrayList<Empresa> listaEmpresa = new ArrayList();
        ArrayList<Tarjeta> listaTarjeta = new ArrayList();
        Path directorio = Paths.get("", new String[0]);
        // Permite sobreescribir la ruta/archivo de configuración mediante -Doracle.config=... si es necesario
        String oracleConfigOverride = System.getProperty("oracle.config");
        String oracleConfigToUse = oracleConfigOverride != null && !oracleConfigOverride.trim().isEmpty()
                ? oracleConfigOverride.trim()
                : ORACLE_DESA;
        log.info((Object)("Cargando configuración Oracle desde: [" + oracleConfigToUse + "]"));
        dbinterface dboracle = new dbinterface(oracleConfigToUse);
        try {
            boolean flagVerificacion = Main.verifyFormatInterfaceAlta();
            if (!flagVerificacion) {
                log.info((Object)"El proceso se detuvo a causa de la lectura del archivo de entrada, puede que su nombre no este correcto");
                System.exit(0);
            } else {
                listaEmpresa = Main.getListEmpresa(pGeneral);
                listaTarjeta = Main.getListTarjeta(pGeneral, listaEmpresa);
                if (dboracle.dbinic() != 0) {
                    log.info((Object)"Conexi\u00f3n a BD ORACLE no disponible. Proceso cancelado");
                    System.exit(0);
                } else {
                    Main.verifyDataInterfaceAlta(dboracle, pSql, pGeneral, listaEmpresa, listaTarjeta);
                    Main.verifyExistNroCuenta(dboracle, pSql, pGeneral, listaEmpresa, listaTarjeta);
                    EmpresaProcessor objEmpresaProcessor = new EmpresaProcessor(pSql, pConfig, listaEmpresa);
                    dboracle.beginTransaction();
                    boolean f1 = objEmpresaProcessor.manageDataEmpresa(dboracle);
                    boolean f2 = objCtaMaestraProcessor.registerCuentaMaestra(dboracle, listaEmpresa);
                    boolean f3 = objUsuarioProcessor.registerUsuario(dboracle, listaEmpresa);
                    boolean f4 = objTarjetaProcessor.registerLotes(dboracle, listaEmpresa, listaTarjeta);
                    if (listaEmpresa != null && f1 && f2 && f3 && f4) {
                        objArchivoProcessor.generateInformationFiles(dboracle, listaEmpresa, listaTarjeta);

                        boolean sgcOk = Main.validateSgcFiles(pConfig);
                        if (!sgcOk) {
                            dboracle.rollback();
                            Main.enviarAlerta(pMail);
                            log.error((Object)"Validación de archivos SGC fallida. Proceso cancelado.");
                            dboracle.dbend();
                            System.exit(1);
                        }

                        Main.buildResponseFile(pConfig, listaEmpresa);

                        boolean respOk = Main.validateResponseFile(pConfig);
                        if (!respOk) {
                            dboracle.rollback();
                            Main.enviarAlerta(pMail);
                            log.error((Object)"Validación de archivo de respuesta fallida. Proceso cancelado.");
                            dboracle.dbend();
                            System.exit(1);
                        }

                        dboracle.commit();
                        if (pMail.getProperty("enviarMail").equalsIgnoreCase("S") && listaEmpresa.size() > 0) {
                            log.info((Object)"--------------------Notificaciones----------------------");
                            for (Empresa emp : listaEmpresa) {
                                if (!emp.getNvpMotivoSrv().equalsIgnoreCase("A") || !emp.getRespuesta().getNvpResultado().equalsIgnoreCase("A") || emp.getUsuario().isExist()) continue;
                                Main.enviarNotificaciones(pMail, emp.getUsuario());
                                log.info((Object)("--------------------Notificacion: " + emp.getUsuario().getNvpCorreoUsuario() + "----------------------"));
                            }
                        }
                        log.info((Object)"----------------Notificaciones Internas----------------------");
                        Main.enviarNotificacionInterna(pMail, listaEmpresa, listaTarjeta);
                    } else {
                        dboracle.rollback();
                        Main.enviarAlerta(pMail);
                    }
                    log.info((Object)"El Proceso Se Ha Culminado Exitosamente!");
                    dboracle.dbend();
                    System.exit(0);
                }
            }
        }
        catch (IOException e) {
            dboracle.rollback();
            dboracle.dbClose();
            log.fatal((Object)("Ha ocurrido un error: " + e));
        }
    }

    public static void sendNotification(Properties prop, String mensaje) {
        String smtpServ = prop.getProperty("smtpConfName");
        String sender = prop.getProperty("smtpConfSender");
        String mailUser = prop.getProperty("mailUser");
        String mailPasswd = prop.getProperty("mailPasswd");
        String asuntoMail = prop.getProperty("asuntoMail");
        int numAdr = Integer.valueOf(prop.getProperty("smtpNumberAdresses"));
        String[] recipients = new String[100];
        for (int i = 0; i < numAdr; ++i) {
            recipients[i] = prop.getProperty("smtpRecvAddress" + (i + 1));
        }
        SendMail.sendEmail(smtpServ, recipients, asuntoMail, mensaje, sender, mailUser, mailPasswd);
    }

    public static void buildResponseFile(Properties pConfig, ArrayList<Empresa> listaEmp) throws IOException {
        Properties p = Utils.getProperties(CONSTANTS_CONFIG);
        Date fecActual = new Date();
        String format = "yyMMdd";
        SimpleDateFormat objDateF = new SimpleDateFormat(format);
        String fecFormat = objDateF.format(fecActual);
        String nameFile = p.getProperty("FILE_NAME_OUTPUT").replaceAll("%%ODATE", fecFormat);
        String fileRute = p.getProperty("FILE_PATH_RESPUESTA") + nameFile + p.getProperty("FILE_EXT");
        int sizeListaArc = 0;
        try {
            for (int i = 0; i < listaArchivo.size(); ++i) {
                if (!((String)((List)listaArchivo.get(i)).get(0)).equals(pConfig.getProperty("FIELD_GENERAL"))) continue;
                sizeListaArc = ((List)listaArchivo.get(i)).size();
                for (int j = 0; j < listaEmp.size(); ++j) {
                    if (!((String)((List)listaArchivo.get(i)).get(1)).equals(listaEmp.get(j).getNvpIdServicioG()) || !((String)((List)listaArchivo.get(i)).get(2)).equals(listaEmp.get(j).getNvpNumStockG())) continue;
                    ((List)listaArchivo.get(i)).add(sizeListaArc, listaEmp.get(j).getRespuesta().getNvpResultado());
                    ((List)listaArchivo.get(i)).add(sizeListaArc + 1, listaEmp.get(j).getRespuesta().getNvpRespuesta());
                }
            }
            File oldFile = new File(fileRute);
            if (oldFile.exists()) {
                oldFile.delete();
            }
            File newFile = new File(fileRute);
            FileWriter fw = new FileWriter(newFile);
            for (int i = 0; i < listaArchivo.size(); ++i) {
                String row = "";
                for (int j = 0; j < ((List)listaArchivo.get(i)).size(); ++j) {
                    row = row + (String)((List)listaArchivo.get(i)).get(j) + "|";
                }
                row = row + "\r\n";
                fw.write(row);
            }
            fw.close();
        }
        catch (IOException e) {
            System.out.println(e);
            log.error((Object)e);
        }
    }

    public static void enviarNotificaciones(Properties prop, Usuario u) {
        String smtpServ = prop.getProperty("smtpConfName");
        String sender = prop.getProperty("smtpConfSender");
        String mailUser = prop.getProperty("mailUser");
        String mailPasswd = prop.getProperty("mailPasswd");
        String asuntoMail = prop.getProperty("asuntoMail");
        int numAdr = Integer.valueOf(prop.getProperty("smtpNumberAdresses"));
        String[] recipients = new String[100];
        if (prop.getProperty("enviarMailBackup").equalsIgnoreCase("S")) {
            for (int i = 0; i < numAdr; ++i) {
                recipients[i] = prop.getProperty("smtpRecvAddress" + (i + 1));
            }
            recipients[numAdr] = u.getNvpCorreoUsuario().trim();
        } else {
            recipients[0] = u.getNvpCorreoUsuario().trim();
        }
        String mensaje = prop.getProperty("TEXT_MAIL");
        mensaje = mensaje.replace("[NVP-USUARIO-BEM-BEP]", u.getNvpUsuarioBemBep()).replace("[NVP-CURP]", u.getNvpCurp()).replace("[NVP-NOMBRE USUARIO]", u.getNvpNombreUsuario()).replace("[NVP-CORREO-USUARIO]", u.getNvpCorreoUsuario());
        mensaje = mensaje.replace("\u00f3", "&oacute;").replace("\u00e1", "&aacute;").replace("\u00e9", "&eacute;").replace("\u00ed", "&iacute;").replace("\u00fa", "&uacute;");
        log.info((Object)mensaje);
        SendMail.sendEmailHtml(smtpServ, recipients, asuntoMail, mensaje, sender, mailUser, mailPasswd, prop.getProperty("IMAGE_PATH"));
    }

    public static void enviarAlerta(Properties prop) {
        Date objDate = new Date();
        String strDateFormat = "dd/MM/yyyy HH:mm:ss";
        SimpleDateFormat objSDF = new SimpleDateFormat(strDateFormat);
        String smtpServ = prop.getProperty("smtpConfName");
        String sender = prop.getProperty("smtpConfSender");
        String mailUser = prop.getProperty("mailUser");
        String mailPasswd = prop.getProperty("mailPasswd");
        String asuntoMail = prop.getProperty("asuntoMailAlert");
        int numAdr = Integer.valueOf(prop.getProperty("smtpNumberAdressesAlert"));
        String[] recipients = new String[100];
        for (int i = 0; i < numAdr; ++i) {
            recipients[i] = prop.getProperty("smtpRecvAddressAlert" + (i + 1));
        }
        String alerta = prop.getProperty("msj_alert_bd");
        String mensaje = "<style>.w {font-size:10.0pt;font-family:Verdana,sans-serif;padding:3.0pt 3.0pt 3.0pt 3.0pt;}th {font-size:10.5pt;font-family:Verdana,sans-serif;color:#EDEDED;background:#13469C;padding:3.0pt 3.0pt 3.0pt 3.0pt;}</style>";
        mensaje = mensaje + "</br></br>Fecha/hora: " + objSDF.format(objDate) + "</br>";
        mensaje = mensaje + "</br>Se notifica que se presentaron fallas en el proceso correspondiente.</br>";
        mensaje = mensaje + "</br><b>Observaciones:</b></br>";
        mensaje = mensaje + "</br><table border=\"1\" cellspacing=\"0\" cellpadding=\"0\"><tr><th style=\"width: 450px;\">Detalle</th></tr>";
        mensaje = mensaje + "<tr><td style=\"width: 450px;\">";
        mensaje = mensaje + "&nbsp;" + alerta.replace("\u00f3", "&oacute;").replace("\u00e1", "&aacute;").replace("\u00e9", "&eacute;").replace("\u00ed", "&iacute;").replace("\u00fa", "&uacute;");
        mensaje = mensaje + "</td></tr>";
        SendMail.sendEmailHtml(smtpServ, recipients, asuntoMail, mensaje, sender, mailUser, mailPasswd, prop.getProperty("IMAGE_PATH_INTERNO"));
    }

    public static void enviarNotificacionInterna(Properties prop, ArrayList<Empresa> listaEmp, ArrayList<Tarjeta> listaTarj) {
        int nroEmpresas = 0;
        int nroUsuarios = 0;
        int nroTarjetas = 0;
        int errores = 0;
        String observacionesMail = "";
        for (Empresa listaEmp1 : listaEmp) {
            if ("A".equals(listaEmp1.getRespuesta().getNvpResultado().trim()) && "A".equals(listaEmp1.getNvpMotivoSrv().trim()) && !listaEmp1.isRegistrarSoloLote()) {
                ++nroEmpresas;
                continue;
            }
            if (!"R".equals(listaEmp1.getRespuesta().getNvpResultado().trim())) continue;
            observacionesMail = observacionesMail + "<tr><td style=\"width: 550px;\">";
            observacionesMail = observacionesMail + "&nbsp;El Id Servicio: " + listaEmp1.getNvpIdServicioG() + " presenta un error" + listaEmp1.getRespuesta().getNvpDetalleResp();
            observacionesMail = observacionesMail + "</td></tr>";
            ++errores;
        }
        for (Empresa listaEmp1 : listaEmp) {
            if (!"A".equals(listaEmp1.getRespuesta().getNvpResultado().trim()) || !"A".equals(listaEmp1.getNvpMotivoSrv().trim()) || listaEmp1.getUsuario().isExist()) continue;
            ++nroUsuarios;
        }
        for (Tarjeta listaTarj1 : listaTarj) {
            if (!"A".equals(listaTarj1.getRespuesta().getNvpResultado().trim())) continue;
            ++nroTarjetas;
        }
        Date objDate = new Date();
        String strDateFormat = "dd/MM/yyyy HH:mm:ss";
        SimpleDateFormat objSDF = new SimpleDateFormat(strDateFormat);
        String smtpServ = prop.getProperty("smtpConfName");
        String sender = prop.getProperty("smtpConfSender");
        String mailUser = prop.getProperty("mailUser");
        String mailPasswd = prop.getProperty("mailPasswd");
        String asuntoMail = prop.getProperty("asuntoMailInterno");
        int numAdr = Integer.valueOf(prop.getProperty("smtpNumberAdresses"));
        int numAdrAlert = Integer.valueOf(prop.getProperty("smtpNumberAdressesAlert"));
        String[] recipients = new String[100];
        String observaciones = "";
        if (errores > 0) {
            for (int i = 0; i < numAdrAlert; ++i) {
                recipients[i] = prop.getProperty("smtpRecvAddressAlert" + (i + 1));
            }
            asuntoMail = prop.getProperty("asuntoMailAlert");
            observaciones = "</br><b>Observaciones:</b></br>";
            observaciones = observaciones + "</br><table border=\"1\" cellspacing=\"0\" cellpadding=\"0\"><tr><th style=\"width: 550px;\">Detalle</th></tr>";
            observaciones = observaciones + observacionesMail.replace("\u00f3", "&oacute;").replace("\u00e1", "&aacute;").replace("\u00e9", "&eacute;").replace("\u00ed", "&iacute;").replace("\u00fa", "&uacute;");
            String mensaje = "<style>.w {font-size:10.0pt;font-family:Verdana,sans-serif;padding:3.0pt 3.0pt 3.0pt 3.0pt;}th {font-size:10.5pt;font-family:Verdana,sans-serif;color:#EDEDED;background:#13469C;padding:3.0pt 3.0pt 3.0pt 3.0pt;}</style>";
            mensaje = mensaje + observaciones;
            SendMail.sendEmailHtml(smtpServ, recipients, asuntoMail, mensaje, sender, mailUser, mailPasswd, prop.getProperty("IMAGE_PATH_INTERNO"));
        } else {
            for (int i = 0; i < numAdr; ++i) {
                recipients[i] = prop.getProperty("smtpRecvAddress" + (i + 1));
            }
            String mensaje = "<style>.w {font-size:10.0pt;font-family:Verdana,sans-serif;padding:3.0pt 3.0pt 3.0pt 3.0pt;}th {font-size:10.5pt;font-family:Verdana,sans-serif;color:#EDEDED;background:#13469C;padding:3.0pt 3.0pt 3.0pt 3.0pt;}</style>";
            mensaje = mensaje + "</br></br>Fecha/hora: " + objSDF.format(objDate) + "</br>";
            mensaje = mensaje + "</br>Se notifica el resumen del proceso:</br>";
            mensaje = mensaje + "</br>Nro de empresas registradas: " + nroEmpresas + "</br>";
            mensaje = mensaje + "</br>Nro de usuarios registrados: " + nroUsuarios + "</br>";
            mensaje = mensaje + "</br>Nro de tarjetas registradas: " + nroTarjetas + "</br>";
            mensaje = mensaje + observaciones;
            SendMail.sendEmailHtml(smtpServ, recipients, asuntoMail, mensaje, sender, mailUser, mailPasswd, prop.getProperty("IMAGE_PATH_INTERNO"));
        }
    }

    private static boolean validateSgcFiles(Properties pConfig) {
        boolean ok = true;
        try {
            String list = pConfig.getProperty("LIST_FILES", "").trim();
            String base = pConfig.getProperty("FILE_PATH_SGC", "").trim();
            String ext = pConfig.getProperty("FILE_EXT", "").trim();
            if (list.isEmpty() || base.isEmpty() || ext.isEmpty()) {
                log.error((Object)"Parámetros de configuración para archivos SGC incompletos (LIST_FILES/FILE_PATH_SGC/FILE_EXT)");
                return false;
            }
            String[] files = list.split(",");
            for (String name : files) {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) continue;
                File f = new File(base + trimmed + ext);
                if (!f.exists()) {
                    log.error((Object)("Archivo de salida no encontrado: " + f.getAbsolutePath()));
                    ok = false;
                } else if (f.length() == 0) {
                    log.error((Object)("Archivo de salida vacío: " + f.getAbsolutePath()));
                    ok = false;
                } else {
                    log.info((Object)("Archivo generado OK: " + f.getAbsolutePath() + " (" + f.length() + " bytes)"));
                }
            }
        } catch (Exception ex) {
            log.error((Object)("Error validando archivos SGC: " + ex.getMessage()));
            ok = false;
        }
        return ok;
    }

    private static boolean validateResponseFile(Properties pConfig) {
        try {
            Date fecActual = new Date();
            String format = "yyMMdd";
            SimpleDateFormat objDateF = new SimpleDateFormat(format);
            String fecFormat = objDateF.format(fecActual);
            String nameFile = pConfig.getProperty("FILE_NAME_OUTPUT", "REPSOLNP.D%%ODATE").replaceAll("%%ODATE", fecFormat);
            String fileRute = pConfig.getProperty("FILE_PATH_RESPUESTA", "").trim() + nameFile + pConfig.getProperty("FILE_EXT", ".TXT").trim();
            File f = new File(fileRute);
            if (!f.exists()) {
                log.error((Object)("Archivo de respuesta no encontrado: " + f.getAbsolutePath()));
                return false;
            }
            if (f.length() == 0) {
                log.error((Object)("Archivo de respuesta vacío: " + f.getAbsolutePath()));
                return false;
            }
            log.info((Object)("Archivo de respuesta OK: " + f.getAbsolutePath() + " (" + f.length() + " bytes)"));
            return true;
        } catch (Exception ex) {
            log.error((Object)("Error validando archivo de respuesta: " + ex.getMessage()));
            return false;
        }
    }
}

