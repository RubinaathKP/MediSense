package com.medisense.translation.indictrans2.model;

public record IndicTrans2Response(
        String translated_text,
        String src_lang,
        String tgt_lang
) {}
