/*
 * Decompiled with CFR 0.151.
 */
package com.novo.bean;

public class Respuesta {
    private String nvpResultado;
    private String nvpRespuesta;
    private String nvpDetalleResp;

    public Respuesta(String nvpResultado, String nvpRespuesta) {
        this.nvpResultado = nvpResultado;
        this.nvpRespuesta = nvpRespuesta;
        this.nvpDetalleResp = "";
    }

    public Respuesta() {
        this.nvpDetalleResp = "";
    }

    public String getNvpResultado() {
        return this.nvpResultado;
    }

    public void setNvpResultado(String nvpResultado) {
        this.nvpResultado = nvpResultado;
    }

    public String getNvpRespuesta() {
        return this.nvpRespuesta;
    }

    public void setNvpRespuesta(String nvpRespuesta) {
        this.nvpRespuesta = nvpRespuesta;
    }

    public String getNvpDetalleResp() {
        return this.nvpDetalleResp;
    }

    public void setNvpDetalleResp(String nvpDetalleResp) {
        this.nvpDetalleResp = nvpDetalleResp;
    }
}

