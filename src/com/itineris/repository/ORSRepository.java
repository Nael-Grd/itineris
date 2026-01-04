package com.itineris.repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Properties;

public class ORSRepository {

    HttpClient client = HttpClient.newHttpClient();
    
    private String getCoordinates(String city) { 
        String cityUrl = city.replace(" ", "%20");    // remplace les espaces par %20 pour les villes composées (ex: Mont Saint Michel)
        String url = "https://nominatim.openstreetmap.org/search?q="+cityUrl+"&format=json&limit=1";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-agent", "itineris").GET().build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            String json = response.body();

            int latStart = json.indexOf("\"lat\":\"") + 7;
            int latEnd = json.indexOf("\"", latStart); 
            String lat = json.substring(latStart, latEnd);

            int lonStart = json.indexOf("\"lon\":\"") + 7;
            int lonEnd = json.indexOf("\"", lonStart);
            String lon = json.substring(lonStart, lonEnd);

            return lon + "," + lat;
        }
        catch (Exception e) {
            return null;
        }
    }

    private String getApiKey() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            return prop.getProperty("ors.api.key");
        } catch (IOException ex) {
            System.err.println("Attention : Fichier config.properties introuvable à la racine.");
            return null;
        }
    }


    public RouteData getRouteData(String departure, String arrival, String transport) {
        String apiKey = getApiKey();
        if (apiKey == null) return null;

        String depCoord = getCoordinates(departure);
        String arrCoord = getCoordinates(arrival);
        if (depCoord == null || arrCoord == null) return null;

        String url = "https://api.openrouteservice.org/v2/directions/" + transport 
                   + "?api_key=" + apiKey + "&start=" + depCoord + "&end=" + arrCoord;

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            String json = client.send(request, BodyHandlers.ofString()).body();

            double duration = Double.parseDouble(parseKey(json, "duration"));    
            double distance = Double.parseDouble(parseKey(json, "distance"));

            return new RouteData(duration, distance);
        } catch (Exception e) {
            return null;
        }
    }

    // Méthode utilitaire interne pour le parsing
    private String parseKey(String json, String key) {
        int start = json.indexOf("\"" + key + "\":") + key.length() + 2;
        while (!Character.isDigit(json.charAt(start))) { start++; }
        int endComma = json.indexOf(",", start);
        int endBracket = json.indexOf("}", start);
        int end = (endComma != -1 && endComma < endBracket) ? endComma : endBracket;
        return json.substring(start, end);
    }

}