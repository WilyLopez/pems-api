package com.playzone.pems.application.marketing.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailBlockDto {

    private String id;
    private String tipo;
    private String texto;
    private String url;
    private String alt;
    private Integer nivel;
}
