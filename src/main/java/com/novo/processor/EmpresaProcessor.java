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
import java.util.ArrayList;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EmpresaProcessor {
    private static Log log = LogFactory.getLog((String)Process.class.getName());
    private ArrayList<Empresa> listaEmpresa;
    Properties pSql = null;
    Properties pConfig = null;

    public ArrayList<Empresa> getListaEmpresa() {
        return this.listaEmpresa;
    }

    public void setListaEmpresa(ArrayList<Empresa> listaEmpresa) {
        this.listaEmpresa = listaEmpresa;
    }

    public EmpresaProcessor(Properties pSql, Properties pConfig, ArrayList<Empresa> listaEmpresa) {
        this.pSql = pSql;
        this.pConfig = pConfig;
        this.listaEmpresa = listaEmpresa;
    }

    public boolean manageDataEmpresa(dbinterface dboracle) throws SQLException {
        String sql = "";
        for (Empresa listaEmp : this.listaEmpresa) {
            String identificador;
            if ("A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) && "A".equals(listaEmp.getNvpMotivoSrv().trim()) && !listaEmp.isReactivacionCuenta() && !listaEmp.isRegistrarSoloLote()) {
                dboracle.dbreset();
                log.info((Object)"*****Inicia Sentencias SQL para Empresa*****");
                identificador = listaEmp.getNvpIdServicioG().trim().substring(listaEmp.getNvpIdServicioG().length() - 1);
                sql = this.pSql.getProperty("SQL_REGISTRO_EMPRESA");
                if (listaEmp.getNvpIndsrvna().equalsIgnoreCase("N")) {
                    listaEmp.setNvpIndsrvna("BRAND-2023");
                } else {
                    listaEmp.setNvpIndsrvna("BRAND-2020");
                }
                sql = sql.replaceAll("NVP-NOMBRE-EMPRESA", listaEmp.getNvpNombreEmpresa().trim()).replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("NVP-TIPO-EMPRESA", listaEmp.getNvpTipoEmpresa().trim()).replaceAll("NVP-NOMBRE-USUARIO", listaEmp.getUsuario().getNvpNombreUsuario().trim()).replaceAll("NVP-CORREO-USUARIO", listaEmp.getUsuario().getNvpCorreoUsuario().trim()).replaceAll("NVP-NUM-EMPRESA", listaEmp.getNvpNumeroEmpresa().trim().replaceFirst("^0+(?!$)", "")).replaceAll("NVP-IDSERVICIO-G", identificador).replaceAll("NVP-ZONA", listaEmp.getNvpIdServicioG()).replaceAll("NVP-INDSRVNA", listaEmp.getNvpIndsrvna());
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_EMPRESA");
                    return false;
                }
                System.out.println("Se registro correctamente SQL_REGISTRO_EMPRESA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim());
                log.info((Object)("Se registro correctamente SQL_REGISTRO_EMPRESA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                sql = this.pSql.getProperty("SQL_REGISTRO_DIRECCION_EMPRESA");
                sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim());
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_EMPRESA_DIRECCION");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_DIRECCION_EMPRESA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                sql = this.pSql.getProperty("SQL_REGISTRO_SUCURSAL_EMPRESA");
                sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim());
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_EMPRESA_SUCURSAL");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_SUCURSAL_EMPRESA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_PRODUCTO_EMPRESA");
                sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim());
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_EMPRESA_ASIG_PRODUCTO");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_PRODUCTO_EMPRESA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_COMISION_EMPRESA");
                sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim());
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_EMPRESA_ASIG_COMISION");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_COMISION_EMPRESA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                dboracle.dbClose();
                if ("S".equals(this.pConfig.getProperty("SQL_REGISTRO_EXON_COMISION_EMPRESA").trim())) {
                    sql = this.pSql.getProperty("SQL_REGISTRO_EXON_COMISION_EMPRESA");
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim())) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_EMPRESA_EXON_COMISION");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_EXON_COMISION_EMPRESA para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                    dboracle.dbClose();
                }
                String[] listaTipoAsigLote = this.pSql.getProperty("LISTA_TIPO_ASIG_LOTE").split(",");
                for (int i = 0; i < listaTipoAsigLote.length; ++i) {
                    sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_IMPUESTO");
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("LISTA-TIPO-LOTE", listaTipoAsigLote[i])) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_EMPRESA_ASIG_IMPUESTO");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_IMPUESTO tipo: " + listaTipoAsigLote[i] + " para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                    dboracle.dbClose();
                }
                if ("0000".equals(listaEmp.getNvpNumStockG().trim())) {
                    sql = this.pSql.getProperty("SQL_REGISTRO_SOLICITUD_EMI_BNT");
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-EMPRESA", listaEmp.getNvpNumeroEmpresa().trim()).replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("NVP-TIPO-EMPRESA", listaEmp.getNvpTipoEmpresa().trim()).replaceAll("SEC-STOCK", "0").replaceAll("NVP-ID-LOTE", "null")) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_SOLICITUD_EMI_BNT");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_SOLICITUD_EMI_BNT para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                    dboracle.dbClose();
                }
                String[] listaIdParametro = this.pSql.getProperty("LISTA_ID_PARAMETRO").split("-");
                String[] listaValorParametro = this.pSql.getProperty("LISTA_PARAMETRO_RECARGA").split("-");
                for (int i = 0; i < listaIdParametro.length; ++i) {
                    sql = this.pSql.getProperty("SQL_REGISTRAR_PARAMETRO_RECARGA");
                    if (dboracle.executeQuery(sql = sql.replaceAll("NUMPARAMETRO", listaIdParametro[i]).replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("VALOR-PARAMETRO", listaValorParametro[i])) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_PARAMETRO_RECARGA");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRAR_PARAMETRO_RECARGA id: " + listaIdParametro[i] + " valor: " + listaValorParametro[i] + " para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
                    dboracle.dbClose();
                }
                log.info((Object)"*****Finaliza Sentencias SQL para Empresa*****");
                continue;
            }
            if (!"A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) || !"A".equals(listaEmp.getNvpMotivoSrv().trim()) || !listaEmp.isReactivacionCuenta()) continue;
            dboracle.dbreset();
            log.info((Object)("*****Inicia La Reactivacion del Cuenta/Empresa: " + listaEmp.getNvpIdServicioG() + " *****"));
            identificador = listaEmp.getNvpIdServicioG().trim().substring(listaEmp.getNvpIdServicioG().length() - 1);
            sql = this.pSql.getProperty("SQL_UPDATE_EMPRESA");
            sql = sql.replaceAll("NVP-NOMBRE-USUARIO", listaEmp.getUsuario().getNvpNombreUsuario().trim()).replaceAll("NVP-CORREO-USUARIO", listaEmp.getUsuario().getNvpCorreoUsuario().trim()).replaceAll("NVP-NUM-EMPRESA", listaEmp.getNvpNumeroEmpresa().trim()).replaceAll("NVP-TIPO-EMPRESA", listaEmp.getNvpTipoEmpresa().trim()).replaceAll("NVP-IDSERVICIO-G", identificador).replaceAll("NVP-ZONA", listaEmp.getNvpIdServicioG()).replaceAll("NVP-INDSRVNA", listaEmp.getNvpIndsrvna()).replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim());
            if (dboracle.executeQuery(sql) != 0) {
                log.debug((Object)"MSG_ERROR_SQL_ACTUALIZA_EMPRESA");
                return false;
            }
            log.info((Object)("Se actualizo el estatus correctamente para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
            dboracle.dbClose();
            if (!"0000".equals(listaEmp.getNvpNumStockG().trim())) continue;
            sql = this.pSql.getProperty("SQL_REGISTRO_SOLICITUD_EMI_BNT");
            if (dboracle.executeQuery(sql = sql.replaceAll("NVP-NUM-EMPRESA", listaEmp.getNvpNumeroEmpresa().trim()).replaceAll("NVP-CTA-ADMIN", listaEmp.getNvpCtaAdmin().trim()).replaceAll("NVP-TIPO-EMPRESA", listaEmp.getNvpTipoEmpresa().trim()).replaceAll("SEC-STOCK", "0").replaceAll("NVP-ID-LOTE", "null")) != 0) {
                log.debug((Object)"MSG_ERROR_INSERT_SOLICITUD_EMI_BNT");
                return false;
            }
            log.info((Object)("Se registro correctamente SQL_REGISTRO_SOLICITUD_EMI_BNT para la cuenta: " + listaEmp.getNvpCtaAdmin().trim()));
            dboracle.dbClose();
        }
        return true;
    }
}

