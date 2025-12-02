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
import com.novo.bean.Respuesta;
import com.novo.bean.Tarjeta;
import com.novo.bean.Usuario;
import com.novo.database.dbinterface;
import com.novo.utils.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ValidadorProcessor {
    public static final String CONSTANTS_CONFIG = "constant_config.properties";
    public static final String CONSTANTS_PROCESS = "constant_process.properties";
    private static Log log = LogFactory.getLog((String)ValidadorProcessor.class.getName());
    public static List<List<String>> listaArchivo = new ArrayList<List<String>>();
    private static List<String> listaLinea = new ArrayList<String>();

    public static boolean verifyFormatInterfaceAlta() throws IOException {
        boolean bandera = true;
        FileReader fr = null;
        Properties p = Utils.getProperties(CONSTANTS_CONFIG);
        Date fecActual = new Date();
        String format = "yyMMdd";
        SimpleDateFormat objDateF = new SimpleDateFormat(format);
        String fecFormat = objDateF.format(fecActual);
        String nameFile = p.getProperty("FILE_NAME").replaceAll("%%ODATE", fecFormat);
        String file = p.getProperty("FILE_PATH_ENTRADA") + nameFile + p.getProperty("FILE_EXT");
        try {
            String cadena;
            String separador = p.getProperty("FIELD_SEPARATOR");
            String general = p.getProperty("FIELD_GENERAL");
            String detalle = p.getProperty("FIELD_DETAIL");
            String control = p.getProperty("FIELD_CONTROL");
            File f = new File(file);
            if (!f.exists()) {
                return false;
            }
            fr = new FileReader(f);
            BufferedReader bf = new BufferedReader(fr);
            int numero = 0;
            while ((cadena = bf.readLine()) != null) {
                String[] linea = cadena.split("\\" + separador);
                System.out.println("Longitud Linea: " + linea.length);
                if (linea[0].equalsIgnoreCase(general)) {
                    bandera = ValidadorProcessor.checkInterfaceAlta("GENERAL", linea);
                    listaArchivo.add(numero, listaLinea);
                } else if (linea[0].equalsIgnoreCase(detalle)) {
                    bandera = ValidadorProcessor.checkInterfaceAlta("DETAIL", linea);
                    listaArchivo.add(numero, listaLinea);
                } else if (linea[0].equalsIgnoreCase(control)) {
                    bandera = ValidadorProcessor.checkInterfaceAlta("CONTROL", linea);
                    listaArchivo.add(numero, listaLinea);
                }
                if (!bandera) {
                    listaArchivo = new ArrayList<List<String>>();
                    return false;
                }
                listaLinea = new ArrayList<String>();
                ++numero;
            }
            fr.close();
            return true;
        }
        catch (FileNotFoundException ex) {
            log.fatal((Object)(ValidadorProcessor.class.getName() + " " + ex));
            return true;
        }
    }

    public static boolean checkInterfaceAlta(String tipo, String[] linea) throws FileNotFoundException, IOException {
        try {
            Properties pGeneral = Utils.getProperties(CONSTANTS_PROCESS);
            int numeroAtributos = Integer.parseInt(pGeneral.getProperty("NUMBER_" + tipo + "_ATRIBUTES"));
            System.out.println("Numero de Atributos: " + numeroAtributos);
            for (int x = 0; x < numeroAtributos; ++x) {
                try {
                    System.out.println("Check Interface Alta Atributo: " + linea[x]);
                    listaLinea.add(x, linea[x]);
                    continue;
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Se produjo un ArrayIndexOutOfBoundsException: " + e.getMessage());
                    listaLinea.add(x, "");
                }
            }
            return true;
        }
        catch (IOException ex) {
            log.fatal((Object)(ValidadorProcessor.class.getName() + " " + ex));
            return true;
        }
    }

    public static void verifyDataInterfaceAlta(dbinterface dboracle, Properties pSql, Properties pGeneral, ArrayList<Empresa> listaEmp, ArrayList<Tarjeta> listaTar) throws IOException, SQLException {
        int i;
        Respuesta objRespuesta = new Respuesta();
        Properties pConfig = Utils.getProperties(CONSTANTS_CONFIG);
        String descRespError = ValidadorProcessor.addResponseDesc(pConfig.getProperty("RESPONSE_ERROR_GENERAL_DESC"));
        objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
        objRespuesta.setNvpRespuesta(descRespError);
        for (i = 0; i < listaEmp.size(); ++i) {
            String respuesta;
            if (!"A".equals(listaEmp.get(i).getRespuesta().getNvpResultado())) continue;
            int cantRegistros = Integer.parseInt(listaEmp.get(i).getNvpNumReg().replaceFirst("^0+(?!$)", ""));
            int cantStock = Integer.parseInt(listaEmp.get(i).getNvpNumStockG().replaceFirst("^0+(?!$)", ""));
            int contadorReg = 0;
            log.info((Object)("CANTIDAD DE REGISTRO: " + cantRegistros));
            log.info((Object)("CANTIDAD DE STOCK: " + cantStock));
            if (cantStock != 0 && cantRegistros == 0 || cantStock == 0 && cantRegistros != 0) {
                log.info((Object)("Error Hay incosistencias entre stocks para el IdServicio: " + listaEmp.get(i).getNvpIdServicioG().trim() + " entonces se rechazara"));
                objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Diferencia entre stock y nro de tarjetas");
                listaEmp.get(i).setRespuesta(objRespuesta);
            }
            for (int j = 0; j < listaTar.size(); ++j) {
                if (!listaEmp.get(i).getNvpIdServicioG().trim().equals(listaTar.get(j).getNvpIdServicioD().trim()) || !listaEmp.get(i).getNvpNumStockG().trim().equals(listaTar.get(j).getNvpNumStockD().trim())) continue;
                ++contadorReg;
            }
            if (contadorReg != cantRegistros) {
                log.info((Object)("Error No existe la cantidad de tarjetas esperadas para el IdServicio: " + listaEmp.get(i).getNvpIdServicioG().trim() + " entonces se rechazara"));
                objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Cantidad de tarjetas no es la esperada");
                listaEmp.get(i).setRespuesta(objRespuesta);
            }
            if (listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("N")) {
                log.info((Object)("CHECK 1=" + listaEmp.get(i).getNvpIdServicioG().trim().substring(0, 10).equalsIgnoreCase(listaEmp.get(i).getNvpCtaAdmin())));
            } else if (listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("A")) {
                log.info((Object)("CHECK 1=" + listaEmp.get(i).getNvpIdServicioG().trim().substring(0, 10).equalsIgnoreCase(listaEmp.get(i).getNvpCuenta())));
            }
            log.info((Object)("CHECK 2=" + listaEmp.get(i).getNvpIdServicioG().trim().substring(10, 20).equalsIgnoreCase(listaEmp.get(i).getNvpNumeroEmpresa())));
            log.info((Object)("CHECK 3=" + listaEmp.get(i).getNvpIdServicioG().trim().substring(20, 21).equalsIgnoreCase(listaEmp.get(i).getNvpTipoEmpresa().trim().substring(2, 3))));
            if (listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("N")) {
                if (!(listaEmp.get(i).getNvpIdServicioG().trim().substring(0, 10).equalsIgnoreCase(listaEmp.get(i).getNvpCtaAdmin()) && listaEmp.get(i).getNvpIdServicioG().trim().substring(10, 20).equalsIgnoreCase(listaEmp.get(i).getNvpNumeroEmpresa()) && listaEmp.get(i).getNvpIdServicioG().trim().substring(20, 21).equalsIgnoreCase(listaEmp.get(i).getNvpTipoEmpresa().trim().substring(2, 3)))) {
                    log.info((Object)("Error No coincide el IdServicio: " + listaEmp.get(i).getNvpIdServicioG().trim() + " con los datos de cuenta modelo nuevo - empresa - tipo empresa entonces se rechazara"));
                    objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Diferencias entre nro cuenta modelo nuevo - empresa - idServicio");
                    listaEmp.get(i).setRespuesta(objRespuesta);
                }
            } else if (!(!listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("A") || listaEmp.get(i).getNvpIdServicioG().trim().substring(0, 10).equalsIgnoreCase(listaEmp.get(i).getNvpCuenta()) && listaEmp.get(i).getNvpIdServicioG().trim().substring(10, 20).equalsIgnoreCase(listaEmp.get(i).getNvpNumeroEmpresa()) && listaEmp.get(i).getNvpIdServicioG().trim().substring(20, 21).equalsIgnoreCase(listaEmp.get(i).getNvpTipoEmpresa().trim().substring(2, 3)))) {
                log.info((Object)("Error No coincide el IdServicio: " + listaEmp.get(i).getNvpIdServicioG().trim() + " con los datos de cuenta modelo actual - empresa - tipo empresa entonces se rechazara"));
                objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Diferencias entre nro cuenta modelo actual - empresa - idServicio");
                listaEmp.get(i).setRespuesta(objRespuesta);
            }
            if ("A".equals(listaEmp.get(i).getNvpMotivoSrv().trim())) {
                boolean existUser;
                String respuesta2 = ValidadorProcessor.getCodigoCliente(dboracle, listaEmp.get(i).getNvpCtaAdmin().trim(), pSql);
                if (!respuesta2.isEmpty()) {
                    if ("0001".equalsIgnoreCase(listaEmp.get(i).getNvpNumStockG())) {
                        if ("A".equalsIgnoreCase(listaEmp.get(i).getRespuesta().getNvpResultado())) {
                            log.info((Object)("Se registrara solo el lote para la cuenta: " + listaEmp.get(i).getNvpCuenta().trim() + " ya que existe y se encuentra activa siendo su primer lote"));
                            listaEmp.get(i).setRegistrarSoloLote(true);
                        }
                    } else {
                        log.info((Object)("Error Se rechazara porque ya existe la cuenta numero: " + listaEmp.get(i).getNvpCuenta().trim() + " y se cuentra Activa, siendo un motivo de Alta A"));
                        objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Ya existe la cuenta para ser un Alta, no es su stock (0001/0000)");
                        listaEmp.get(i).setRespuesta(objRespuesta);
                    }
                } else {
                    String respuestaInactiva = ValidadorProcessor.getCodigoClienteInactivo(dboracle, listaEmp.get(i).getNvpCtaAdmin().trim(), pSql);
                    if (!respuestaInactiva.isEmpty()) {
                        listaEmp.get(i).setReactivacionCuenta(true);
                    }
                }
                if (existUser = ValidadorProcessor.getExistUsuario(dboracle, listaEmp.get(i).getUsuario().getNvpUsuarioBemBep().trim(), listaEmp.get(i).getUsuario().getNvpCurp().trim().toUpperCase(), pSql)) {
                    listaEmp.get(i).getUsuario().setExist(true);
                    boolean existUserReactivar = ValidadorProcessor.getExistUsuarioInactivo(dboracle, listaEmp.get(i).getUsuario().getNvpUsuarioBemBep().trim(), listaEmp.get(i).getUsuario().getNvpCurp().trim().toUpperCase(), pSql);
                    if (existUserReactivar) {
                        listaEmp.get(i).getUsuario().setReactivacionUsuario(true);
                    }
                }
            }
            if (listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("N") && listaEmp.get(i).getNvpMotivoSrv().equalsIgnoreCase("M")) {
                String respuesta3 = ValidadorProcessor.getCodigoCliente(dboracle, listaEmp.get(i).getNvpCtaAdmin().trim(), pSql);
                if (respuesta3.isEmpty()) {
                    log.info((Object)("Error No existe o esta cancelada la cuenta numero: " + listaEmp.get(i).getNvpCtaAdmin().trim() + " para altas subsecuentes (M), entonces se rechazara"));
                    objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; No existe la cuenta o esta cancelada para altas subsecuentes (M)");
                    listaEmp.get(i).setRespuesta(objRespuesta);
                }
            } else if (listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("A") && listaEmp.get(i).getNvpMotivoSrv().equalsIgnoreCase("M") && (respuesta = ValidadorProcessor.getCodigoCliente(dboracle, listaEmp.get(i).getNvpCuenta().trim(), pSql)).isEmpty()) {
                log.info((Object)("Error No existe o esta cancelada la cuenta numero: " + listaEmp.get(i).getNvpCuenta().trim() + " para altas subsecuentes (M), entonces se rechazara"));
                objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; No existe la cuenta o esta cancelada para altas subsecuentes (M)");
                listaEmp.get(i).setRespuesta(objRespuesta);
            }
            if (listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("N")) {
                log.info((Object)("listaEmp.get(i).getNvpCtaAdmin() :" + listaEmp.get(i).getNvpCtaAdmin()));
                if (listaEmp.get(i).getNvpCtaAdmin().contains(" ")) {
                    log.info((Object)("Error en el Numero de Cuenta Administradora: " + listaEmp.get(i).getNvpCtaAdmin() + ". No se permite ingresar con espacio en blanco."));
                    objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; El Numero de Cuenta Administradora no permite espacio en blanco.");
                    listaEmp.get(i).setRespuesta(objRespuesta);
                }
            } else if (listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("A")) {
                log.info((Object)("listaEmp.get(i).getNvpCuenta() :" + listaEmp.get(i).getNvpCuenta()));
                if (listaEmp.get(i).getNvpCuenta().contains(" ")) {
                    log.info((Object)("Error en el Numero de Cuenta Concentradora: " + listaEmp.get(i).getNvpCuenta() + ". No se permite ingresar con espacio en blanco."));
                    objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; El Numero de Cuenta Concentradora no permite espacio en blanco.");
                    listaEmp.get(i).setRespuesta(objRespuesta);
                }
            }
            log.info((Object)("listaEmp.get(i).getNvpIndsrvna(): " + listaEmp.get(i).getNvpIndsrvna()));
            if (!listaEmp.get(i).getNvpIndsrvna().equals(String.valueOf("N")) && !listaEmp.get(i).getNvpIndsrvna().equals(String.valueOf("A"))) {
                log.info((Object)("Error en el Indicador de Servicio: '" + listaEmp.get(i).getNvpIndsrvna() + "'. El caracter solo puede tomar valor de 'A'(SERVICIO ANTERIOR) o 'N'(SERVICIO NUEVO)."));
                objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; El Caracter Indicador de Servicio solo permite valores 'A' o 'B'.");
                listaEmp.get(i).setRespuesta(objRespuesta);
            }
            if (listaEmp.get(i).getNvpIndsrvna().isEmpty() || listaEmp.get(i).getNvpIndsrvna().equals("")) {
                log.info((Object)"Error en el Indicador de Servicio:  NO PUEDE SER VACIO.");
                objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "'. El caracter NO PUEDE SER VACIO.");
                listaEmp.get(i).setRespuesta(objRespuesta);
            }
            if (!listaEmp.get(i).getNvpIndsrvna().equalsIgnoreCase("A") || !listaEmp.get(i).getNvpMotivoSrv().equalsIgnoreCase("A")) continue;
            log.info((Object)"Error en el Indicador de Servicio:  NO SE PUEDE DAR DE ALTA A UNA CUENTA-EMPRESA CON MODELO ACTUAL.");
            objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "'. NO SE PUEDE DAR DE ALTA A UNA CUENTA-EMPRESA CON MODELO ACTUAL.");
            listaEmp.get(i).setRespuesta(objRespuesta);
        }
        for (i = 0; i < listaEmp.size(); ++i) {
            if (!"R".equals(listaEmp.get(i).getRespuesta().getNvpResultado())) continue;
            for (int j = 0; j < listaTar.size(); ++j) {
                if (!listaTar.get(j).getNvpIdServicioD().equals(listaEmp.get(i).getNvpIdServicioG()) || !listaTar.get(j).getNvpNumStockD().equals(listaEmp.get(i).getNvpNumStockG())) continue;
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                objRespuesta.setNvpRespuesta(descRespError);
                listaTar.get(j).setRespuesta(objRespuesta);
            }
        }
    }

    public static void verifyExistNroCuenta(dbinterface dboracle, Properties pSql, Properties pGeneral, ArrayList<Empresa> listaEmp, ArrayList<Tarjeta> listaTar) throws IOException, SQLException {
        int i;
        Respuesta objRespuesta = new Respuesta();
        String tarjeta = "";
        String numEmp = "";
        Properties pConfig = Utils.getProperties(CONSTANTS_CONFIG);
        String descRespError = ValidadorProcessor.addResponseDesc(pConfig.getProperty("RESPONSE_ERROR_GENERAL_DESC"));
        objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
        objRespuesta.setNvpRespuesta(descRespError);
        ArrayList<String> listEmpresaError = new ArrayList<String>();
        for (i = 0; i < listaEmp.size(); ++i) {
            if (!"A".equals(listaEmp.get(i).getRespuesta().getNvpResultado().trim())) continue;
            for (int j = 0; j < listaTar.size(); ++j) {
                String[] infoLote;
                String union;
                String[] persona;
                if (!"A".equals(listaTar.get(j).getRespuesta().getNvpResultado().trim()) || !listaTar.get(j).getNvpIdServicioD().equals(listaEmp.get(i).getNvpIdServicioG()) || !listaTar.get(j).getNvpNumStockD().equals(listaEmp.get(i).getNvpNumStockG())) continue;
                tarjeta = listaTar.get(j).getNvpNumeroTarjeta().trim();
                boolean existNroCuenta = ValidadorProcessor.getExistCuenta(dboracle, tarjeta, numEmp = listaTar.get(j).getNvpIdServicioD().trim().substring(0, 10), pSql);
                if (existNroCuenta) {
                    log.info((Object)("Error Ya Existe el numero de tarjeta: " + listaTar.get(j).getNvpNumeroTarjeta().trim() + " siendo un motivo de rechazo"));
                    objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                    objRespuesta.setNvpRespuesta(descRespError);
                    objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Existe la tarjeta: " + listaTar.get(j).getNvpNumeroTarjeta() + " en BD");
                    listaTar.get(j).setRespuesta(objRespuesta);
                    String union2 = numEmp + "," + listaTar.get(j).getNvpNumStockD().trim() + ",EXIST";
                    listEmpresaError.add(union2);
                }
                if (!"M".equals(listaEmp.get(i).getNvpMotivoSrv().trim()) && (!"A".equals(listaEmp.get(i).getNvpMotivoSrv().trim()) || !listaEmp.get(i).isRegistrarSoloLote())) continue;
                if ("N".equals(listaTar.get(j).getNvpIndEmb().trim()) && (persona = ValidadorProcessor.getPersonaNominada(dboracle, listaTar.get(j).getNvpIdServicioD().trim(), listaTar.get(j).getNvpNumStockD().trim(), listaTar.get(j).getNvpNumReg().trim(), pSql))[0] == null) {
                    log.info((Object)("Error No hay persona nominada para el numero de tarjeta: " + listaTar.get(j).getNvpNumeroTarjeta().trim() + " siendo un motivo de rechazo"));
                    objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                    objRespuesta.setNvpRespuesta(descRespError);
                    listaTar.get(j).setRespuesta(objRespuesta);
                    union = numEmp + "," + listaTar.get(j).getNvpNumStockD().trim() + ",N";
                    listEmpresaError.add(union);
                }
                if (!"I".equals(listaTar.get(j).getNvpIndEmb().trim()) || (infoLote = ValidadorProcessor.getLoteInnoSubsecuente(dboracle, listaTar.get(j).getNvpIdServicioD().trim(), listaTar.get(j).getNvpNumStockD().trim(), pSql))[0] != null) continue;
                log.info((Object)("Error No se encontro la relacion correcta del lote con la tabla TEB_LOTE_EMISION_TI para el numero de tarjeta: " + listaTar.get(j).getNvpNumeroTarjeta().trim() + " puede ser el BN_IDSERVICIOS - BN_STOCK siendo un motivo de rechazo"));
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                objRespuesta.setNvpRespuesta(descRespError);
                listaTar.get(j).setRespuesta(objRespuesta);
                union = numEmp + "," + listaTar.get(j).getNvpNumStockD().trim() + ",I";
                listEmpresaError.add(union);
            }
        }
        for (int x = 0; x < listEmpresaError.size(); ++x) {
            String[] dividido = ((String)listEmpresaError.get(x)).split(",");
            for (int y = 0; y < listaEmp.size(); ++y) {
                if (!"A".equals(listaEmp.get(y).getRespuesta().getNvpResultado().trim())) continue;
                if (listaEmp.get(y).getNvpIndsrvna().equalsIgnoreCase("A") && dividido[0].equals(listaEmp.get(y).getNvpCuenta().trim()) && dividido[1].equals(listaEmp.get(y).getNvpNumStockG().trim())) {
                    if (!dividido[2].isEmpty()) {
                        objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Problema con el Lote de tarjetas debido a que no exista o puede que no coincida el nro stock y/o idServicio con BD");
                    }
                    objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                    objRespuesta.setNvpRespuesta(descRespError);
                    listaEmp.get(y).setRespuesta(objRespuesta);
                    continue;
                }
                if (!listaEmp.get(y).getNvpIndsrvna().equalsIgnoreCase("N") || !dividido[0].equals(listaEmp.get(y).getNvpCtaAdmin().trim()) || !dividido[1].equals(listaEmp.get(y).getNvpNumStockG().trim())) continue;
                if (!dividido[2].isEmpty()) {
                    objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; Problema con el Lote de tarjetas debido a que no exista o puede que no coincida el nro stock y/o idServicio con BD");
                }
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                objRespuesta.setNvpRespuesta(descRespError);
                listaEmp.get(y).setRespuesta(objRespuesta);
            }
        }
        for (i = 0; i < listaEmp.size(); ++i) {
            if (!"R".equals(listaEmp.get(i).getRespuesta().getNvpResultado())) continue;
            for (int j = 0; j < listaTar.size(); ++j) {
                if (!listaTar.get(j).getNvpIdServicioD().equals(listaEmp.get(i).getNvpIdServicioG()) || !listaTar.get(j).getNvpNumStockD().equals(listaEmp.get(i).getNvpNumStockG())) continue;
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                objRespuesta.setNvpRespuesta(descRespError);
                listaTar.get(j).setRespuesta(objRespuesta);
            }
        }
    }

    public static ArrayList<Empresa> getListEmpresa(Properties pGeneral) throws IOException {
        ArrayList<Empresa> listaEmpresa = new ArrayList<Empresa>();
        Properties pConfig = Utils.getProperties(CONSTANTS_CONFIG);
        String descRespOk = ValidadorProcessor.addResponseDesc(pConfig.getProperty("RESPONSE_OK_DESC"));
        String descRespError = ValidadorProcessor.addResponseDesc(pConfig.getProperty("RESPONSE_ERROR_GENERAL_DESC"));
        for (int i = 0; i < listaArchivo.size(); ++i) {
            if (!listaArchivo.get(i).get(0).equals(pConfig.getProperty("FIELD_GENERAL"))) continue;
            Empresa objEmpresa = new Empresa();
            Usuario objUsuario = new Usuario();
            Respuesta objRespuesta = new Respuesta();
            List<String> listaAuxiliar = listaArchivo.get(i);
            boolean flagPermiteRegistro = true;
            for (int j = 0; j < listaAuxiliar.size(); ++j) {
                String valorTipo = "FIELD_GENERAL_TYPE_" + j;
                String tipoDato = pGeneral.getProperty(valorTipo);
                System.out.println(valorTipo);
                System.out.println("Tipo Dato: " + tipoDato);
                System.out.println("Tipo Lista Auxiliar: " + listaAuxiliar.get(j).toString());
                if (Pattern.matches(tipoDato, listaAuxiliar.get(j))) continue;
                System.out.println("No coincide la longitud o el tipo de dato " + listaAuxiliar.get(j).toString());
                log.error((Object)("No coincide la longitud o el tipo de dato " + listaAuxiliar.get(j).toString()));
                flagPermiteRegistro = false;
            }
            System.out.println("SALE DEL BUCLE");
            objEmpresa.setNvpIdServicioG(listaAuxiliar.get(1));
            objEmpresa.setNvpNumStockG(listaAuxiliar.get(2));
            objEmpresa.setNvpNumReg(listaAuxiliar.get(3));
            objEmpresa.setNvpMotivoSrv(listaAuxiliar.get(5));
            objEmpresa.setNvpCuenta(listaAuxiliar.get(6));
            objEmpresa.setNvpNumeroEmpresa(listaAuxiliar.get(7));
            objEmpresa.setNvpTipoEmpresa(listaAuxiliar.get(8));
            objEmpresa.setNvpSaldo(listaAuxiliar.get(11));
            objEmpresa.setNvpNombreEmpresa(listaAuxiliar.get(17).toUpperCase());
            objEmpresa.setRegistrarSoloLote(false);
            objUsuario.setNvpUsuarioBemBep(listaAuxiliar.get(13).toUpperCase());
            objUsuario.setNvpCorreoUsuario(listaAuxiliar.get(14).toUpperCase());
            objUsuario.setNvpNombreUsuario(listaAuxiliar.get(15).toUpperCase());
            objUsuario.setNvpCurp(listaAuxiliar.get(18).toUpperCase());
            objUsuario.setExist(false);
            objUsuario.setReactivacionUsuario(false);
            objEmpresa.setUsuario(objUsuario);
            System.out.println("NUM19: " + listaAuxiliar.get(19).toString());
            objEmpresa.setNvpCtaAdmin(listaAuxiliar.get(19));
            objEmpresa.setNvpIndsrvna(listaAuxiliar.get(20).toUpperCase());
            if (flagPermiteRegistro) {
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_OK"));
                objRespuesta.setNvpRespuesta(descRespOk);
            } else {
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; No coincide la longitud o el tipo de dato para esta trama general");
                objRespuesta.setNvpRespuesta(descRespError);
            }
            objEmpresa.setRespuesta(objRespuesta);
            objEmpresa.setReactivacionCuenta(false);
            System.out.println("Objeto Empresa: " + objEmpresa.toString());
            listaEmpresa.add(objEmpresa);
        }
        return listaEmpresa;
    }

    public static ArrayList<Tarjeta> getListTarjeta(Properties pGeneral, ArrayList<Empresa> listaEmp) throws IOException {
        ArrayList<Tarjeta> listaTarjeta = new ArrayList<Tarjeta>();
        Properties pConfig = Utils.getProperties(CONSTANTS_CONFIG);
        ArrayList<String> listaTarjError = new ArrayList<String>();
        String descRespOk = ValidadorProcessor.addResponseDesc(pConfig.getProperty("RESPONSE_OK_DESC"));
        String descRespError = ValidadorProcessor.addResponseDesc(pConfig.getProperty("RESPONSE_ERROR_GENERAL_DESC"));
        for (int i = 0; i < listaArchivo.size(); ++i) {
            if (!listaArchivo.get(i).get(0).equals(pConfig.getProperty("FIELD_DETAIL"))) continue;
            Tarjeta objTarjeta = new Tarjeta();
            Respuesta objRespuesta = new Respuesta();
            List<String> listaAuxiliar = listaArchivo.get(i);
            for (int j = 0; j < listaAuxiliar.size(); ++j) {
                String valorTipo = "FIELD_DETAIL_TYPE_" + j;
                String tipoDato = pGeneral.getProperty(valorTipo);
                System.out.println("Tipo Dato: " + tipoDato);
                System.out.println("Tipo Dato Lista: " + listaAuxiliar.get(j));
                if (Pattern.matches(tipoDato, listaAuxiliar.get(j))) continue;
                System.out.println("No coincide la longitud o el tipo de dato " + listaAuxiliar.get(j));
                log.error((Object)("No coincide la longitud o el tipo de dato " + listaAuxiliar.get(j)));
                String union = listaAuxiliar.get(1) + "," + listaAuxiliar.get(2);
                listaTarjError.add(union);
            }
            objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_OK"));
            objRespuesta.setNvpRespuesta(descRespOk);
            if ("N".equals(listaAuxiliar.get(6)) && listaAuxiliar.get(7).trim().isEmpty()) {
                String union = listaAuxiliar.get(1) + "," + listaAuxiliar.get(2);
                listaTarjError.add(union);
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                objRespuesta.setNvpRespuesta(descRespError);
            }
            objTarjeta.setNvpIdServicioD(listaAuxiliar.get(1));
            objTarjeta.setNvpNumStockD(listaAuxiliar.get(2));
            objTarjeta.setNvpNumReg(listaAuxiliar.get(3));
            objTarjeta.setNvpTipoMotivo(listaAuxiliar.get(4));
            objTarjeta.setNvpNumeroTarjeta(listaAuxiliar.get(5));
            objTarjeta.setNvpIndEmb(listaAuxiliar.get(6));
            objTarjeta.setNvpEmb1(listaAuxiliar.get(7).toUpperCase());
            objTarjeta.setNvpNombreEmpLar(listaAuxiliar.get(8).toUpperCase());
            objTarjeta.setNvpApellidoPat(listaAuxiliar.get(9).toUpperCase());
            objTarjeta.setNvpApellidoMat(listaAuxiliar.get(10).toUpperCase());
            objTarjeta.setNvpEmb2(listaAuxiliar.get(11).toUpperCase());
            objTarjeta.setNvpFecExpiracion(listaAuxiliar.get(12));
            objTarjeta.setRespuesta(objRespuesta);
            System.out.println("Objeto Tarjeta: " + objTarjeta.toString());
            listaTarjeta.add(objTarjeta);
        }
        if (!listaTarjError.isEmpty()) {
            Respuesta objRespuesta = new Respuesta();
            objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
            objRespuesta.setNvpRespuesta(descRespError);
            for (int x = 0; x < listaTarjError.size(); ++x) {
                String[] dividido = ((String)listaTarjError.get(x)).split(",");
                for (int z = 0; z < listaEmp.size(); ++z) {
                    if (!dividido[0].equals(listaEmp.get(z).getNvpIdServicioG().trim()) || !dividido[1].equals(listaEmp.get(z).getNvpNumStockG().trim())) continue;
                    objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                    objRespuesta.setNvpDetalleResp(objRespuesta.getNvpDetalleResp() + "; No coincide la longitud o el tipo de dato de alguna trama de sus tarjetas");
                    objRespuesta.setNvpRespuesta(descRespError);
                    listaEmp.get(z).setRespuesta(objRespuesta);
                }
                for (int y = 0; y < listaTarjeta.size(); ++y) {
                    if (!dividido[0].equals(listaTarjeta.get(y).getNvpIdServicioD().trim()) || !dividido[1].equals(listaTarjeta.get(y).getNvpNumStockD().trim())) continue;
                    listaTarjeta.get(y).setRespuesta(objRespuesta);
                }
            }
        }
        Respuesta objRespuesta = new Respuesta();
        for (int i = 0; i < listaEmp.size(); ++i) {
            if (!"R".equals(listaEmp.get(i).getRespuesta().getNvpResultado())) continue;
            for (int j = 0; j < listaTarjeta.size(); ++j) {
                if (!listaTarjeta.get(j).getNvpIdServicioD().equals(listaEmp.get(i).getNvpIdServicioG()) || !listaTarjeta.get(j).getNvpNumStockD().equals(listaEmp.get(i).getNvpNumStockG())) continue;
                objRespuesta.setNvpResultado(pConfig.getProperty("RESPONSE_ERROR_GENERAL"));
                objRespuesta.setNvpRespuesta(descRespError);
                listaTarjeta.get(j).setRespuesta(objRespuesta);
            }
        }
        return listaTarjeta;
    }

    public static String addResponseDesc(String descResponse) {
        int c = 20 - descResponse.length();
        for (int y = 0; y < c; ++y) {
            descResponse = descResponse + " ";
        }
        return descResponse;
    }

    public static String getCodigoCliente(dbinterface dboracle, String cirifCliente, Properties pSql) throws SQLException {
        String sql = pSql.getProperty("SQL_CONSULTA_EMPRESA");
        String codCliente = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-CTA-ADMIN", cirifCliente);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_EMPRESA");
            dboracle.dbClose();
            return "";
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_EMPRESA para la cuenta " + cirifCliente));
            codCliente = dboracle.getFieldString("COD_CLIENTE");
        }
        dboracle.dbClose();
        return codCliente;
    }

    public static String getCodigoClienteInactivo(dbinterface dboracle, String cirifCliente, Properties pSql) throws SQLException {
        String sql = pSql.getProperty("SQL_CONSULTA_EMPRESA_INACTIVA");
        String codCliente = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-CTA-ADMIN", cirifCliente);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_EMPRESA_INACTIVA");
            dboracle.dbClose();
            return "";
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_EMPRESA_INACTIVA para la cuenta " + cirifCliente));
            codCliente = dboracle.getFieldString("COD_CLIENTE");
        }
        dboracle.dbClose();
        return codCliente;
    }

    public static boolean getExistCuenta(dbinterface dboracle, String tarjeta, String numCta, Properties pSql) throws SQLException {
        String sql = pSql.getProperty("SQL_CONSULTA_CUENTA");
        String nroCuenta = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-NUM-TAR", "0000" + tarjeta);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_CUENTA");
            dboracle.dbClose();
            return false;
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_CUENTA " + tarjeta));
            nroCuenta = dboracle.getFieldString("NRO_CUENTA");
            if (!nroCuenta.isEmpty()) {
                dboracle.dbClose();
                return true;
            }
        }
        dboracle.dbClose();
        return false;
    }

    public static String[] getPersonaNominada(dbinterface dboracle, String idServicio, String stock, String numReg, Properties pSql) throws SQLException {
        String sql = pSql.getProperty("SQL_CONSULTA_DATOS_PERSONA");
        String[] persona = new String[5];
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-STATUS", "0").replaceAll("NVP-IDSERVICIO-D", idServicio).replaceAll("NVP-NUM-STOCK-D", stock).replaceAll("NVP-SEC-TAR", numReg);
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

    public static boolean existTarjetaCorrect(ArrayList<Tarjeta> listaTar) {
        int contadorTarjCorrecta = 0;
        for (int i = 0; i < listaTar.size(); ++i) {
            if (!"A".equals(listaTar.get(i).getRespuesta().getNvpResultado().trim())) continue;
            ++contadorTarjCorrecta;
        }
        return contadorTarjCorrecta != 0;
    }

    public static String[] getLoteInnoSubsecuente(dbinterface dboracle, String idServicio, String stock, Properties pSql) throws SQLException {
        String sql = pSql.getProperty("SQL_CONSULTA_LOTE_INNO_M");
        String[] infoLote = new String[2];
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-IDSERVICIO-D", idServicio).replaceAll("NVP-NUM-STOCK-D", stock);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_SQL_CONSULTA_LOTE_INNO_M");
            dboracle.dbClose();
            return null;
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente CONSULTA_LOTE_INNO_M " + idServicio + " " + stock));
            infoLote[0] = dboracle.getFieldValue("NRO_LOTE");
            infoLote[1] = dboracle.getFieldValue("ID_LOTE");
        }
        dboracle.dbClose();
        return infoLote;
    }

    public static boolean getExistUsuario(dbinterface dboracle, String user, String curp, Properties pSql) throws SQLException {
        String sql = pSql.getProperty("SQL_CONSULTA_USUARIO");
        String usuario = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-USUARIO-BEM-BEP", user);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_USUARIO");
            dboracle.dbClose();
            return false;
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_USUARIO " + user));
            usuario = dboracle.getFieldString("ACCODUSUARIO");
            if (!usuario.isEmpty()) {
                dboracle.dbClose();
                return true;
            }
        }
        dboracle.dbClose();
        return false;
    }

    public static boolean getExistUsuarioInactivo(dbinterface dboracle, String user, String curp, Properties pSql) throws SQLException {
        String sql = pSql.getProperty("SQL_CONSULTA_USUARIO_INACTIVO");
        String usuario = "";
        dboracle.dbreset();
        sql = sql.replaceAll("NVP-USUARIO-BEM-BEP", user);
        if (dboracle.executeQuery(sql) != 0) {
            log.debug((Object)"MSG_ERROR_CONSULTA_USUARIO_INACTIVO");
            dboracle.dbClose();
            return false;
        }
        if (dboracle.nextRecord()) {
            log.info((Object)("Se obtuvo correctamente SQL_CONSULTA_USUARIO_INACTIVO " + user));
            usuario = dboracle.getFieldString("ACCODUSUARIO");
            if (!usuario.isEmpty()) {
                dboracle.dbClose();
                return true;
            }
        }
        dboracle.dbClose();
        return false;
    }
}

