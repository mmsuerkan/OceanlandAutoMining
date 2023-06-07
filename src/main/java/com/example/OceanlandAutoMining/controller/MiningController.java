package com.example.OceanlandAutoMining.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MiningController {
    private final RestTemplate restTemplate;

    @GetMapping("/fetch-token")
    public String fetchToken() {


        String apiUrl = "https://api.oceanland.io/api/auth/signin/email";
        String requestBody = "{ \"chainid\": \"" + "0x38" + "\", \"email\": \"" + "msuerkan301@gmail.com" + "\", \"password\": \"" + "Mu102019*" + "\" }";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

        String accessToken = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());
            accessToken = jsonNode.get("accessToken").asText();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return accessToken;
    }

    @GetMapping("/fetch-equipped-nfts")
    public List<Equipment> fetchEquipment() {
        String accessToken = fetchToken();
        String apiUrl = "https://api.oceanland.io/api/equip";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Equipment[]> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Equipment[].class);
        Equipment[] equipmentArray = response.getBody();
        return Arrays.asList(equipmentArray);
    }
    @Scheduled(fixedRate = 1800000)
    public void startAllEquippedNFTs() {
        // Fetch equipped NFTs
        List<Equipment> equippedNFTs = fetchEquipment();

        // Filter NFTs with type TOOL
        List<Equipment> tools = equippedNFTs.stream()
                .filter(nft -> "TOOL".equals(nft.getNftType()))
                .collect(Collectors.toList());

        // Get the current server time
        long serverTime = System.currentTimeMillis(); // Assume server time is same as system time

        // Filter tools that can be started now
        List<Equipment> startableTools = tools.stream()
                .filter(tool -> tool.getNextAvailableTime() <= serverTime && tool.getRemainingSeconds() == 0)
                .collect(Collectors.toList());

        // Start all startable TOOL NFTs
        for (Equipment tool : startableTools) {
            startNFT(tool.getId());
        }
    }

    public void startNFT(long id) {
        try {
            String accessToken = fetchToken();
            String apiUrl = "https://api.oceanland.io/api/mine/" + id;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            // Optional: check the response
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Successfully started NFT with id: " + id);
            } else {
                System.out.println("Failed to start NFT with id: " + id);
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                // Handle 500 error
                System.out.println("Received 500 error for NFT with id: " + id + ". Skipping this NFT.");
            }
        }
    }
}
