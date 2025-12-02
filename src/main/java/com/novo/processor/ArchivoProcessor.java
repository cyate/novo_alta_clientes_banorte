/*
 * Decompiled with CFR 0.151.
 * 
 * Could not load the following classes:
 *  com.novo.database.dbinterface
 *  org.apache.commons.logging.Log
 *  org.apache.commons.logging.LogFactory
 */
package com.novo.processor;

import com.novo.bean.Empresa;
import com.novo.bean.Tarjeta;
import com.novo.database.dbinterface;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ArchivoProcessor {
    private static Log log = LogFactory.getLog((String)Process.class.getName());
    Properties pSql = null;
    Properties pConfig = null;

    public ArchivoProcessor(Properties pSql, Properties pConfig) {
        this.pSql = pSql;
        this.pConfig = pConfig;
    }

    public void generateInformationFiles(dbinterface dboracle, ArrayList<Empresa> listaEmpresa, ArrayList<Tarjeta> listaTarjeta) throws IOException, SQLException {
        String listPackArchivo = this.pConfig.getProperty("LIST_FILES").trim();
        String[] arrayPackArchivo = listPackArchivo.split(",");
        log.info((Object)"****Inicia la creacion los archivos de SGC **********");
        for (String arrayPackArchivos : arrayPackArchivo) {
            File file = new File(this.pConfig.getProperty("FILE_PATH_SGC").trim() + arrayPackArchivos + this.pConfig.getProperty("FILE_EXT").trim());
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter fw = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file), "Cp1252"));
            int inicial = 0;
            String nroCuenta = "";
            String codCliente = "";
            String codSucursal = "";
            String nameCompany = "";
            for (Tarjeta listaTarjetas : listaTarjeta) {
                if (!"A".equals(listaTarjetas.getRespuesta().getNvpResultado().trim())) continue;
                String dataRow = "";
                String customerId = "1" + listaTarjetas.getNvpNumeroTarjeta().trim().substring(1, 16);
                String nationalId = "";
                if ("I".equals(listaTarjetas.getNvpIndEmb().trim())) {
                    nationalId = this.pConfig.getProperty("NATIONAL_ID_INNO");
                } else {
                    String[] persona = this.getPersonaNominada(dboracle, listaTarjetas.getNvpIdServicioD().trim(), listaTarjetas.getNvpNumStockD().trim(), listaTarjetas.getNvpNumReg().trim());
                    nationalId = persona[4];
                }
                if (inicial == 0) {
                    nroCuenta = listaTarjetas.getNvpIdServicioD().trim().substring(10, 20);
                    codCliente = this.getCodigoCliente(dboracle, nroCuenta);
                    codCliente = String.format("%6s", codCliente).replace(' ', '0');
                    codSucursal = this.getCodigoSucursal(dboracle, nroCuenta);
                    codSucursal = String.format("%6s", codSucursal).replace(' ', '0');
                    nameCompany = this.getNameCompany(listaEmpresa, listaTarjetas.getNvpIdServicioD().trim());
                } else if (!listaTarjeta.get(inicial).getNvpIdServicioD().equals(listaTarjeta.get(inicial - 1).getNvpIdServicioD())) {
                    nroCuenta = listaTarjetas.getNvpIdServicioD().trim().substring(10, 20);
                    codCliente = this.getCodigoCliente(dboracle, nroCuenta);
                    codCliente = String.format("%6s", codCliente).replace(' ', '0');
                    codSucursal = this.getCodigoSucursal(dboracle, nroCuenta);
                    codSucursal = String.format("%6s", codSucursal).replace(' ', '0');
                    nameCompany = this.getNameCompany(listaEmpresa, listaTarjetas.getNvpIdServicioD().trim());
                }
                if ("ACCOUNTS".equalsIgnoreCase(arrayPackArchivos)) {
                    dataRow = this.buildAccountsFiles(dataRow, listaTarjetas.getNvpNumeroTarjeta().trim(), nroCuenta, codCliente, codSucursal);
                } else if ("CARDACCOUNTS".equalsIgnoreCase(arrayPackArchivos)) {
                    dataRow = this.buildCardAccountsFiles(dataRow, listaTarjetas.getNvpNumeroTarjeta().trim());
                } else if ("CARDS".equalsIgnoreCase(arrayPackArchivos)) {
                    dataRow = this.buildCardsFiles(dataRow, listaTarjetas.getNvpNumeroTarjeta().trim(), listaTarjetas.getNvpFecExpiracion().trim(), customerId, nroCuenta);
                } else if ("CUSTOMERACCOUNTS".equalsIgnoreCase(arrayPackArchivos)) {
                    dataRow = this.buildCustomerAccountsFiles(dataRow, listaTarjetas.getNvpNumeroTarjeta().trim(), customerId);
                } else if ("CUSTOMERS".equalsIgnoreCase(arrayPackArchivos)) {
                    String name = this.pConfig.getProperty("C1_FIRST_NAME").trim();
                    String lastname = this.pConfig.getProperty("C1_LAST_NAME").trim();
                    String nameCard = this.pConfig.getProperty("C1_NAME_ON_CARD").trim();
                    if ("N".equals(listaTarjetas.getNvpIndEmb().trim())) {
                        name = listaTarjetas.getNvpNombreEmpLar().trim();
                        lastname = listaTarjetas.getNvpApellidoPat().trim();
                        nameCard = listaTarjetas.getNvpEmb1().trim();
                    }
                    dataRow = this.buildCustomersFiles(dataRow, listaTarjetas.getNvpNumeroTarjeta().trim(), name, lastname, nameCard, nameCompany, customerId, nationalId);
                } else if ("ACCOUNTBALANCES".equalsIgnoreCase(arrayPackArchivos)) {
                    dataRow = this.buildAccountBalancesFiles(dataRow, listaTarjetas.getNvpNumeroTarjeta().trim());
                }
                dataRow = dataRow + "\r\n";
                String dataRowCode = new String(dataRow.getBytes("Cp1252"));
                fw.write(dataRowCode);
                ((Writer)fw).flush();
                ++inicial;
            }
            ((Writer)fw).close();
        }
        log.info((Object)"****Finaliza la creacion los archivos de SGC **********");
    }

    public String buildAccountsFiles(String dataRow, String accountId, String nroCuenta, String codCliente, String codSucursal) {
        String extendedField = this.pConfig.getProperty("EXTENDED_FIELDS").trim().replaceAll("NVP-NUM-EMPRESA", nroCuenta).replaceAll("COD_SUCURSAL_EMPRESA", codSucursal).replaceAll("COD_CLIENTE_EMPRESA", codCliente);
        String rowAccount = this.pConfig.getProperty("ACCOUNTS_FILE").trim();
        dataRow = rowAccount.replaceAll("ACTION", this.pConfig.getProperty("ACTION").trim()).replaceAll("ACCOUNT_ID", accountId).replaceAll("ACCOUNT_TYPE", this.pConfig.getProperty("ACCOUNT_TYPE").trim()).replaceAll("CURRENCY_CODE", this.pConfig.getProperty("CURRENCY_CODE").trim()).replaceAll("EXTENDED_FIELDS", extendedField);
        return dataRow;
    }

    public String buildCardAccountsFiles(String dataRow, String accountId) {
        String rowAccount = this.pConfig.getProperty("CARDACCOUNTS_FILE").trim();
        dataRow = rowAccount.replaceAll("ACTION", this.pConfig.getProperty("ACTION").trim()).replaceAll("CARD_PAN", accountId).replaceAll("ACCOUNT_ID", accountId).replaceAll("ACCOUNT_TYPE_NOMINATED", this.pConfig.getProperty("ACCOUNT_TYPE_NOMINATED").trim()).replaceAll("ACCOUNT_TYPE_QUALIFIER", this.pConfig.getProperty("ACCOUNT_TYPE_QUALIFIER").trim()).replaceAll("ACCOUNT_TYPE", this.pConfig.getProperty("ACCOUNT_TYPE").trim());
        return dataRow;
    }

    public String buildCardsFiles(String dataRow, String accountId, String expfecha, String customerId, String nroCuenta) {
        Date fecActual = new Date();
        String format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat objDateF = new SimpleDateFormat(format);
        String fechaExpiracion = expfecha.substring(2, 4) + expfecha.substring(0, 2);
        String rowAccount = this.pConfig.getProperty("CARDS_FILE").trim();
        dataRow = rowAccount.replaceAll("ACTION", this.pConfig.getProperty("ACTION").trim()).replaceAll("CARD_PAN", accountId).replaceAll("CARD_PROGRAM", this.pConfig.getProperty("CARD_PROGRAM").trim()).replaceAll("DEFAULT_ACCOUNT_TYPE", this.pConfig.getProperty("DEFAULT_ACCOUNT_TYPE").trim()).replaceAll("CARD_STATUS", this.pConfig.getProperty("CARD_STATUS").trim()).replaceAll("CUSTOM_STATE", this.pConfig.getProperty("CUSTOM_STATE").trim()).replaceAll("NVP-FEC-EXPIRACION", fechaExpiracion).replaceAll("MAILER_DESTINATION", this.pConfig.getProperty("MAILER_DESTINATION").trim()).replaceAll("COMPANY_CARD", this.pConfig.getProperty("COMPANY_CARD").trim()).replaceAll("DATE_ISSUED", objDateF.format(fecActual)).replaceAll("CUSTOMER_ID", customerId).replaceAll("COMPANY_ID", this.pConfig.getProperty("COMPANY_ID").trim().replaceAll("NVP-NUM-EMPRESA", nroCuenta));
        return dataRow;
    }

    public String buildCustomerAccountsFiles(String dataRow, String accountId, String customerId) {
        String rowAccount = this.pConfig.getProperty("CUSTOMERACCOUNTS_FILE").trim();
        dataRow = rowAccount.replaceAll("ACTION", this.pConfig.getProperty("ACTION").trim()).replaceAll("CUSTOMER_ID", customerId).replaceAll("ACCOUNT_ID", accountId).replaceAll("ACCOUNT_TYPE", this.pConfig.getProperty("ACCOUNT_TYPE").trim());
        return dataRow;
    }

    public String buildCustomersFiles(String dataRow, String accountId, String firstName, String lastName, String onCard, String nameCompany, String customerId, String nationalId) {
        String nameOnCard = "";
        if (firstName.isEmpty() && lastName.isEmpty()) {
            nameOnCard = this.pConfig.getProperty("C1_NAME_ON_CARD").trim();
        } else if (!firstName.isEmpty() || !lastName.isEmpty()) {
            nameOnCard = onCard;
        }
        if (firstName.isEmpty()) {
            firstName = this.pConfig.getProperty("C1_FIRST_NAME").trim();
        }
        if (lastName.isEmpty()) {
            lastName = this.pConfig.getProperty("C1_LAST_NAME").trim();
        }
        String rowAccount = this.pConfig.getProperty("CUSTOMERS_FILE").trim();
        dataRow = rowAccount.replaceAll("ACTION", this.pConfig.getProperty("ACTION").trim()).replaceAll("CUSTOMER_ID", customerId).replaceAll("NATIONAL_ID", nationalId).replaceAll("C1_FIRST_NAME", firstName).replaceAll("C1_LAST_NAME", lastName).replaceAll("C1_NAME_ON_CARD", nameOnCard).replaceAll("COMPANY_NAME", nameCompany.substring(0, 26).trim());
        return dataRow;
    }

    public String buildAccountBalancesFiles(String dataRow, String accountId) {
        String rowAccount = this.pConfig.getProperty("ACCOUNTBALANCES_FILE").trim();
        dataRow = rowAccount.replaceAll("ACTION", this.pConfig.getProperty("ACTION").trim()).replaceAll("ACCOUNT_ID", accountId).replaceAll("LEDGER_BALANCE", this.pConfig.getProperty("LEDGER_BALANCE").trim()).replaceAll("AVAILABLE_BALANCE", this.pConfig.getProperty("AVAILABLE_BALANCE").trim()).replaceAll("ACCOUNT_TYPE", this.pConfig.getProperty("ACCOUNT_TYPE").trim());
        return dataRow;
    }

    public String getCodigoCliente(dbinterface dboracle, String cirifCliente) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_EMPRESA");
        String codCliente = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-NUM-EMPRESA", cirifCliente);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_EMPRESA");
            dboracle.dbClose();
            return "";
        }
        if (dboracle.nextRecord()) {
            codCliente = dboracle.getFieldString("COD_CLIENTE");
        }
        dboracle.dbClose();
        return codCliente;
    }

    public String getCodigoSucursal(dbinterface dboracle, String rif) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_SUCURSAL");
        String codSucursal = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-NUM-EMPRESA", rif);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_SUCURSAL");
            dboracle.dbClose();
            return "";
        }
        if (dboracle.nextRecord()) {
            codSucursal = dboracle.getFieldString("COD");
        }
        dboracle.dbClose();
        return codSucursal;
    }

    public String getNameCompany(ArrayList<Empresa> listaEmpresa, String codigo) {
        for (Empresa listaEmpresas : listaEmpresa) {
            if (!listaEmpresas.getNvpIdServicioG().trim().equalsIgnoreCase(codigo)) continue;
            return listaEmpresas.getNvpNombreEmpresa();
        }
        return null;
    }

    public String[] getPersonaNominada(dbinterface dboracle, String idServicio, String stock, String numReg) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_DATOS_PERSONA");
        String[] persona = new String[5];
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-STATUS", "1").replaceAll("NVP-IDSERVICIO-D", idServicio).replaceAll("NVP-NUM-STOCK-D", stock).replaceAll("NVP-SEC-TAR", numReg);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_DATOS_PERSONA");
            dboracle.dbClose();
            return null;
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente CONSULTA_DATOS_PERSONA " + idServicio + " " + stock + " " + numReg));
            persona[0] = dboracle.getFieldValue("ID_EXT_PER");
            persona[1] = dboracle.getFieldValue("NOMBRES");
            persona[2] = dboracle.getFieldValue("APELLIDO_PATERNO");
            persona[3] = dboracle.getFieldValue("APELLIDO_MATERNO");
            persona[4] = dboracle.getFieldValue("NATIONAL_ID");
        }
        dboracle.dbClose();
        return persona;
    }
}

