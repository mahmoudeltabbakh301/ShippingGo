package com.shipment.shippinggo.enums;

import lombok.Getter;

@Getter
public enum IntegrationPlatform {
    SHOPIFY("Shopify"),
    WUILT("Wuilt"),
    ZAMMIT("Zammit"),
    SALLA("Salla"),
    CUSTOM("مخصص"),
    EXTERNAL_SYSTEM("نظام خارجي");

    private final String displayName;

    IntegrationPlatform(String displayName) {
        this.displayName = displayName;
    }
}
