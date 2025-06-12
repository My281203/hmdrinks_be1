package com.hmdrinks.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Entity.UserChatHistory;
import com.hmdrinks.Entity.UserCoin;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Repository.UserChatRepository;
import com.hmdrinks.Repository.UserCointRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Request.DeleteChatRequest;
import com.hmdrinks.Request.QuestionChatRequest;
import com.hmdrinks.Request.UpdateNameChatRequest;
import com.hmdrinks.Response.CRUDChatHistoryResponse;
import com.hmdrinks.Response.CRUDUserCoinResponse;
import com.hmdrinks.Response.ListAllChatHistoryResponse;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import scala.None;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class UserChatService {

    private final RestTemplate restTemplate = new RestTemplate();


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCointRepository userCointRepository;
    @Autowired
    private UserChatRepository userChatRepository;


    @Value("${api.user-service.url}")
    private String userServiceUrl;

    @Value("${api.group-order.url}")
    private String groupOrderUrl;


    public ResponseEntity<?> createNewChat(Integer userId, String token) {
        try {
            User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }
            String  apiUrl = userServiceUrl + "/chat/new_chat/create/";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();


            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);


            try (OutputStream os = conn.getOutputStream()) {
                os.write("".getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.toString());

                String idMongo = jsonNode.get("idMongo").asText();
                String chatName = jsonNode.get("chat_name").asText();
                UserChatHistory userChatHistory = new UserChatHistory();
                userChatHistory.setChatName(chatName);
                userChatHistory.setIdMongoDb(idMongo);
                userChatHistory.setUser(user);
                userChatHistory.setDateCreated(LocalDateTime.now());
                userChatHistory.setIsDeleted(false);
                userChatRepository.save(userChatHistory);

                return ResponseEntity.status(HttpStatus.OK).body(new CRUDChatHistoryResponse(
                        userChatHistory.getUserChatId(),
                        userChatHistory.getUser().getUserId(),
                        userChatHistory.getIdMongoDb(),
                        userChatHistory.getChatName(),
                        userChatHistory.getIsDeleted(),
                        userChatHistory.getDateDeleted(),
                        userChatHistory.getDateUpdated(),
                        userChatHistory.getDateCreated()
                ));

            } else {
                return ResponseEntity.status(responseCode).body("❌ Lỗi: " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Lỗi máy chủ: " + e.getMessage());
        }
    }




    public ResponseEntity<?> updateNameChat(UpdateNameChatRequest req, String token) {
        try {

            User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }
            UserChatHistory userChatHistory = userChatRepository.findByUserChatId(req.getChatId());
            if (userChatHistory == null) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userChat");
            }
            UserChatHistory userChatHistory1 = userChatRepository.findByChatNameAndUserChatIdNot(req.getChatName(), req.getChatId());
            if(userChatHistory1 != null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Name chat already exist");
            }

            String id_mongo_chat = userChatHistory.getIdMongoDb();

            String  apiUrl = userServiceUrl + "/chat/update";

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();


            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{"
                    + "\"chat_id\": \"" + id_mongo_chat + "\","
                    + "\"name_chat\": \"" + req.getChatName() + "\""
                    + "}";

            // Gửi request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                userChatHistory.setChatName(req.getChatName());
                userChatHistory.setDateUpdated(LocalDateTime.now());
                userChatRepository.save(userChatHistory);
                return ResponseEntity.ok(new CRUDChatHistoryResponse(
                        userChatHistory.getUserChatId(),
                        userChatHistory.getUser().getUserId(),
                        userChatHistory.getIdMongoDb(),
                        userChatHistory.getChatName(),
                        userChatHistory.getIsDeleted(),
                        userChatHistory.getDateDeleted(),
                        userChatHistory.getDateUpdated(),
                        userChatHistory.getDateCreated()
                ));
            } else {
                return ResponseEntity.status(responseCode).body("❌ Lỗi: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed");
        }
    }

    public ResponseEntity<?> deleteChat(DeleteChatRequest req, String token) {
        try {

            User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }
            UserChatHistory userChatHistory = userChatRepository.findByUserChatId(req.getChatId());
            if (userChatHistory == null) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userChat");
            }


            String id_mongo_chat = userChatHistory.getIdMongoDb();

            String  apiUrl = userServiceUrl + "/chat/delete";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();


            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true); // Cho phép gửi body

            // Tạo JSON body trực tiếp
            String jsonInputString = "{ \"chat_id\": \"" + id_mongo_chat + "\" }";

            // Gửi request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                userChatHistory.setIsDeleted(true);
                userChatHistory.setDateDeleted(LocalDateTime.now());
                userChatRepository.save(userChatHistory);
                return ResponseEntity.ok(new CRUDChatHistoryResponse(
                        userChatHistory.getUserChatId(),
                        userChatHistory.getUser().getUserId(),
                        userChatHistory.getIdMongoDb(),
                        userChatHistory.getChatName(),
                        userChatHistory.getIsDeleted(),
                        userChatHistory.getDateDeleted(),
                        userChatHistory.getDateUpdated(),
                        userChatHistory.getDateCreated()
                ));
            } else {
                return ResponseEntity.status(responseCode).body("Lỗi: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed");
        }
    }


    public ResponseEntity<?> stopChat(Integer chatId, String token, Integer userId) {
        try {
            User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }
            UserChatHistory userChatHistory = userChatRepository.findByUserChatId(chatId);
            if (userChatHistory == null) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userChat");
            }


            String id_mongo_chat = userChatHistory.getIdMongoDb();
            String stopChatUrl = userServiceUrl + "/chat/stop-task/" + id_mongo_chat;
            URL url = new URL(stopChatUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();
            BufferedReader in;

            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            String inputLine;
            StringBuilder responseContent = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseContent.append(inputLine);
            }
            in.close();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return ResponseEntity.ok("✅ Đã gửi tín hiệu stop cho chat_id: " + chatId);
            } else {
                return ResponseEntity.status(responseCode).body("❌ Stop thất bại: " + responseContent.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi stop chat");
        }
    }


    public ResponseEntity<?> ListChatHistory(Integer userId) {

            User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }

            List<UserChatHistory> userChatHistoryList = userChatRepository.findByUserUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
            List<CRUDChatHistoryResponse> crudChatHistoryResponseList = new ArrayList<>();
            for(UserChatHistory userChatHistory : userChatHistoryList) {
                crudChatHistoryResponseList.add(new CRUDChatHistoryResponse(
                        userChatHistory.getUserChatId(),
                        userChatHistory.getUser().getUserId(),
                        userChatHistory.getIdMongoDb(),
                        userChatHistory.getChatName(),
                        userChatHistory.getIsDeleted(),
                        userChatHistory.getDateDeleted(),
                        userChatHistory.getDateUpdated(),
                        userChatHistory.getDateCreated()
                ));
            }

            return ResponseEntity.ok(new ListAllChatHistoryResponse(userId,crudChatHistoryResponseList.size(),crudChatHistoryResponseList));

    }

    public ResponseEntity<?> getDetailChat(DeleteChatRequest req, String token) {
        try {
            User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }
            UserChatHistory userChatHistory = userChatRepository.findByUserChatId(req.getChatId());
            if (userChatHistory == null) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userChat");
            }


            String id_mongo_chat = userChatHistory.getIdMongoDb();
            String  apiUrl = userServiceUrl + "/chat/list_detail_chat/"+ id_mongo_chat;



            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody()); // ✅ Chuyển JSON thành object
                // 🛑 Tạo ObjectNode mới để thêm thuộc tính chat_id
                ObjectNode responseObject = objectMapper.createObjectNode();
                responseObject.put("chat_id", req.getChatId()); // ✅ Thêm chat_id vào response
                responseObject.set("data", jsonNode); // ✅ Chèn toàn bộ JSON API vào "data"

                return ResponseEntity.ok(responseObject); // ✅ Trả về JSON mới // ✅ Trả về JSON trực tiếp
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("❌ API lỗi: " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Lỗi máy chủ: " + e.getMessage());
        }
    }




    public ResponseEntity<?> sendQuestion(String token, QuestionChatRequest req, Language language) {
        try {
            String  apiUrl = userServiceUrl + "/chat/question";


            // ✅ Kiểm tra user có tồn tại không
            User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }

            // ✅ Kiểm tra lịch sử chat có tồn tại không
            UserChatHistory userChatHistory = userChatRepository.findByUserChatId(req.getChatId());
            if (userChatHistory == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userChat");
            }

            String idMongoChat = userChatHistory.getIdMongoDb();

            // ✅ Tạo JSON body
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("user_input", req.getQuestion());
            requestBody.put("language", String.valueOf(language));
            requestBody.put("chat_history_id", idMongoChat);
            String jsonInputString = objectMapper.writeValueAsString(requestBody);
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");

            // ✅ Gửi request bằng `HttpURLConnection`
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine);
                }
                br.close();

                // ✅ Chuyển phản hồi thành JSON
                JsonNode jsonResponse = objectMapper.readTree(response.toString());

                // ✅ Ghép thêm `user_id` và `chat_id`
                Map<String, Object> finalResponse = new HashMap<>();
                finalResponse.put("user_id", req.getUserId());
                finalResponse.put("chat_id", req.getChatId());
                finalResponse.put("data", jsonResponse);

                return ResponseEntity.ok(finalResponse);
            } else {
                return ResponseEntity.status(responseCode).body("❌ API lỗi: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Lỗi máy chủ: " + e.getMessage());
        }
    }


    public ResponseEntity<?> sendRegeneration(String token, QuestionChatRequest req, Language language) {
        try {
            String  apiUrl = userServiceUrl + "/chat/regenerate";
            User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }

            // ✅ Kiểm tra lịch sử chat có tồn tại không
            UserChatHistory userChatHistory = userChatRepository.findByUserChatId(req.getChatId());
            if (userChatHistory == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userChat");
            }

            String idMongoChat = userChatHistory.getIdMongoDb();

            // ✅ Tạo JSON body
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("question_new", req.getQuestion());
            requestBody.put("languages", String.valueOf(language));
            requestBody.put("chat_id", idMongoChat);
            String jsonInputString = objectMapper.writeValueAsString(requestBody);

            // ✅ Gửi request bằng `HttpURLConnection`
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine);
                }
                br.close();

                // ✅ Chuyển phản hồi thành JSON
                JsonNode jsonResponse = objectMapper.readTree(response.toString());

                // ✅ Ghép thêm `user_id` và `chat_id`
                Map<String, Object> finalResponse = new HashMap<>();
                finalResponse.put("user_id", req.getUserId());
                finalResponse.put("chat_id", req.getChatId());
                finalResponse.put("data", jsonResponse);

                return ResponseEntity.ok(finalResponse);
            } else {
                return ResponseEntity.status(responseCode).body("❌ API lỗi: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Lỗi máy chủ: " + e.getMessage());
        }
    }


    public static void printFormattedJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(json);
            String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            System.out.println(formattedJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}