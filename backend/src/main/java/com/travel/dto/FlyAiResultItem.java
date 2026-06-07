package com.travel.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FlyAiResultItem {
    private String title;
    private String subtitle;
    private String price;
    private String time;
    private List<String> meta = new ArrayList<>();
    private String link;
}
