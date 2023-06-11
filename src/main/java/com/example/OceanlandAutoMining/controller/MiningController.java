package com.example.OceanlandAutoMining.controller;

import com.example.OceanlandAutoMining.entity.User;
import com.example.OceanlandAutoMining.error.CustomResponseErrorHandler;
import com.example.OceanlandAutoMining.entity.Equipment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MiningController {
    private final RestTemplate restTemplate;
    private List<User> users = Arrays.asList(
            new User("msuerkan301@gmail.com", "Mu102019*"),
            new User("msuerkan302@gmail.com", "Mu102019*")
    );

    public MiningController() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new CustomResponseErrorHandler());
    }

    public String fetchToken(User user) {
        String apiUrl = "https://api.oceanland.io/api/auth/signin/email";
        String requestBody = "{ \"chainid\": \"" + "0x38" + "\", \"email\": \"" + user.getEmail() + "\", \"password\": \"" + user.getPassword() + "\" }";

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

    public List<Equipment> fetchEquipment(User user) {
        String accessToken = fetchToken(user);
        String apiUrl = "https://api.oceanland.io/api/equip";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Equipment[]> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Equipment[].class);
        Equipment[] equipmentArray = response.getBody();
        return Arrays.asList(equipmentArray);
    }

    @Scheduled(fixedRate = 1000 * 60)
    public void startAllEquippedNFTs() {
        for (User user : users) {
            List<Equipment> equippedNFTs = fetchEquipment(user);

            List<Equipment> tools = equippedNFTs.stream()
                    .filter(nft -> "TOOL".equals(nft.getNftType()))
                    .filter(nft -> nft.getNextAvailableTime() < System.currentTimeMillis())
                    .collect(Collectors.toList());

            List<Equipment> startableTools = tools.stream()
                    .collect(Collectors.toList());

            for (Equipment tool : startableTools) {
                startNFT(tool.getId(), user);
            }
        }
    }

    public void startNFT(long id, User user) {
        try {
            String accessToken = fetchToken(user);
            String apiUrl = "https://api.oceanland.io/api/mine/" + id;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Successfully started NFT with id: " + id);
            } else {
                System.out.println("Failed to start NFT with id: " + id);
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                System.out.println("Received 500 error for NFT with id: " + id + ". Skipping this NFT.");
            }
        }
    }
}
