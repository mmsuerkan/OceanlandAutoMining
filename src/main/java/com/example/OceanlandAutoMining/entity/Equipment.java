package com.example.OceanlandAutoMining.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Equipment {
    private int id;
    private int user;
    private int nftId;
    private boolean isEquipped;
    private long nextAvailableTime;
    private int numberOfUses;
    private String blockchainId;
    private long equippedAt;
    private Long unequippedAt;
    private Integer boosterNftId;
    private Integer boosterUseCount;
    private String status;
    private int remainingSeconds;
    private long serverTime;
    private String nftType;
}
