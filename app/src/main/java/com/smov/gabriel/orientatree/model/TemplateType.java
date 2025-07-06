package com.smov.gabriel.orientatree.model;

public enum TemplateType {
    EDUCATIVA, DEPORTIVA;
    public static TemplateType fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
