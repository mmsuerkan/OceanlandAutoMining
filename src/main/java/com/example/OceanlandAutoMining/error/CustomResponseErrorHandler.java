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

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            // You can do logging here
            System.out.println("Received 500 error. Skipping this NFT.");
        } else {
            throw new HttpClientErrorException(response.getStatusCode(), response.getStatusText());
        }
    }
}
