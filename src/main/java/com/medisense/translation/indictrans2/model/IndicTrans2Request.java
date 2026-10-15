package com.medisense.translation.indictrans2.model;

public record IndicTrans2Request(
        String text,
        String src_lang,
        String tgt_lang
) {}
