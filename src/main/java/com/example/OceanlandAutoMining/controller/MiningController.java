package com.example.OceanlandAutoMining.controller;

import com.example.OceanlandAutoMining.entity.User;
import com.example.OceanlandAutoMining.entity.Equipment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MiningController {
    private final Logger logger = LoggerFactory.getLogger(MiningController.class);

    private final RestTemplate restTemplate;
    private final Cache<String, List<Equipment>> equipmentCache;
    private List<User> users = Arrays.asList(
            new User("msuerkan301@gmail.com", "Mu102019*"),
            new User("msuerkan302@gmail.com", "Mu102019*"),
            new User("msuerkan303@gmail.com", "Mu102019*")
    );

    public MiningController() {
        this.restTemplate = new RestTemplate();
        this.equipmentCache = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build();
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
            logger.error("Error while parsing token response for user: {}", user.getEmail(), e);
        }

        return accessToken;
    }

    @Scheduled(fixedRate = 1000 * 60 * 30)
    public void startAllEquippedNFTs() throws InterruptedException {
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
                Thread.sleep(2000);
            }
        }
    }

    public List<Equipment> fetchEquipment(User user) {
        if (equipmentCache.asMap().containsKey(user.getEmail())) {
            logger.info("Fetching equipment data from cache for user: {}", user.getEmail());
            return equipmentCache.get(user.getEmail(), key -> fetchEquipmentFromAPI(user));
        }

        return fetchEquipmentFromAPI(user);
    }

    private List<Equipment> fetchEquipmentFromAPI(User user) {
        String accessToken = fetchToken(user);
        String apiUrl = "https://api.oceanland.io/api/equip";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Equipment[]> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Equipment[].class);
        Equipment[] equipmentArray = response.getBody();
        List<Equipment> equipmentList = Arrays.asList(equipmentArray);

        equipmentCache.put(user.getEmail(), equipmentList);
        logger.info("Fetched equipment data from API for user: {}", user.getEmail());

        return equipmentList;
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
                logger.info("Successfully started NFT with id: {} for user: {}", id, user.getEmail());
            } else {
                logger.error("Failed to start NFT with id: {} for user: {}", id, user.getEmail());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                logger.error("Received 500 error for NFT with id: {} for user: {}. Skipping this NFT.", id, user.getEmail());
            }
        }
    }
}
