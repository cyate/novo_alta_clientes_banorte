/*
 * Decompiled with CFR 0.151.
 */
package com.novo.bean;

public class Usuario {
    private String nvpUsuarioBemBep;
    private String nvpCurp;
    private String nvpNombreUsuario;
    private String nvpCorreoUsuario;
    private boolean exist;
    private boolean reactivacionUsuario;

    public Usuario() {
    }

    public Usuario(String nvpUsuarioBemBep, String nvpCurp, String nvpNombreUsuario, String nvpCorreoUsuario) {
        this.nvpUsuarioBemBep = nvpUsuarioBemBep;
        this.nvpCurp = nvpCurp;
        this.nvpNombreUsuario = nvpNombreUsuario;
        this.nvpCorreoUsuario = nvpCorreoUsuario;
    }

    public String getNvpUsuarioBemBep() {
        return this.nvpUsuarioBemBep;
    }

    public void setNvpUsuarioBemBep(String nvpUsuarioBemBep) {
        this.nvpUsuarioBemBep = nvpUsuarioBemBep;
    }

    public String getNvpCurp() {
        return this.nvpCurp;
    }

    public void setNvpCurp(String nvpCurp) {
        this.nvpCurp = nvpCurp;
    }

    public String getNvpNombreUsuario() {
        return this.nvpNombreUsuario;
    }

    public void setNvpNombreUsuario(String nvpNombreUsuario) {
        this.nvpNombreUsuario = nvpNombreUsuario;
    }

    public String getNvpCorreoUsuario() {
        return this.nvpCorreoUsuario;
    }

    public void setNvpCorreoUsuario(String nvpCorreoUsuario) {
        this.nvpCorreoUsuario = nvpCorreoUsuario;
    }

    public boolean isExist() {
        return this.exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public boolean isReactivacionUsuario() {
        return this.reactivacionUsuario;
    }

    public void setReactivacionUsuario(boolean reactivacionUsuario) {
        this.reactivacionUsuario = reactivacionUsuario;
    }
}

