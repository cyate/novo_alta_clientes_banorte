/*
 * Decompiled with CFR 0.151.
 */
package com.novo.bean;

import com.novo.bean.Respuesta;
import com.novo.bean.Usuario;

public class Empresa {
    private String nvpNombreEmpresa;
    private String nvpTipoEmpresa;
    private String nvpCuenta;
    private String nvpNumeroEmpresa;
    private String nvpIdServicioG;
    private String nvpNumStockG;
    private String nvpNumReg;
    private String nvpSaldo;
    private String nvpMotivoSrv;
    private boolean reactivacionCuenta;
    private Usuario usuario;
    private Respuesta respuesta;
    private boolean registrarSoloLote;
    private String nvpIndsrvna;
    private String nvpCtaAdmin;

    public Empresa() {
    }

    public Empresa(String nvpNombreEmpresa, String nvpTipoEmpresa, String nvpCuenta, String nvpNumeroEmpresa, String nvpIdServicioG, String nvpNumReg, String nvpMotivoSrv, Usuario usuario, Respuesta respuesta, String nvpCtaAdmin, String nvpIndsrvna) {
        this.nvpNombreEmpresa = nvpNombreEmpresa;
        this.nvpTipoEmpresa = nvpTipoEmpresa;
        this.nvpCuenta = nvpCuenta;
        this.nvpNumeroEmpresa = nvpNumeroEmpresa;
        this.nvpIdServicioG = nvpIdServicioG;
        this.nvpNumReg = nvpNumReg;
        this.nvpMotivoSrv = nvpMotivoSrv;
        this.usuario = usuario;
        this.respuesta = respuesta;
        this.nvpCtaAdmin = nvpCtaAdmin;
        this.nvpIndsrvna = nvpIndsrvna;
    }

    public String getNvpNombreEmpresa() {
        return this.nvpNombreEmpresa;
    }

    public void setNvpNombreEmpresa(String nvpNombreEmpresa) {
        this.nvpNombreEmpresa = nvpNombreEmpresa;
    }

    public String getNvpTipoEmpresa() {
        return this.nvpTipoEmpresa;
    }

    public void setNvpTipoEmpresa(String nvpTipoEmpresa) {
        this.nvpTipoEmpresa = nvpTipoEmpresa;
    }

    public String getNvpCuenta() {
        return this.nvpCuenta;
    }

    public void setNvpCuenta(String nvpCuenta) {
        this.nvpCuenta = nvpCuenta;
    }

    public String getNvpNumeroEmpresa() {
        return this.nvpNumeroEmpresa;
    }

    public void setNvpNumeroEmpresa(String nvpNumeroEmpresa) {
        this.nvpNumeroEmpresa = nvpNumeroEmpresa;
    }

    public String getNvpIdServicioG() {
        return this.nvpIdServicioG;
    }

    public void setNvpIdServicioG(String nvpIdServicioG) {
        this.nvpIdServicioG = nvpIdServicioG;
    }

    public String getNvpNumReg() {
        return this.nvpNumReg;
    }

    public void setNvpNumReg(String nvpNumReg) {
        this.nvpNumReg = nvpNumReg;
    }

    public String getNvpSaldo() {
        return this.nvpSaldo;
    }

    public void setNvpSaldo(String nvpSaldo) {
        this.nvpSaldo = nvpSaldo;
    }

    public String getNvpMotivoSrv() {
        return this.nvpMotivoSrv;
    }

    public void setNvpMotivoSrv(String nvpMotivoSrv) {
        this.nvpMotivoSrv = nvpMotivoSrv;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNvpNumStockG() {
        return this.nvpNumStockG;
    }

    public void setNvpNumStockG(String nvpNumStockG) {
        this.nvpNumStockG = nvpNumStockG;
    }

    public Respuesta getRespuesta() {
        return this.respuesta;
    }

    public void setRespuesta(Respuesta respuesta) {
        this.respuesta = respuesta;
    }

    public boolean isReactivacionCuenta() {
        return this.reactivacionCuenta;
    }

    public void setReactivacionCuenta(boolean reactivacionCuenta) {
        this.reactivacionCuenta = reactivacionCuenta;
    }

    public boolean isRegistrarSoloLote() {
        return this.registrarSoloLote;
    }

    public void setRegistrarSoloLote(boolean registrarSoloLote) {
        this.registrarSoloLote = registrarSoloLote;
    }

    public void setNvpIndsrvna(String nvpIndsrvna) {
        this.nvpIndsrvna = nvpIndsrvna;
    }

    public String getNvpIndsrvna() {
        return this.nvpIndsrvna;
    }

    public void setNvpCtaAdmin(String nvpCtaAdmin) {
        this.nvpCtaAdmin = nvpCtaAdmin;
    }

    public String getNvpCtaAdmin() {
        return this.nvpCtaAdmin;
    }
}

