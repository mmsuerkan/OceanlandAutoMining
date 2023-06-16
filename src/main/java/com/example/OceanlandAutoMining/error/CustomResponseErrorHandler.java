package com.example.OceanlandAutoMining.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class CustomResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().is5xxServerError();
    }
    //500 Internal Server Error: "{"timestamp":1686939715470,"status":500,"error":"Internal Server Error","message":"You cannot mine with free NFT on multiple accounts!","path":"/api/mine/101621"}"
    //hatadaki mesajı ve pathi almak için
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            // You can do logging here




        } else {
            throw new HttpClientErrorException(response.getStatusCode(), response.getStatusText());
        }
    }
}
