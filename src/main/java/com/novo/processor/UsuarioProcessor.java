/*
 * Decompiled with CFR 0.151.
 * 
 * Could not load the following classes:
 *  com.devflex.MD5Hash
 *  com.novo.database.dbinterface
 *  org.apache.commons.logging.Log
 *  org.apache.commons.logging.LogFactory
 */
package com.novo.processor;

import com.devflex.MD5Hash;
import com.novo.bean.Empresa;
import com.novo.database.dbinterface;
import com.novo.processor.ValidadorProcessor;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UsuarioProcessor {
    private static Log log = LogFactory.getLog((String)Process.class.getName());
    Properties pSql = null;
    Properties pConfig = null;

    public UsuarioProcessor(Properties pSql, Properties pConfig) {
        this.pSql = pSql;
        this.pConfig = pConfig;
    }

    public boolean registerUsuario(dbinterface dboracle, List<Empresa> listaEmpresa) throws SQLException, NoSuchAlgorithmException {
        String sql = "";
        for (Empresa listaEmp : listaEmpresa) {
            if ("A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) && "A".equals(listaEmp.getNvpMotivoSrv().trim()) && !listaEmp.getUsuario().isReactivacionUsuario() && !listaEmp.isRegistrarSoloLote()) {
                dboracle.dbreset();
                log.info((Object)"*****Inicia Sentencias SQL para Usuario*****");
                boolean existUser = ValidadorProcessor.getExistUsuario(dboracle, listaEmp.getUsuario().getNvpUsuarioBemBep().trim(), listaEmp.getUsuario().getNvpCurp().trim().toUpperCase(), this.pSql);
                if (existUser) {
                    listaEmp.getUsuario().setExist(true);
                } else {
                    listaEmp.getUsuario().setExist(false);
                }
                if (!listaEmp.getUsuario().isExist()) {
                    String Nombre = "";
                    String Apellido = " ";
                    Nombre = listaEmp.getUsuario().getNvpNombreUsuario().trim();
                    sql = this.pSql.getProperty("SQL_REGISTRO_USUARIO");
                    if (dboracle.executeQuery(sql = sql.replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim()).replaceAll("NVP-CURP", listaEmp.getUsuario().getNvpCurp().trim()).replaceAll("NVP-NOMBRE-USUARIO-NOMBRE", Nombre).replaceAll("NVP-NOMBRE-USUARIO-APELLIDO", Apellido).replaceAll("TOKEN", MD5Hash.hash((String)listaEmp.getUsuario().getNvpUsuarioBemBep().trim())).replaceAll("NVP-CORREO-USUARIO", listaEmp.getUsuario().getNvpCorreoUsuario().trim())) != 0) {
                        log.debug((Object)"MSG_ERROR_INSERT_USUARIO");
                        return false;
                    }
                    log.info((Object)("Se registro correctamente SQL_REGISTRO_USUARIO " + listaEmp.getUsuario().getNvpNombreUsuario().trim()));
                    dboracle.dbClose();
                    String[] permisos = this.pSql.getProperty("LISTA_FUNCION_ASIG_USUARIO").split(",");
                    for (int i = 0; i < permisos.length; ++i) {
                        sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_USUARIO_PERMISO");
                        if (dboracle.executeQuery(sql = sql.replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim()).replaceAll("LISTA-FUNCION", permisos[i])) != 0) {
                            log.debug((Object)"MSG_ERROR_INSERT_ASIG_USUARIO_PERMISO");
                            return false;
                        }
                        log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_USUARIO_PERMISO " + listaEmp.getUsuario().getNvpUsuarioBemBep().trim() + " " + permisos[i]));
                        dboracle.dbClose();
                    }
                }
                String codCliente = this.getCodigoCliente(dboracle, listaEmp.getNvpCtaAdmin().trim());
                sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_USUARIO_EMPRESA");
                if (dboracle.executeQuery(sql = sql.replaceAll("COD_CLIENTE_MAESTRO", codCliente).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim())) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_ASIGNACION_EMPRESA");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_USUARIO_EMPRESA " + codCliente));
                dboracle.dbClose();
                sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_USUARIO_PRODUCTO");
                sql = sql.replaceAll("COD_CLIENTE_MAESTRO", codCliente).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim());
                if (dboracle.executeQuery(sql) != 0) {
                    log.debug((Object)"MSG_ERROR_INSERT_ASIG_USUARIO_PRODUCTO");
                    return false;
                }
                log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_USUARIO_PRODUCTO para el cliente " + codCliente));
                dboracle.dbClose();
                log.info((Object)"*****Finaliza Sentencias SQL para Usuario*****");
                continue;
            }
            if (!"A".equals(listaEmp.getRespuesta().getNvpResultado().trim()) || !"A".equals(listaEmp.getNvpMotivoSrv().trim()) || !listaEmp.getUsuario().isReactivacionUsuario()) continue;
            dboracle.dbreset();
            log.info((Object)("*****Inicia La Reactivacion del Usuario: " + listaEmp.getUsuario().getNvpUsuarioBemBep() + " *****"));
            sql = this.pSql.getProperty("SQL_UPDATE_USUARIO");
            sql = sql.replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim());
            if (dboracle.executeQuery(sql) != 0) {
                log.debug((Object)"MSG_ERROR_SQL_ACTUALIZA_USUARIO");
                return false;
            }
            log.info((Object)("Se actualizo el estatus correctamente para el usuario: " + listaEmp.getUsuario().getNvpUsuarioBemBep().trim()));
            dboracle.dbClose();
            String codCliente = this.getCodigoCliente(dboracle, listaEmp.getNvpCtaAdmin().trim());
            sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_USUARIO_EMPRESA");
            sql = sql.replaceAll("COD_CLIENTE_MAESTRO", codCliente).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim());
            if (dboracle.executeQuery(sql) != 0) {
                log.debug((Object)"MSG_ERROR_INSERT_ASIGNACION_EMPRESA");
                return false;
            }
            log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_USUARIO_EMPRESA para el cliente " + codCliente));
            dboracle.dbClose();
            sql = this.pSql.getProperty("SQL_REGISTRO_ASIG_USUARIO_PRODUCTO");
            sql = sql.replaceAll("COD_CLIENTE_MAESTRO", codCliente).replaceAll("NVP-USUARIO-BEM-BEP", listaEmp.getUsuario().getNvpUsuarioBemBep().trim());
            if (dboracle.executeQuery(sql) != 0) {
                log.debug((Object)"MSG_ERROR_INSERT_ASIG_USUARIO_PRODUCTO");
                return false;
            }
            log.info((Object)("Se registro correctamente SQL_REGISTRO_ASIG_USUARIO_PRODUCTO para el cliente " + codCliente));
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
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_EMPRESA para la cuenta " + cirifCliente));
            codCliente = dboracle.getFieldString("COD_CLIENTE");
        }
        return codCliente;
    }
}

