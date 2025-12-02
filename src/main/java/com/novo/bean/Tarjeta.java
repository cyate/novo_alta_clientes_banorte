/*
 * Decompiled with CFR 0.151.
 */
package com.novo.bean;

import com.novo.bean.Respuesta;

public class Tarjeta {
    private String nvpIdServicioD;
    private String nvpNumStockD;
    private String nvpNumReg;
    private String nvpTipoMotivo;
    private String nvpNumeroTarjeta;
    private String nvpIndEmb;
    private String nvpEmb1;
    private String nvpNombreEmpLar;
    private String nvpApellidoPat;
    private String nvpApellidoMat;
    private String nvpEmb2;
    private String nvpFecExpiracion;
    private Respuesta respuesta;

    public Tarjeta(String nvpIdServicioD, String nvpTipoMotivo, String nvpNumeroTarjeta, String nvpIndEmb, String nvpEmb2, String nvpFecExpiracion) {
        this.nvpIdServicioD = nvpIdServicioD;
        this.nvpTipoMotivo = nvpTipoMotivo;
        this.nvpNumeroTarjeta = nvpNumeroTarjeta;
        this.nvpIndEmb = nvpIndEmb;
        this.nvpEmb2 = nvpEmb2;
        this.nvpFecExpiracion = nvpFecExpiracion;
    }

    public Tarjeta() {
    }

    public String getNvpIdServicioD() {
        return this.nvpIdServicioD;
    }

    public void setNvpIdServicioD(String nvpIdServicioD) {
        this.nvpIdServicioD = nvpIdServicioD;
    }

    public String getNvpNumStockD() {
        return this.nvpNumStockD;
    }

    public void setNvpNumStockD(String nvpNumStockD) {
        this.nvpNumStockD = nvpNumStockD;
    }

    public String getNvpNumReg() {
        return this.nvpNumReg;
    }

    public void setNvpNumReg(String nvpNumReg) {
        this.nvpNumReg = nvpNumReg;
    }

    public String getNvpTipoMotivo() {
        return this.nvpTipoMotivo;
    }

    public void setNvpTipoMotivo(String nvpTipoMotivo) {
        this.nvpTipoMotivo = nvpTipoMotivo;
    }

    public String getNvpNumeroTarjeta() {
        return this.nvpNumeroTarjeta;
    }

    public void setNvpNumeroTarjeta(String nvpNumeroTarjeta) {
        this.nvpNumeroTarjeta = nvpNumeroTarjeta;
    }

    public String getNvpIndEmb() {
        return this.nvpIndEmb;
    }

    public void setNvpIndEmb(String nvpIndEmb) {
        this.nvpIndEmb = nvpIndEmb;
    }

    public String getNvpEmb2() {
        return this.nvpEmb2;
    }

    public String getNvpEmb1() {
        return this.nvpEmb1;
    }

    public void setNvpEmb1(String nvpEmb1) {
        this.nvpEmb1 = nvpEmb1;
    }

    public void setNvpEmb2(String nvpEmb2) {
        this.nvpEmb2 = nvpEmb2;
    }

    public String getNvpFecExpiracion() {
        return this.nvpFecExpiracion;
    }

    public void setNvpFecExpiracion(String nvpFecExpiracion) {
        this.nvpFecExpiracion = nvpFecExpiracion;
    }

    public String getNvpNombreEmpLar() {
        return this.nvpNombreEmpLar;
    }

    public void setNvpNombreEmpLar(String nvpNombreEmpLar) {
        this.nvpNombreEmpLar = nvpNombreEmpLar;
    }

    public String getNvpApellidoPat() {
        return this.nvpApellidoPat;
    }

    public void setNvpApellidoPat(String nvpApellidoPat) {
        this.nvpApellidoPat = nvpApellidoPat;
    }

    public String getNvpApellidoMat() {
        return this.nvpApellidoMat;
    }

    public void setNvpApellidoMat(String nvpApellidoMat) {
        this.nvpApellidoMat = nvpApellidoMat;
    }

    public Respuesta getRespuesta() {
        return this.respuesta;
    }

    public void setRespuesta(Respuesta respuesta) {
        this.respuesta = respuesta;
    }
}

