package com.smov.gabriel.orientatree.model;

public enum TemplateColor {
    NARANJA, ROJA ;

    public static TemplateColor fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
