package edu.cit.antolijao.supplier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

class LegacySupplyXml {

    @XmlRootElement(name = "AuthRequest")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class AuthRequest {
        @XmlElement(name = "ClientId")
        String clientId;
        @XmlElement(name = "ApiKey")
        String apiKey;

        AuthRequest() {}
        AuthRequest(String clientId, String apiKey) {
            this.clientId = clientId;
            this.apiKey = apiKey;
        }
    }

    @XmlRootElement(name = "AuthResponse")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class AuthResponse {
        @XmlElement(name = "SessionToken")
        String sessionToken;
        @XmlElement(name = "IssuedAt")
        String issuedAt;
    }

    @XmlRootElement(name = "PurchaseOrder")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class PurchaseOrderRequest {
        @XmlElement(name = "SupplierSku")
        String supplierSku;
        @XmlElement(name = "Qty")
        int qty;
        @XmlElement(name = "BuyerRef")
        String buyerRef;

        PurchaseOrderRequest() {}
        PurchaseOrderRequest(String supplierSku, int qty, String buyerRef) {
            this.supplierSku = supplierSku;
            this.qty = qty;
            this.buyerRef = buyerRef;
        }
    }

    @XmlRootElement(name = "PurchaseOrderAck")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class PurchaseOrderAck {
        @XmlElement(name = "PoNumber")
        String poNumber;
        @XmlElement(name = "StatusCode")
        int statusCode;
        @XmlElement(name = "SupplierSku")
        String supplierSku;
        @XmlElement(name = "Qty")
        int qty;
        @XmlElement(name = "Uom")
        String uom;
        @XmlElement(name = "BuyerRef")
        String buyerRef;
        @XmlElement(name = "CreatedAt")
        String createdAt;
    }

    @XmlRootElement(name = "PurchaseOrderStatus")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class PurchaseOrderStatusXml {
        @XmlElement(name = "PoNumber")
        String poNumber;
        @XmlElement(name = "StatusCode")
        int statusCode;
        @XmlElement(name = "SupplierSku")
        String supplierSku;
        @XmlElement(name = "Qty")
        int qty;
        @XmlElement(name = "Uom")
        String uom;
        @XmlElement(name = "BuyerRef")
        String buyerRef;
        @XmlElement(name = "CreatedAt")
        String createdAt;
        @XmlElement(name = "CheckedAt")
        String checkedAt;
    }

    @XmlRootElement(name = "LSError")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class LSError {
        @XmlElement(name = "Code")
        String code;
        @XmlElement(name = "Message")
        String message;
    }

    @XmlRootElement(name = "Catalog")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class Catalog {
        @XmlElement(name = "Item")
        java.util.List<Item> items;

        @XmlAccessorType(XmlAccessType.FIELD)
        static class Item {
            @XmlElement(name = "SupplierSku")
            String supplierSku;
            @XmlElement(name = "Description")
            String description;
            @XmlElement(name = "PackSize")
            int packSize;
            @XmlElement(name = "UnitCost")
            String unitCost;
        }
    }
}