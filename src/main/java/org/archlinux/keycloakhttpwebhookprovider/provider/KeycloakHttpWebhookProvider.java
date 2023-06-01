package org.archlinux.keycloakhttpwebhookprovider.provider;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class KeycloakHttpWebhookProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(KeycloakHttpWebhookProvider.class);
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();
    private String serverUrl;
    private String username;
    private String password;
    private String apiKey;

    public KeycloakHttpWebhookProvider(String serverUrl, String username, String password, String apiKey) {
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        this.apiKey = apiKey;
    }

    private void sendJson(String jsonString) {
        Request.Builder request_builder = new Request.Builder().url(this.serverUrl).addHeader("User-Agent", "Keycloak Webhook");

        if (this.username != null && this.password != null) {
            String credential = Credentials.basic(this.username, this.password);
            request_builder.addHeader("Authorization", credential);
        }

        if (this.apiKey != null) {
            request_builder.addHeader("X-API-KEY", this.apiKey);
        }

        Request request = request_builder.post(RequestBody.create(jsonString, JSON)).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error(String.format("Failed to POST webhook: %s %s",  response.code(), response.message()));
            }
        } catch (IOException e) {
            log.error("Failed to POST webhook:", e);
        }
    }

    @Override
    public void onEvent(Event event) {
        log.debug("Event Occurred:" + toString(event));
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        log.debug("Admin Event Occurred:" + toString(adminEvent));
    }

    @Override
    public void close() {}

    private String toString(Event event) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "";
        try {
            jsonString = mapper.writeValueAsString(event);
            sendJson(jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    private String toString(AdminEvent adminEvent) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "";
        try {
            // An AdminEvent has weird JSON representation field which we need to special case.
            JsonNode representationNode = mapper.readTree(adminEvent.getRepresentation());
            ObjectNode node = mapper.valueToTree(adminEvent);
            node.replace("representation", representationNode);
            jsonString = mapper.writeValueAsString(node);

            sendJson(jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }
}
