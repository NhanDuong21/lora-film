package com.lorafilm.movie.autoschedule.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AutoScheduleRequestFingerprintServiceImpl implements AutoScheduleRequestFingerprintService {

    private final ObjectMapper objectMapper;

    public AutoScheduleRequestFingerprintServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateFingerprint(NormalizedGeneratePreviewRequest request) {
        try {
            // Build canonical JSON node manually to ensure consistent property order
            ObjectNode root = objectMapper.createObjectNode();
            
            root.put("applyMode", "ALL_OR_NOTHING");
            
            ArrayNode auditoriumsNode = root.putArray("auditoriumPublicIds");
            request.getAuditoriumPublicIds().forEach(auditoriumsNode::add);
            
            root.put("cinemaPublicId", request.getCinemaPublicId());
            
            ArrayNode movieVersionsNode = root.putArray("movieVersionPublicIds");
            request.getMovieVersionPublicIds().forEach(movieVersionsNode::add);
            
            root.put("previewTtlMinutes", request.getPreviewTtlMinutes());
            root.put("scheduleFrom", request.getScheduleFrom().toString());
            root.put("scheduleTo", request.getScheduleTo().toString());
            root.put("slotGranularityMinutes", request.getSlotGranularityMinutes());
            root.put("strategy", "BALANCED");
            root.put("strategyVersion", "BALANCED_V1");

            // Convert to string and hash
            String canonicalJson = objectMapper.writeValueAsString(root);
            return hashSha256(canonicalJson);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate request fingerprint", e);
        }
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
