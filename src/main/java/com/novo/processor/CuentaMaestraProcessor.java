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
import com.novo.database.dbinterface;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CuentaMaestraProcessor {
    private static Log log = LogFactory.getLog((String)Process.class.getName());
    Properties pSql = null;
    Properties pConfig = null;

    public CuentaMaestraProcessor(Properties pSql, Properties pConfig) {
        this.pSql = pSql;
        this.pConfig = pConfig;
    }

    public boolean registerCuentaMaestra(dbinterface dboracle, List<Empresa> listaEmpresa) throws SQLException {
        String sql = "";
        for (Empresa listaEmp : listaEmpresa) {
            String referencia;
            SimpleDateFormat objDateF;
            String format;
            Date fecActual;
            String saldo;
            if ("A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) && "A".equals(listaEmp.getNvpMotivoSrv().trim()) && !listaEmp.isReactivacionCuenta() && !listaEmp.isRegistrarSoloLote()) {
                String[] listaTxn;
                dboracle.dbreset();
                log.info((Object)"*****Inicia Sentencias SQL para Cuenta Maestra*****");
                saldo = this.getSaldo(listaEmp.getNvpSaldo().trim());
                sql = this.pSql.getProperty("SQL_REGISTRO_CUENTA_MAESTRA");
                sql = sql.replaceAll("NVP-CUENTA", listaEmp.getNvpCuenta().trim()).replaceAll("NVP-SALDO", saldo).replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin());
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_CUENTA_MAESTRA");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_CUENTA_MAESTRA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                fecActual = new Date();
                format = "yyyyMMdd";
                objDateF = new SimpleDateFormat(format);
                referencia = listaEmp.getNvpCuenta().trim() + objDateF.format(fecActual);
                sql = this.pSql.getProperty("SQL_REGISTRAR_DETALLE_DEPOSITO");
                sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("MONTODEP", saldo).replaceAll("REFERENCIASET", referencia).replaceAll("SALDODISP", saldo).replaceAll("ACPREFSET", this.pConfig.getProperty("PRODUCT")).replaceAll("SALDOINI", saldo).replaceAll("IDREFRUBRO", referencia.substring(0, 16));
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_DETALLE DEPOSITO");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRAR_DETALLE_DEPOSITO para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                sql = this.pSql.getProperty("SQL_REGISTRAR_DETALLE_DEPOSITO_EN");
                sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCuenta().trim()).replaceAll("MONTODEP", saldo).replaceAll("REFERENCIASET", referencia).replaceAll("SALDODISP", saldo).replaceAll("ACPREFSET", this.pConfig.getProperty("PRODUCT")).replaceAll("SALDOINI", saldo);
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_DETALLE DEPOSITO_EN");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRAR_DETALLE_DEPOSITO_EN para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                for (String listaTxn1 : listaTxn = this.pSql.getProperty("LISTA_TIPO_TRANSACCION").split(",")) {
                    sql = this.pSql.getProperty("SQL_REGISTRO_OPERACION_EMPRESA");
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("LISTA-TXN", listaTxn1)) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_OPERACION_EMPRESA");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_OPERACION_EMPRESA tipo: " + listaTxn1 + " para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                    dboracle.dbClose();
                }
                String codCliente = this.getCodigoCliente(dboracle, listaEmp.getNvpCtaAdmin().trim());
                sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_GRUPO_EMPRESA");
                if (dboracle.executeQuery(sql = sql.replaceAll("COD_CLIENTE", codCliente)) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_ASIG_GRUPO_EMPRESA");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_GRUPO_EMPRESA " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                log.info((Object)"*****Finaliza Sentencias SQL para Cuenta Maestra*****");
                continue;
            }
            if (!"A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) || !"A".equals(listaEmp.getNvpMotivoSrv().trim()) || !listaEmp.isReactivacionCuenta()) continue;
            dboracle.dbreset();
            log.info((Object)("*****Inicia La Actualizacion Maestro Deposito de la Cuenta/Empresa: " + listaEmp.getNvpIdServicioG() + " *****"));
            saldo = this.getSaldo(listaEmp.getNvpSaldo().trim());
            sql = this.pSql.getProperty("SQL_UPDATE_CUENTA_MAESTRA");
            sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("NVP-CUENTA", listaEmp.getNvpCuenta().trim()).replaceAll("NVP-SALDO", saldo);
            if (dboracle.executeQuery(sql) != 0) {
                log.debug((Object)"MSG_ERROR_UPDATE_CUENTA_MAESTRA");
                return false;
            }
            log.info((Object)("Se actualizo correctamente la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
            dboracle.dbClose();
            fecActual = new Date();
            format = "yyyyMMdd";
            objDateF = new SimpleDateFormat(format);
            referencia = listaEmp.getNvpCuenta().trim() + objDateF.format(fecActual);
            sql = this.pSql.getProperty("SQL_REGISTRAR_DETALLE_DEPOSITO");
            sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("MONTODEP", saldo).replaceAll("REFERENCIASET", referencia).replaceAll("SALDODISP", saldo).replaceAll("ACPREFSET", this.pConfig.getProperty("PRODUCT")).replaceAll("SALDOINI", saldo).replaceAll("IDREFRUBRO", referencia.substring(0, 16));
            if (dboracle.executeQuery(sql) != 0) {
                log.debug((Object)"MSG_ERROR_INSERT_DETALLE DEPOSITO");
                return false;
            }
            log.info((Object)("Se registro correctamente SQL_REGISTRAR_DETALLE_DEPOSITO para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
            dboracle.dbClose();
            sql = this.pSql.getProperty("SQL_REGISTRAR_DETALLE_DEPOSITO_EN");
            sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("MONTODEP", saldo).replaceAll("REFERENCIASET", referencia).replaceAll("SALDODISP", saldo).replaceAll("ACPREFSET", this.pConfig.getProperty("PRODUCT")).replaceAll("SALDOINI", saldo);
            if (dboracle.executeQuery(sql) != 0) {
                log.debug((Object)"MSG_ERROR_INSERT_DETALLE DEPOSITO_EN");
                return false;
            }
            log.info((Object)("Se registro correctamente SQL_REGISTRAR_DETALLE_DEPOSITO_EN para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
            dboracle.dbClose();
        }
        return true;
    }

    public String getCodigoCliente(dbinterface dboracle, String cirifCliente) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_EMPRESA");
        String codCliente = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-CTA-ADMIN", cirifCliente);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_SQL_CONSULTA_EMPRESA");
            return "";
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_EMPRESA " + cirifCliente));
            codCliente = dboracle.getFieldString("COD_CLIENTE");
        }
        return codCliente;
    }

    public String getSaldo(String saldo) {
        String saldoAux = saldo.substring(0, 13) + "." + saldo.substring(13, 15);
        String f = saldoAux.replaceFirst("^0+(?!$)", "");
        if (f.length() == 3) {
            f = "0" + f;
        }
        return f;
    }
}

