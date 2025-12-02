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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TarjetaProcessor {
    private static Log log = LogFactory.getLog((String)Process.class.getName());
    Properties pSql = null;
    Properties pConfig = null;

    public TarjetaProcessor(Properties pSql, Properties pConfig) {
        this.pSql = pSql;
        this.pConfig = pConfig;
    }

    public boolean registerLotes(dbinterface dboracle, ArrayList<Empresa> listaEmpresa, ArrayList<Tarjeta> listaTarjeta) throws SQLException {
        ArrayList<Empresa> listaEmpresaAuxTar = new ArrayList<Empresa>();
        for (int j = 0; j < listaEmpresa.size(); ++j) {
            log.info((Object)("listaEmpresa.getNvpCuenta() " + listaEmpresa.get(j).getNvpCuenta()));
            log.info((Object)("listaEmpresa.getNvpCtaAdmin() " + listaEmpresa.get(j).getNvpCtaAdmin()));
            log.info((Object)("listaEmpresa.getNvpNumStockG() " + listaEmpresa.get(j).getNvpNumStockG()));
            log.info((Object)("listaEmpresa.getRespuesta().getNvpResultado() " + listaEmpresa.get(j).getRespuesta().getNvpResultado()));
            if ("00000".equals(listaEmpresa.get(j).getNvpNumReg()) || "0000".equals(listaEmpresa.get(j).getNvpNumStockG()) || !"A".equals(listaEmpresa.get(j).getRespuesta().getNvpResultado().trim())) continue;
            listaEmpresaAuxTar.add(listaEmpresa.get(j));
        }
        Date fecActual = new Date();
        String format = "yyyy-MM-dd";
        SimpleDateFormat objDateF = new SimpleDateFormat(format);
        String[] formateoFecha = objDateF.format(fecActual).split("-");
        String nroLote = formateoFecha[0].substring(2, 4) + formateoFecha[1] + formateoFecha[2] + "00";
        for (Empresa listaEmp : listaEmpresaAuxTar) {
            String namePlastico;
            String[] infoLote;
            if ("A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) && !listaEmp.isRegistrarSoloLote()) {
                String codLote = "";
                String nOrden = "";
                String sql = "";
                String codCliente = "";
                String codSucursal = "";
                if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("N")) {
                    codCliente = this.getCodigoCliente(dboracle, listaEmp.getNvpCtaAdmin().trim());
                    codSucursal = this.getCodigoSucursal(dboracle, listaEmp.getNvpCtaAdmin().trim());
                } else if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("A")) {
                    codCliente = this.getCodigoCliente(dboracle, listaEmp.getNvpCuenta().trim());
                    codSucursal = this.getCodigoSucursal(dboracle, listaEmp.getNvpCuenta().trim());
                }
                log.info((Object)"*****Inicia Sentencias SQL para Lote y Maestros*****");
                if ("A".equals(listaEmp.getNvpMotivoSrv().trim())) {
                    dboracle.dbreset();
                    sql = this.pSql.getProperty("SQL_REGISTRO_LOTE");
                    sql = sql.replaceAll("COD_CLIENTE", codCliente).replaceAll("NRO-LOTE", nroLote).replaceAll("TIPO-LOTE", this.pConfig.getProperty("TYPE_LOTE").trim()).replaceAll("NVP-NUM-REGST", listaEmp.getNvpNumReg().trim().replaceFirst("^0+(?!$)", "")).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim()).replaceAll("ESTATUS-LOTE", this.pConfig.getProperty("STATUS_LOTE").trim()).replaceAll("PRODUCTO_VALOR", this.pConfig.getProperty("PRODUCT").trim());
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_LOTE");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_LOTE " + codCliente + " " + nroLote));
                    dboracle.dbClose();
                    codLote = this.getCodigoLote(dboracle, nroLote, codCliente);
                    sql = this.pSql.getProperty("SQL_REGISTRO_NRO_ORDEN");
                    sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim()).replaceAll("NVP-PRODUCTO", this.pConfig.getProperty("PRODUCT").trim());
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_NRO_ORDEN");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_NRO_ORDEN para la cuenta " + listaEmp.getNvpCtaAdmin().trim()));
                    dboracle.dbClose();
                    nOrden = this.getNroOrden(dboracle, listaEmp.getNvpCtaAdmin().trim());
                    sql = this.pSql.getProperty("SQL_REGISTRO_VINCULACION_NRO_ORDEN");
                    sql = sql.replaceAll("NRO-ORDEN", nOrden).replaceAll("NVP-ID-LOTE", codLote).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim());
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_VINCULACION_USUARIO_NRO_ORDEN");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_VINCULACION_NRO_ORDEN orden " + nOrden + " y lote " + codLote));
                    dboracle.dbClose();
                    if ("0001".equals(listaEmp.getNvpNumStockG().trim())) {
                        sql = this.pSql.getProperty("SQL_REGISTRO_SOLICITUD_EMI_BNT");
                        if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-EMPRESA", listaEmp.getNvpNumeroEmpresa().trim()).replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("NVP-TIPO-EMPRESA", listaEmp.getNvpTipoEmpresa().trim()).replaceAll("SEC-STOCK", "1").replaceAll("NVP-ID-LOTE", codLote)) != 0) {
                            log.debug((Object)"MSG_ERROR_INSERT_SOLICITUD_EMI_BNT");
                            return false;
                        }
                        log.info((Object)("Se registro correctamente SQL_REGISTRO_SOLICITUD_EMI_BNT para la cuenta " + listaEmp.getNvpCtaAdmin().trim()));
                        dboracle.dbClose();
                    }
                }
                boolean flagInsert = true;
                for (Tarjeta listaTarjetas : listaTarjeta) {
                    String nombreCliente;
                    if (!listaTarjetas.getNvpIdServicioD().trim().equals(listaEmp.getNvpIdServicioG().trim()) || !listaTarjetas.getNvpNumStockD().trim().equals(listaEmp.getNvpNumStockG().trim()) || !"A".equals(listaTarjetas.getRespuesta().getNvpResultado().trim())) continue;
                    dboracle.dbreset();
                    if (flagInsert) {
                        if ("A".equals(listaEmp.getNvpMotivoSrv().trim())) {
                            sql = this.pSql.getProperty("SQL_REGISTRO_EMISION");
                            if (dboracle.executeQuery(sql = sql.replaceAll("ACIDLOTE", codLote).replaceAll("COD_CLIENTE", codCliente).replaceAll("NVP-NUM-REGST", listaEmp.getNvpNumReg().trim().replaceFirst("^0+(?!$)", "")).replaceAll("NVP-FEC-EXPIRACION", listaTarjetas.getNvpFecExpiracion().trim()).replaceAll("NVP-EMB1", listaTarjetas.getNvpEmb1().trim()).replaceAll("PRODUCTO_VALOR", this.pConfig.getProperty("PRODUCT").trim()).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim()).replaceAll("COD_SUCURSAL", codSucursal).replaceAll("NVP-EMB2", listaTarjetas.getNvpEmb2().trim())) != 0) {
                                log.debug((Object)"MSG_ERROR_INSERT_EMISION_DETALLE");
                                return false;
                            }
                            log.info((Object)("Se registro correctamente SQL_REGISTRO_EMISION " + codCliente + " " + nroLote));
                            dboracle.dbClose();
                        }
                        if ("M".equals(listaEmp.getNvpMotivoSrv().trim())) {
                            if ("I".equals(listaTarjetas.getNvpIndEmb().trim())) {
                                infoLote = this.getLoteInnoSubsecuente(dboracle, listaTarjetas.getNvpIdServicioD().trim(), listaTarjetas.getNvpNumStockD().trim());
                                nroLote = infoLote[0];
                                codLote = infoLote[1];
                            }
                            if ("N".equals(listaTarjetas.getNvpIndEmb().trim())) {
                                infoLote = this.getNovoLoteEmiSubsecuente(dboracle, listaTarjetas.getNvpIdServicioD().trim(), listaTarjetas.getNvpNumStockD().trim(), listaTarjetas.getNvpNumReg());
                                nroLote = infoLote[0];
                                codLote = infoLote[1];
                            }
                        }
                        flagInsert = false;
                    }
                    String cuenta = "";
                    if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("N")) {
                        cuenta = listaEmp.getNvpCtaAdmin().trim();
                    } else if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("A")) {
                        cuenta = listaEmp.getNvpCuenta().trim();
                    }
                    if ("I".equals(listaTarjetas.getNvpIndEmb().trim())) {
                        nombreCliente = this.pConfig.getProperty("C1_NAME_ON_CARD");
                        if ("A".equals(listaEmp.getNvpMotivoSrv().trim())) {
                            sql = this.pSql.getProperty("SQL_REGISTRO_EMISION_INNO");
                            if (dboracle.executeQuery(sql = sql.replaceAll("ACIDLOTE", codLote).replaceAll("NRO-LOTE", nroLote).replaceAll("NVP-CTA-ADMIN", cuenta).replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("COD_SUCURSAL", codSucursal)) != 0) {
                                log.debug((Object)"MSG_ERROR_INSERT_EMISION_DETALLE_INNO");
                                return false;
                            }
                            log.info((Object)("Se registro correctamente SQL_REGISTRO_EMISION_INNO para la cuenta empresa " + listaEmp.getNvpCtaAdmin().trim() + " y lote " + nroLote));
                            dboracle.dbClose();
                        }
                        if ("M".equals(listaEmp.getNvpMotivoSrv().trim())) {
                            sql = this.pSql.getProperty("SQL_REGISTRO_EMISION_INNO");
                            if (dboracle.executeQuery(sql = sql.replaceAll("ACIDLOTE", codLote).replaceAll("NRO-LOTE", nroLote).replaceAll("NVP-CTA-ADMIN", cuenta).replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("COD_SUCURSAL", codSucursal)) != 0) {
                                log.debug((Object)"MSG_ERROR_INSERT_EMISION_DETALLE_INNO");
                                return false;
                            }
                            log.info((Object)("Se registro correctamente SQL_REGISTRO_EMISION_INNO para la cuenta empresa " + listaEmp.getNvpCtaAdmin().trim() + " y lote " + nroLote));
                            dboracle.dbClose();
                            sql = this.pSql.getProperty("SQL_UPDATE_TEB_LOTE_INNO");
                            sql = sql.replaceAll("ID-LOTE", codLote);
                            if (dboracle.executeQuery(sql) != 0) {
                                log.debug((Object)"MSG_ERROR_UPDATE_TEB_LOTE_INNO");
                                return false;
                            }
                            log.info((Object)("Se update correctamente SQL_UPDATE_TEB_LOTE_INNO para el lote " + codLote));
                            dboracle.dbClose();
                        }
                        sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_CONSOLIDADO_INNO");
                        if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-FEC-EXPIRACION", listaTarjetas.getNvpFecExpiracion().trim()).replaceAll("NVP-CTA-ADMIN", cuenta).replaceAll("COD_CLIENTE", codCliente)) != 0) {
                            log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_CONSOLIDADO_INNO");
                            return false;
                        }
                        log.info((Object)("Se registro correctamente SQL_MAESTRO_CONSOLIDADO_INNO para la cuenta empresa " + listaEmp.getNvpCtaAdmin().trim() + " y cuenta personal " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                        dboracle.dbClose();
                        sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_PLASTICO_INNO");
                        namePlastico = nombreCliente + ";" + listaEmp.getNvpNombreEmpresa().trim();
                        if (namePlastico.length() >= 41) {
                            namePlastico = namePlastico.substring(0, 41);
                        }
                        if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-NOM-PLASTIC", namePlastico).replaceAll("NVP-CTA-ADMIN", cuenta).replaceAll("COD_CLIENTE", codCliente)) != 0) {
                            log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_PLASTICO_INNO");
                            return false;
                        }
                        log.info((Object)("Se registro correctamente SQL_MAESTRO_PLASTICO_INNO para la cuenta empresas " + listaEmp.getNvpCtaAdmin().trim() + " y tarjeta " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                        dboracle.dbClose();
                        continue;
                    }
                    if (!"N".equals(listaTarjetas.getNvpIndEmb().trim())) continue;
                    nombreCliente = listaTarjetas.getNvpEmb1().trim();
                    String[] persona = this.getPersonaNominada(dboracle, listaTarjetas.getNvpIdServicioD().trim(), listaTarjetas.getNvpNumStockD().trim(), listaTarjetas.getNvpNumReg().trim());
                    if (persona[0] == null) {
                        log.debug((Object)"MSG_ERROR_PERSONA");
                        continue;
                    }
                    sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_CONSOLIDADO_NOMI");
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-FEC-EXPIRACION", listaTarjetas.getNvpFecExpiracion().trim()).replaceAll("NVP-CTA-ADMIN", cuenta).replaceAll("ID-EXT-PER", persona[0]).replaceAll("COD_CLIENTE", codCliente)) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_CONSOLIDADO_NOMI");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_MAESTRO_CONSOLIDADO_NOMI para la cuenta empresa " + listaEmp.getNvpCtaAdmin().trim() + " y cuenta personal " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                    dboracle.dbClose();
                    sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_PLASTICO_NOMI");
                    String namePlastico2 = nombreCliente + ";" + listaEmp.getNvpNombreEmpresa().trim();
                    if (namePlastico2.length() >= 41) {
                        namePlastico2 = namePlastico2.substring(0, 41);
                    }
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-NOM-PLASTIC", namePlastico2).replaceAll("NVP-CTA-ADMIN", cuenta).replaceAll("ID-EXT-PER", persona[0]).replaceAll("COD_CLIENTE", codCliente)) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_PLASTICO_NOMI");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_MAESTRO_PLASTICO_NOMI para la cuenta empresa " + listaEmp.getNvpCtaAdmin().trim() + " y tarjeta " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                    dboracle.dbClose();
                    sql = this.pSql.getProperty("SQL_UPDATE_NOVO_LOTE_EMI");
                    sql = sql.replaceAll("NVP-IDSERVICIO-D", listaTarjetas.getNvpIdServicioD().trim()).replaceAll("NVP-NUM-STOCK-D", listaTarjetas.getNvpNumStockD().trim()).replaceAll("NVP-SEC-TAR", listaTarjetas.getNvpNumReg().trim()).replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim());
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_SQL_UPDATE_NOVO_LOTE_EMI");
                        return false;
                    }
                    log.info((Object)("Se actualizo Novo Lote Emi para la tarjeta " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                    dboracle.dbClose();
                    sql = this.pSql.getProperty("SQL_UPDATE_TEB_LOTE_INNO");
                    sql = sql.replaceAll("ID-LOTE", codLote);
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_UPDATE_TEB_LOTE_INNO");
                        return false;
                    }
                    log.info((Object)("Se update correctamente SQL_UPDATE_TEB_LOTE_INNO para el lote " + codLote));
                    dboracle.dbClose();
                }
                continue;
            }
            if (!"A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) || !listaEmp.isRegistrarSoloLote()) continue;
            String codCliente = "";
            String codSucursal = "";
            String codLote = "";
            if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("N")) {
                codCliente = this.getCodigoCliente(dboracle, listaEmp.getNvpCtaAdmin().trim());
                codSucursal = this.getCodigoSucursal(dboracle, listaEmp.getNvpCtaAdmin().trim());
            } else if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("A")) {
                codCliente = this.getCodigoCliente(dboracle, listaEmp.getNvpCuenta().trim());
                codSucursal = this.getCodigoSucursal(dboracle, listaEmp.getNvpCuenta().trim());
            }
            boolean flag = true;
            for (Tarjeta listaTarjetas : listaTarjeta) {
                if (listaTarjetas.getNvpIdServicioD().trim().equals(listaEmp.getNvpIdServicioG().trim()) && listaTarjetas.getNvpNumStockD().trim().equals(listaEmp.getNvpNumStockG().trim())) {
                    dboracle.dbreset();
                    String sql = "";
                    String nombreCliente = "";
                    if ("N".equals(listaTarjetas.getNvpIndEmb().trim())) {
                        if (flag) {
                            infoLote = this.getNovoLoteEmiSubsecuente(dboracle, listaTarjetas.getNvpIdServicioD().trim(), listaTarjetas.getNvpNumStockD().trim(), listaTarjetas.getNvpNumReg());
                            nroLote = infoLote[0];
                            codLote = infoLote[1];
                            flag = false;
                        }
                        nombreCliente = listaTarjetas.getNvpEmb1().trim();
                        String[] persona = this.getPersonaNominada(dboracle, listaTarjetas.getNvpIdServicioD().trim(), listaTarjetas.getNvpNumStockD().trim(), listaTarjetas.getNvpNumReg().trim());
                        String cuenta2 = "";
                        if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("N")) {
                            cuenta2 = listaEmp.getNvpCtaAdmin().trim();
                        } else if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("A")) {
                            cuenta2 = listaEmp.getNvpCuenta().trim();
                        }
                        sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_CONSOLIDADO_NOMI");
                        sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-FEC-EXPIRACION", listaTarjetas.getNvpFecExpiracion().trim()).replaceAll("NVP-CTA-ADMIN", cuenta2).replaceAll("ID-EXT-PER", persona[0]).replaceAll("COD_CLIENTE", codCliente);
                        if (dboracle.executeQuery(sql) != 0) {
                            log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_CONSOLIDADO_NOMI");
                            return false;
                        }
                        log.info((Object)("Se registro correctamente SQL_MAESTRO_CONSOLIDADO_NOMI para la cuenta empresa " + cuenta2 + " y cuenta personal " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                        dboracle.dbClose();
                        sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_PLASTICO_NOMI");
                        namePlastico = nombreCliente + ";" + listaEmp.getNvpNombreEmpresa().trim();
                        if (namePlastico.length() >= 41) {
                            namePlastico = namePlastico.substring(0, 41);
                        }
                        if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-NOM-PLASTIC", namePlastico).replaceAll("NVP-CTA-ADMIN", cuenta2).replaceAll("ID-EXT-PER", persona[0]).replaceAll("COD_CLIENTE", codCliente)) != 0) {
                            log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_PLASTICO_NOMI");
                            return false;
                        }
                        log.info((Object)("Se registro correctamente SQL_MAESTRO_PLASTICO_NOMI para la cuenta empresa " + cuenta2 + " y tarjeta " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                        dboracle.dbClose();
                        sql = this.pSql.getProperty("SQL_UPDATE_NOVO_LOTE_EMI");
                        sql = sql.replaceAll("NVP-IDSERVICIO-D", listaTarjetas.getNvpIdServicioD().trim()).replaceAll("NVP-NUM-STOCK-D", listaTarjetas.getNvpNumStockD().trim()).replaceAll("NVP-SEC-TAR", listaTarjetas.getNvpNumReg().trim()).replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim());
                        if (dboracle.executeQuery(sql) != 0) {
                            log.debug((Object)"MSG_ERROR_SQL_UPDATE_NOVO_LOTE_EMI");
                            return false;
                        }
                        log.info((Object)("Se actualizo Novo Lote Emi para la tarjeta " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                        dboracle.dbClose();
                        sql = this.pSql.getProperty("SQL_UPDATE_TEB_LOTE_INNO");
                        sql = sql.replaceAll("ID-LOTE", codLote);
                        if (dboracle.executeQuery(sql) != 0) {
                            log.debug((Object)"MSG_ERROR_UPDATE_TEB_LOTE_INNO");
                            return false;
                        }
                        log.info((Object)("Se update correctamente SQL_UPDATE_TEB_LOTE_INNO para el lote " + codLote));
                        dboracle.dbClose();
                        continue;
                    }
                    if (!"I".equals(listaTarjetas.getNvpIndEmb().trim())) continue;
                    nombreCliente = this.pConfig.getProperty("C1_NAME_ON_CARD");
                    if (flag) {
                        infoLote = this.getLoteInnoSubsecuente(dboracle, listaTarjetas.getNvpIdServicioD().trim(), listaTarjetas.getNvpNumStockD().trim());
                        nroLote = infoLote[0];
                        codLote = infoLote[1];
                        flag = false;
                    }
                    String cuenta3 = "";
                    if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("N")) {
                        cuenta3 = listaEmp.getNvpCtaAdmin().trim();
                    } else if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("A")) {
                        cuenta3 = listaEmp.getNvpCuenta().trim();
                    }
                    sql = this.pSql.getProperty("SQL_REGISTRO_EMISION_INNO");
                    sql = sql.replaceAll("ACIDLOTE", codLote).replaceAll("NRO-LOTE", nroLote).replaceAll("NVP-CTA-ADMIN", cuenta3).replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("COD_SUCURSAL", codSucursal);
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_EMISION_DETALLE_INNO");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_EMISION_INNO para la cuenta " + cuenta3 + " y lote " + nroLote));
                    dboracle.dbClose();
                    sql = this.pSql.getProperty("SQL_UPDATE_TEB_LOTE_INNO");
                    sql = sql.replaceAll("ID-LOTE", codLote);
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_UPDATE_TEB_LOTE_INNO");
                        return false;
                    }
                    log.info((Object)("Se update correctamente SQL_UPDATE_TEB_LOTE_INNO para el lote " + codLote));
                    dboracle.dbClose();
                    sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_CONSOLIDADO_INNO");
                    sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-FEC-EXPIRACION", listaTarjetas.getNvpFecExpiracion().trim()).replaceAll("NVP-CTA-ADMIN", cuenta3).replaceAll("COD_CLIENTE", codCliente);
                    if (dboracle.executeQuery(sql) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_CONSOLIDADO_INNO");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_MAESTRO_CONSOLIDADO_INNO para la cuenta empresa " + cuenta3 + " y cuenta personal " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                    dboracle.dbClose();
                    sql = this.pSql.getProperty("SQL_REGISTRO_MAESTRO_PLASTICO_INNO");
                    String namePlastico3 = nombreCliente + ";" + listaEmp.getNvpNombreEmpresa().trim();
                    if (namePlastico3.length() >= 41) {
                        namePlastico3 = namePlastico3.substring(0, 41);
                    }
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-TAR", listaTarjetas.getNvpNumeroTarjeta().trim()).replaceAll("NVP-NOM-CLIENT", nombreCliente).replaceAll("NVP-NUM-CLI", listaTarjetas.getNvpNumeroTarjeta().trim().substring(8, 16)).replaceAll("NVP-NOM-PLASTIC", namePlastico3).replaceAll("NVP-CTA-ADMIN", cuenta3).replaceAll("COD_CLIENTE", codCliente)) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_MAESTRO_PLASTICO_INNO");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_MAESTRO_PLASTICO_INNO para la cuenta empresa " + cuenta3 + " y tarjeta " + listaTarjetas.getNvpNumeroTarjeta().trim()));
                    dboracle.dbClose();
                    continue;
                }
                listaEmp.getRespuesta().setNvpRespuesta("R");
                listaEmp.getRespuesta().setNvpResultado("ERROR EN TARJETAS");
                listaEmp.getRespuesta().setNvpDetalleResp("TARJETAS CON INCONSISTENCIA");
            }
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

    public String getCodigoSucursal(dbinterface dboracle, String rif) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_SUCURSAL");
        String codSucursal = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-CTA-ADMIN", rif);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_SUCURSAL");
            return "";
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_SUCURSAL " + rif));
            codSucursal = dboracle.getFieldString("COD");
        }
        return codSucursal;
    }

    public String getCodigoLote(dbinterface dboracle, String nroLote, String codCliente) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_LOTE");
        String codLote = "";
        dboracle.dbreset();
        sql = sql.replaceAll("TIPO-LOTE", this.pConfig.getProperty("TYPE_LOTE").trim()).replaceAll("NRO-LOTE", nroLote).replaceAll("COD_CLIENTE", codCliente);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_LOTE");
            return "";
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_LOTE para el lote " + nroLote + " y cliente " + codCliente));
            codLote = dboracle.getFieldString("ACIDLOTE");
        }
        return codLote;
    }

    public String[] getPersonaNominada(dbinterface dboracle, String idServicio, String stock, String numReg) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_DATOS_PERSONA");
        String[] persona = new String[5];
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-STATUS", "0").replaceAll("NVP-IDSERVICIO-D", idServicio).replaceAll("NVP-NUM-STOCK-D", stock).replaceAll("NVP-SEC-TAR", numReg);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_DATOS_PERSONA");
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
        return persona;
    }

    public String[] getLoteInnoSubsecuente(dbinterface dboracle, String idServicio, String stock) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_LOTE_INNO_M");
        String[] infoLote = new String[2];
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-IDSERVICIO-D", idServicio).replaceAll("NVP-NUM-STOCK-D", stock);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_SQL_CONSULTA_LOTE_INNO_M");
            return null;
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente CONSULTA_LOTE_INNO_M para el id de servicio " + idServicio + " y secuencia " + stock));
            infoLote[0] = dboracle.getFieldValue("NRO_LOTE");
            infoLote[1] = dboracle.getFieldValue("ID_LOTE");
        }
        return infoLote;
    }

    public String[] getNovoLoteEmiSubsecuente(dbinterface dboracle, String idServicio, String stock, String secuencia) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_NOVO_LOTE_EMI_M");
        String[] infoLote = new String[2];
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-IDSERVICIO-D", idServicio).replaceAll("NVP-SEC-TAR", secuencia).replaceAll("NVP-NUM-STOCK-D", stock);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_SQL_CONSULTA_NOVO_LOTE_EMI_M");
            return null;
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente CONSULTA_NOVO_LOTE_EMI_M para el id de servicio " + idServicio + " y secuencia " + stock));
            infoLote[0] = dboracle.getFieldValue("NRO_LOTE");
            infoLote[1] = dboracle.getFieldValue("ID_LOTE");
        }
        return infoLote;
    }

    public String getNroOrden(dbinterface dboracle, String cuenta) throws SQLException {
        String sql = this.pSql.getProperty("SQL_CONSULTA_NRO_ORDEN");
        String nOrden = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-CTA-ADMIN", cuenta);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_NRO_ORDEN");
            return "";
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_NRO_ORDEN para la cuenta " + cuenta));
            nOrden = dboracle.getFieldString("IDORDENS");
        }
        return nOrden;
    }
}

