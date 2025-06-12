package com.hmdrinks.SupportFunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Exception.ConflictException;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Service.JwtService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.json.JSONArray;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Component
public class SupportFunction {
    private final JwtService jwtService;
//    VudYm4ZnWzUU2Rv5HmxxV2IwrK834KcKmuUQMkGG
    private final String apiKey = "VPnbNbrNMwDOWN9EzMGZ9DicRd8WU4lTyejKYzHk";
    @Autowired
    private ShipmentDirectionRepository shipmentDirectionRepo; // Không dùng static

    @Autowired
    private ShipperDetailRepository shipperDetailRepo;
    @Autowired
    private StepDetailsRepository stepDetailsRepo;
    @Autowired
    private ShipmentGroupRepository shipmentGroupRepository;
    @Autowired
    private ShipmentRepository shipmentRepo;// Không dùng static

    public static ShipmentDirectionRepository shipmentDirectionRepository;
    public static StepDetailsRepository stepDetailsRepository;
    public static  ShipperDetailRepository shipperDetailRepository;
    public  static  ShipmentRepository shipmentRepository;

    @PostConstruct
    public void init() {
        shipmentDirectionRepository = shipmentDirectionRepo;
        stepDetailsRepository = stepDetailsRepo;
        shipmentRepository = shipmentRepo;
        shipperDetailRepository  = shipperDetailRepo;
    }
    @Autowired
    public SupportFunction(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public boolean checkRole(String role) {
        return role.equals("ADMIN") || role.equals("CUSTOMER") || role.equals("SHIPPER");
    }


    public ResponseEntity<?> validatePositiveId(String name, Integer id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Tham số '" + name + "' phải là số nguyên dương");
        }
        return null;
    }


    public ResponseEntity<?> validatePositiveIntegers(Map<String, Integer> params) {
        for (Map.Entry<String, Integer> entry : params.entrySet()) {
            String fieldName = entry.getKey();
            Integer value = entry.getValue();

            if (value == null) {
                return ResponseEntity.badRequest().body("Tham số '" + fieldName + "' không được null");
            }

            if (value <= 0) {
                return ResponseEntity.badRequest().body("Tham số '" + fieldName + "' phải > 0");
            }
        }
        return null; // hợp lệ
    }

    public ResponseEntity<?> checkUserAuthorizationUpgrade(HttpServletRequest httpRequest, int userIdFromRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing or invalid");
        }

        String jwt = authHeader.substring(7);
        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        String role = jwtService.extractUserRole(jwt);
        System.out.println(role);

        int userIdFromToken;
        try {
            userIdFromToken = Integer.parseInt(userIdFromTokenStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format in token");
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.ok("Authorized as admin");
        }

        // Nếu không phải admin, kiểm tra user ID có trùng không
        if (userIdFromRequest != userIdFromToken) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to perform this action");
        }



        return ResponseEntity.ok("Authorized as user");
    }


    public ResponseEntity<?> checkUserAuthorization(HttpServletRequest httpRequest, int userIdFromRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing or invalid");
        }
        String jwt = authHeader.substring(7);

        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        int userIdFromToken;
        try {
            userIdFromToken = Integer.parseInt(userIdFromTokenStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format in token");
        }

        if (userIdFromRequest != userIdFromToken) {
//         String userIdFromToken = jwtService.extractUserId(jwt);
//         System.out.println("UserId from request: " + userIdFromRequest);
//         System.out.println("UserId from token: " + userIdFromToken);
//         if (!String.valueOf(userIdFromRequest).equals(userIdFromToken)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to perform this action");
        }
        else
        {
            return ResponseEntity.status(HttpStatus.OK).body("You have successfully logged in");
        }
    }

    // Hàm kiểm tra
    public ResponseEntity<?> validatePaginationParams(String pageStr, String limitStr) {
        try {
            int page = Integer.parseInt(pageStr);
            if (page <= 0) {
                return ResponseEntity.badRequest().body("Tham số 'page' phải > 0");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Tham số 'page' phải là số");
        }

        try {
            int limit = Integer.parseInt(limitStr);
            if (limit <= 0) {
                return ResponseEntity.badRequest().body("Tham số 'limit' phải > 0");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Tham số 'limit' phải là số");
        }

        return null; // hợp lệ
    }


    public ResponseEntity<?> checkPhoneNumber(String phoneNumber, Integer userId, UserRepository userRepository) {

//     public boolean checkUserAuthorizationRe(HttpServletRequest httpRequest, Long userIdFromRequest) {
//         String authHeader = httpRequest.getHeader("Authorization");
//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is missing or invalid");
//         }

//         String jwt = authHeader.substring(7);
//         String userIdFromToken = jwtService.extractUserId(jwt);

//         if (!String.valueOf(userIdFromRequest).equals(userIdFromToken)) {
//             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
//         }

//         return true;
//     }


//     public void checkPhoneNumber(String phoneNumber, Integer userId, UserRepository userRepository) {
//         // Kiểm tra độ dài của số điện thoại
        if (phoneNumber == null || phoneNumber.length() != 10) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Số điện thoại không hợp lệ. Phải chứa 10 chữ số.");
        }
        Optional<User> existingUserOptional = userRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber);
        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
            if (!(existingUser.getUserId() ==userId)) {
                throw new ConflictException("Số điện thoại đã tồn tại.");
            }
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public String getLocation(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, "UTF-8");

            String location = "21.013715429594125,105.79829597455202";
            String urlString = "https://rsapi.goong.io/Place/AutoComplete?api_key=" + apiKey
                    + "&location=" + location + "&input=" + encodedAddress;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();
            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray predictions = jsonResponse.getAsJsonArray("predictions");
            if (predictions != null && predictions.size() > 0) {
                for (int i = 0; i < predictions.size(); i++) {
                    JsonObject prediction = predictions.get(i).getAsJsonObject();
                    if (prediction.has("place_id")) {
                        String placeId = prediction.get("place_id").getAsString();
                        return placeId;
                    }
                }
            } else {
                System.out.println("Không tìm thấy trường 'predictions' hoặc không có dữ liệu.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static double[] getCoordinates(String placeId) {
        try {
            String apiKey = "VPnbNbrNMwDOWN9EzMGZ9DicRd8WU4lTyejKYzHk";
            String urlString = "https://rsapi.goong.io/geocode?place_id=" + placeId + "&api_key=" + apiKey;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();
            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (jsonResponse.has("results") && jsonResponse.getAsJsonArray("results").size() > 0) {
                JsonObject location = jsonResponse.getAsJsonArray("results")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("geometry")
                        .getAsJsonObject("location");

                double lat = location.get("lat").getAsDouble();
                double lng = location.get("lng").getAsDouble();

                return new double[]{lat, lng};
            } else {
                System.out.println("Không có kết quả nào được trả về.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void resetShipperDaily() {
        LocalDate today = LocalDate.now();
        List<ShipperDetail> shipperDetails = shipperDetailRepository.findAll();

        for (ShipperDetail s : shipperDetails) {
            if (s.getDateReset() == null || !s.getDateReset().isEqual(today)) {
                s.setStatus("available");
                s.setOnLeave(false);
                s.setLocation(null);
                s.setTotalOrdersToday(0);
                s.setIsReset(true);
                s.setDateReset(today);
                shipperDetailRepository.save(s);
            }
        }

        System.out.println("✅ Đã reset shipper cho ngày " + today);
    }






//    public static double getShortestDistance(double[] origins, double[] destinations) {
//        double shortestDistanceValue = Double.MAX_VALUE;
//        try {
//            String apiKey = "VudYm4ZnWzUU2Rv5HmxxV2IwrK834KcKmuUQMkGG";
//            String originsParam = origins[0] + "," + origins[1];
//            String destinationsParam = destinations[0] + "," + destinations[1];
//
//            String urlString = "https://rsapi.goong.io/DistanceMatrix?origins=" + originsParam +
//                    "&destinations=" + destinationsParam +
//                    "&vehicle=car&api_key=" + apiKey;
//            URL url = new URL(urlString);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setRequestProperty("Accept", "application/json");
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
//            StringBuilder response = new StringBuilder();
//            String output;
//            while ((output = br.readLine()) != null) {
//                response.append(output);
//            }
//            conn.disconnect();
//
//            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
//            JsonArray rows = jsonResponse.getAsJsonArray("rows");
//
//            if (rows.size() > 0) {
//                JsonArray elements = rows.get(0).getAsJsonObject().getAsJsonArray("elements");
//
//                for (JsonElement element : elements) {
//                    JsonObject elementObj = element.getAsJsonObject();
//                    if (elementObj.get("status").getAsString().equals("OK")) {
//                        String distanceText = elementObj.getAsJsonObject("distance").get("text").getAsString();
//                        String minute=  elementObj.getAsJsonObject("duration").get("text").getAsString();
//                        double distanceValue;
//
//                        if (distanceText.contains("km")) {
//                            distanceValue = Double.parseDouble(distanceText.replace(" km", "").trim());
//                        } else if (distanceText.contains("m")) {
//                            distanceValue = Double.parseDouble(distanceText.replace(" m", "").trim()) / 1000; // Chuyển đổi từ m sang km
//                        } else {
//                            continue;
//                        }
//                        if (distanceValue < shortestDistanceValue) {
//                            shortestDistanceValue = distanceValue;
//                        }
//                    }
//                }
//            } else {
//                System.out.println("Không có kết quả nào được trả về.");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return shortestDistanceValue != Double.MAX_VALUE ? shortestDistanceValue : -1;
//    }

    public DistanceAndDuration getShortestDistance(double[] origins, double[] destinations) {
        double shortestDistanceValue = Double.MAX_VALUE;
        String shortestDuration = null;
        try {
            String apiKey = "VPnbNbrNMwDOWN9EzMGZ9DicRd8WU4lTyejKYzHk";
            String originsParam = origins[0] + "," + origins[1];
            String destinationsParam = destinations[0] + "," + destinations[1];

            String urlString = "https://rsapi.goong.io/DistanceMatrix?origins=" + originsParam +
                    "&destinations=" + destinationsParam +
                    "&vehicle=bike&api_key=" + apiKey;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray rows = jsonResponse.getAsJsonArray("rows");

            if (rows.size() > 0) {
                JsonArray elements = rows.get(0).getAsJsonObject().getAsJsonArray("elements");

                for (JsonElement element : elements) {
                    JsonObject elementObj = element.getAsJsonObject();
                    if (elementObj.get("status").getAsString().equals("OK")) {
                        String distanceText = elementObj.getAsJsonObject("distance").get("text").getAsString();
                        String durationText = elementObj.getAsJsonObject("duration").get("text").getAsString();
                        double distanceValue;

                        if (distanceText.contains("km")) {
                            distanceValue = Double.parseDouble(distanceText.replace(" km", "").trim());
                        } else if (distanceText.contains("m")) {
                            distanceValue = Double.parseDouble(distanceText.replace(" m", "").trim()) / 1000; // Chuyển đổi từ m sang km
                        } else {
                            continue;
                        }
                        if (distanceValue < shortestDistanceValue) {
                            shortestDistanceValue = distanceValue;
                            shortestDuration = durationText;
                        }
                    }
                }
            } else {
                System.out.println("Không có kết quả nào được trả về.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (shortestDistanceValue != Double.MAX_VALUE && shortestDuration != null) {
            return new DistanceAndDuration(shortestDistanceValue, shortestDuration);
        } else {
            return null; // Nếu không có kết quả hợp lệ
        }
    }

    public String convertLanguage(String query, Language language) {
        String langTarget = "";
        if(language == Language.VN)
        {
            langTarget = "vi";
        }
        else if(language == Language.EN)
        {
            langTarget = "en";
        }
        String result = "";
        try {

            String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                    + langTarget + "&dt=t&q=" + URLEncoder.encode(query, "UTF-8");

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JSONArray jsonArray = new JSONArray(response.toString());
            StringBuilder translatedText = new StringBuilder();

            JSONArray translations = jsonArray.getJSONArray(0);
            for (int i = 0; i < translations.length(); i++) {
                translatedText.append(translations.getJSONArray(i).getString(0)).append(" ");
            }

            result = translatedText.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return  result;
    }





    private static final String API_KEY = "2gHtWCeuhGl06QEaGVT05QHWSb49IHNdOd1i46mT";
    private static final String GOONG_API_URL = "https://rsapi.goong.io/Direction";
    public void getDirections(double[] origin, double[] destination,Integer shipmentId) {
        RestTemplate restTemplate = new RestTemplate();
        String originStr = String.format("%f,%f", origin[0], origin[1]);
        String destinationStr = String.format("%f,%f", destination[0], destination[1]);
        String url = String.format("%s?origin=%s&destination=%s&vehicle=bike&api_key=%s",
                GOONG_API_URL, originStr, destinationStr, API_KEY);

        String response = restTemplate.getForObject(url, String.class);
        if (response != null) {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray routes = jsonResponse.getJSONArray("routes");
            if (!routes.isEmpty()) {
                JSONObject route = routes.getJSONObject(0);
                String overviewPolyline = route.getJSONObject("overview_polyline").getString("points");
                JSONArray steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");

                ShipmentDirection shipmentDirection = new ShipmentDirection();
                shipmentDirection.setCreatedAt(LocalDateTime.now());
                shipmentDirection.setIsDeleted(false);
                shipmentDirection.setOverviewPolyline(overviewPolyline);
                Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
                shipmentDirection.setShipment(shippment);
                shipmentDirection.setLatitudeEnd(destination[0]);
                shipmentDirection.setLatitudeStart(origin[0]);
                shipmentDirection.setLongitudeStart(origin[1]);
                shipmentDirection.setLongitudeEnd(destination[1]);
                shipmentDirectionRepository.save(shipmentDirection);

                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    String instruction = step.getString("html_instructions");
                    JSONObject startLocation = step.getJSONObject("start_location");
                    double lat = startLocation.getDouble("lat");
                    double lng = startLocation.getDouble("lng");
                    String distanceText = step.getJSONObject("distance").getString("text");
                    String durationText = step.getJSONObject("duration").getString("text");
                    StepDetail stepDetail = new StepDetail();
                    stepDetail.setDirection(shipmentDirection);
                    stepDetail.setInstruction(instruction);
                    stepDetail.setDistanceText(distanceText);
                    stepDetail.setDurationText(durationText);
                    stepDetail.setLatitude(lat);
                    stepDetail.setLongitude(lng);
                    stepDetailsRepository.save(stepDetail);

                }
            }
        }
    }

    public void getDirectionsGroup(double[] origin, double[] destination,Integer shipmentGroupId) {
        RestTemplate restTemplate = new RestTemplate();
        String originStr = String.format("%f,%f", origin[0], origin[1]);
        String destinationStr = String.format("%f,%f", destination[0], destination[1]);
        String url = String.format("%s?origin=%s&destination=%s&vehicle=bike&api_key=%s",
                GOONG_API_URL, originStr, destinationStr, API_KEY);

        String response = restTemplate.getForObject(url, String.class);
        if (response != null) {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray routes = jsonResponse.getJSONArray("routes");
            if (!routes.isEmpty()) {
                JSONObject route = routes.getJSONObject(0);
                String overviewPolyline = route.getJSONObject("overview_polyline").getString("points");
                JSONArray steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");

                ShipmentDirection shipmentDirection = new ShipmentDirection();
                shipmentDirection.setCreatedAt(LocalDateTime.now());
                shipmentDirection.setIsDeleted(false);
                shipmentDirection.setOverviewPolyline(overviewPolyline);
                ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentGroupId);
                shipmentDirection.setShipment(null);
                shipmentDirection.setShipmentGroup(shippment);
                shipmentDirection.setLatitudeEnd(destination[0]);
                shipmentDirection.setLatitudeStart(origin[0]);
                shipmentDirection.setLongitudeStart(origin[1]);
                shipmentDirection.setLongitudeEnd(destination[1]);
                shipmentDirectionRepository.save(shipmentDirection);

                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    String instruction = step.getString("html_instructions");
                    JSONObject startLocation = step.getJSONObject("start_location");
                    double lat = startLocation.getDouble("lat");
                    double lng = startLocation.getDouble("lng");
                    String distanceText = step.getJSONObject("distance").getString("text");
                    String durationText = step.getJSONObject("duration").getString("text");
                    StepDetail stepDetail = new StepDetail();
                    stepDetail.setDirection(shipmentDirection);
                    stepDetail.setInstruction(instruction);
                    stepDetail.setDistanceText(distanceText);
                    stepDetail.setDurationText(durationText);
                    stepDetail.setLatitude(lat);
                    stepDetail.setLongitude(lng);
                    stepDetailsRepository.save(stepDetail);

                }
            }
        }
    }

//    public static void main(String[] args) {
//
//
//        double[] origin = {10.859561564000046,106.76977695400006};
//        double[] destination = {10.8480014,106.7743068};
//
//        getDirections(origin, destination);
//    }

//    public ResponseEntity<?> validate(Object request) {
//        for (Field field : request.getClass().getDeclaredFields()) {
//            field.setAccessible(true);
//            try {
//                Object value = field.get(request);
//
//                // Kiểm tra trường kiểu nguyên thủy (int, boolean, v.v.) nếu không có giá trị hợp lệ
//                if (field.getType().isPrimitive()) {
//                    if (value == null || (value instanceof Integer && (Integer) value == 0) ||
//                            (value instanceof Boolean && (Boolean) value == false) ||
//                            (value instanceof Double && (Double) value == 0.0) ||
//                            (value instanceof Float && (Float) value == 0.0f) ||
//                            (value instanceof Long && (Long) value == 0L) ||
//                            (value instanceof Short && (Short) value == (short) 0) ||
//                            (value instanceof Byte && (Byte) value == (byte) 0)) {
//                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                                .body(field.getName() + " is required. Its type is " + field.getType().getSimpleName());
//                    }
//                }
//
//                // Kiểm tra trường kiểu đối tượng có giá trị null hoặc chuỗi rỗng
//                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                            .body(field.getName() + " is required. Its type is " + field.getType().getSimpleName());
//                }
//
//                // Kiểm tra trường kiểu java.sql.Date có giá trị null (nếu cần thiết bỏ qua kiểm tra)
//                if (field.getType() == java.sql.Date.class && Objects.equals(value, null)) {
//                    continue;
//                }
//
//            } catch (IllegalAccessException e) {
//                // Trả về ResponseEntity nếu có lỗi trong khi truy cập trường
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                        .body("Failed to validate request: " + e.getMessage());
//            }
//        }
//
//        // Nếu tất cả các trường hợp hợp lệ, trả về thông báo thành công
//        return ResponseEntity.ok().body("Validation successful");
//    }

}

