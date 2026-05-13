package com.shipment.shippinggo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestDownload {
    public static void main(String[] args) throws Exception {
        String[] urls = {
            "https://raw.githubusercontent.com/alif-type/amiri/master/fonts/ttf/Amiri-Regular.ttf",
            "https://raw.githubusercontent.com/alif-type/amiri/main/fonts/ttf/Amiri-Regular.ttf",
            "https://github.com/google/fonts/raw/main/ofl/amiri/Amiri-Regular.ttf",
            "https://cdn.jsdelivr.net/gh/alif-type/amiri@master/fonts/ttf/Amiri-Regular.ttf"
        };
        
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        
        for (String url : urls) {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
            System.out.println("URL: " + url + " -> Status: " + res.statusCode());
        }
    }
}
