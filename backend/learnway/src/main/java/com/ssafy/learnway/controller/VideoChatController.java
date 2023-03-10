package com.ssafy.learnway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.learnway.dto.study.StudyRecordRequestDto;
import com.ssafy.learnway.service.study.StudyService;
import io.openvidu.java.client.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
@CrossOrigin(origins = "*")
@Tag(name = "video")
public class VideoChatController {

//    @Value("${OPENVIDU_URL}")
    private String OPENVIDU_URL = "https://i8a408.p.ssafy.io";

//    @Value("${OPENVIDU_SECRET}")
    private String OPENVIDU_SECRET = "ssafy";

    // OpenVidu object as entrypoint of the SDK
    private OpenVidu openVidu;

    // Collection to pair session names and OpenVidu Session objects
    private Map<String, Session> mapSessions = new ConcurrentHashMap<>();
    // Collection to pair session names and tokens (the inner Map pairs tokens and
    // role associated)
    private Map<String, Map<String, OpenViduRole>> mapSessionNamesTokens = new ConcurrentHashMap<>();
    // Collection to pair session names and recording objects
    private Map<String, String> sessionRecordings = new ConcurrentHashMap<>();
    @Autowired
    private StudyService studyService;

    @PostConstruct
    public void init() {
        this.openVidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
    }

    /*******************/
    /*** Session API ***/
    /*******************/

    /**
     * @param params The Session properties
     * @return The Session ID
     */
    @RequestMapping(value="/api/sessions", method = RequestMethod.POST)
    public ResponseEntity<Map> initializeSession(@RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        System.out.println(params.get("customSessionId"));
        Map<String,String> map = new HashMap<>();
        if(openVidu.getActiveSession((String)params.get("customSessionId")) != null){
            map.put("isSecond", "true");
        } else{
            map.put("iseSecond", "false");
        }
        SessionProperties properties = SessionProperties.fromJson(params).build();
        Session session = openVidu.createSession(properties);
        map.put("sessionId",session.getSessionId());
        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    /**
     * @param sessionId The Session in which to create the Connection
     * @param params    The Connection properties
     * @return The Token associated to the Connection
     */
    @RequestMapping(value="/api/sessions/{sessionId}/connections", method = RequestMethod.POST)
    public ResponseEntity<String> createConnection(@PathVariable("sessionId") String sessionId,
                                                   @RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        Session session = openVidu.getActiveSession(sessionId);
        // Role associated to this user
        OpenViduRole role = OpenViduRole.PUBLISHER;

        if (session == null) {
            System.out.println("no session found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        }
        ConnectionProperties properties = ConnectionProperties.fromJson(params).build();
        Connection connection = session.createConnection(properties);
        String token = connection.getToken();
        // Store the session and the token in our collections
        this.mapSessions.put(sessionId, session);
        this.mapSessionNamesTokens.put(sessionId, new ConcurrentHashMap<>());
        this.mapSessionNamesTokens.get(sessionId).put(token, role);


        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    @RequestMapping(value = "/get-token", method = RequestMethod.POST)
    public ResponseEntity<JsonObject> getToken(@RequestBody Map<String, Object> sessionNameParam) {

        System.out.println("Getting sessionId and token | {sessionName}=" + sessionNameParam);

        // The video-call to connect ("TUTORIAL")
        String sessionName = (String) sessionNameParam.get("sessionName");

        // Role associated to this user
        OpenViduRole role = OpenViduRole.PUBLISHER;

        // Build connectionProperties object with the serverData and the role
        ConnectionProperties connectionProperties = new ConnectionProperties.Builder().type(ConnectionType.WEBRTC)
                .role(role).data("user_data").build();

        JsonObject responseJson = new JsonObject();
        ObjectMapper mapper = new ObjectMapper();

        if (this.mapSessions.get(sessionName) != null) {
            // Session already exists
            System.out.println("Existing session " + sessionName);
            try {

                // Generate a new token with the recently created connectionProperties
                String token = this.mapSessions.get(sessionName).createConnection(connectionProperties).getToken();

                // Update our collection storing the new token
                this.mapSessionNamesTokens.get(sessionName).put(token, role);

                // Prepare the response with the token
                responseJson.addProperty("0", token);

                // Return the response to the client
                return new ResponseEntity<>(responseJson, HttpStatus.OK);

            } catch (OpenViduJavaClientException e1) {
                // If internal error generate an error message and return it to client
                return getErrorResponse(e1);
            } catch (OpenViduHttpException e2) {
                if (404 == e2.getStatus()) {
                    // Invalid sessionId (user left unexpectedly). Session object is not valid
                    // anymore. Clean collections and continue as new session
                    this.mapSessions.remove(sessionName);
                    this.mapSessionNamesTokens.remove(sessionName);
                }
            }
        }

        // New session
        System.out.println("New session " + sessionName);
        try {

            // Create a new OpenVidu Session
            Session session = this.openVidu.createSession();
            // Generate a new token with the recently created connectionProperties
            String token = session.createConnection(connectionProperties).getToken();

            // Store the session and the token in our collections
            this.mapSessions.put(sessionName, session);
            this.mapSessionNamesTokens.put(sessionName, new ConcurrentHashMap<>());
            this.mapSessionNamesTokens.get(sessionName).put(token, role);

            // Prepare the response with the sessionId and the token
            responseJson.addProperty("0", token);
//            String value = mapper.readValue(token, String);
//            System.out.println(responseJson.toString());
            // Return the response to the client
            return new ResponseEntity<>(responseJson, HttpStatus.OK);

        } catch (Exception e) {
            // If error generate an error message and return it to client
            return getErrorResponse(e);
        }
    }

    @RequestMapping(value = "/remove-user", method = RequestMethod.POST)
    public ResponseEntity<JsonObject> removeUser(@RequestBody Map<String, Object> sessionNameToken) throws Exception {

        System.out.println("Removing user | {sessionName, token}=" + sessionNameToken);

        // Retrieve the params from BODY
        String sessionName = (String) sessionNameToken.get("sessionName");
        String token = (String) sessionNameToken.get("token");

        // If the session exists
        if (this.mapSessions.get(sessionName) != null && this.mapSessionNamesTokens.get(sessionName) != null) {

            // If the token exists
            if (this.mapSessionNamesTokens.get(sessionName).remove(token) != null) {
                // User left the session
                if (this.mapSessionNamesTokens.get(sessionName).isEmpty()) {
                    // Last user left: session must be removed
                    this.mapSessions.remove(sessionName);
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                // The TOKEN wasn't valid
                System.out.println("Problems in the app server: the TOKEN wasn't valid");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } else {
            // The SESSION does not exist
            System.out.println("Problems in the app server: the SESSION does not exist");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/close-session", method = RequestMethod.DELETE)
    public ResponseEntity<JsonObject> closeSession(@RequestBody Map<String, Object> sessionName) throws Exception {

        System.out.println("Closing session | {sessionName}=" + sessionName);

        // Retrieve the param from BODY
        String session = (String) sessionName.get("sessionName");

        // If the session exists
        if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
            Session s = this.mapSessions.get(session);
            s.close();
            this.mapSessions.remove(session);
            this.mapSessionNamesTokens.remove(session);
            this.sessionRecordings.remove(s.getSessionId());
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            // The SESSION does not exist
            System.out.println("Problems in the app server: the SESSION does not exist");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/fetch-info", method = RequestMethod.POST)
    public ResponseEntity<JsonObject> fetchInfo(@RequestBody Map<String, Object> sessionName) {
        try {
            System.out.println("Fetching session info | {sessionName}=" + sessionName);

            // Retrieve the param from BODY
            String session = (String) sessionName.get("sessionName");

            // If the session exists
            if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
                Session s = this.mapSessions.get(session);
                boolean changed = s.fetch();
                System.out.println("Any change: " + changed);
                return new ResponseEntity<>(this.sessionToJson(s), HttpStatus.OK);
            } else {
                // The SESSION does not exist
                System.out.println("Problems in the app server: the SESSION does not exist");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            e.printStackTrace();
            return getErrorResponse(e);
        }
    }

    @RequestMapping(value = "/fetch-all", method = RequestMethod.GET)
    public ResponseEntity<?> fetchAll() {
        try {
            System.out.println("Fetching all session info");
            boolean changed = this.openVidu.fetch();
            System.out.println("Any change: " + changed);
            JsonArray jsonArray = new JsonArray();
            for (Session s : this.openVidu.getActiveSessions()) {
                jsonArray.add(this.sessionToJson(s));
            }
            return new ResponseEntity<>(jsonArray, HttpStatus.OK);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            e.printStackTrace();
            return getErrorResponse(e);
        }
    }

    @RequestMapping(value = "/force-disconnect", method = RequestMethod.DELETE)
    public ResponseEntity<JsonObject> forceDisconnect(@RequestBody Map<String, Object> params) {
        try {
            // Retrieve the param from BODY
            String session = (String) params.get("sessionName");
            String connectionId = (String) params.get("connectionId");

            // If the session exists
            if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
                Session s = this.mapSessions.get(session);
                s.forceDisconnect(connectionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                // The SESSION does not exist
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            e.printStackTrace();
            return getErrorResponse(e);
        }
    }

    @RequestMapping(value = "/force-unpublish", method = RequestMethod.DELETE)
    public ResponseEntity<JsonObject> forceUnpublish(@RequestBody Map<String, Object> params) {
        try {
            // Retrieve the param from BODY
            String session = (String) params.get("sessionName");
            String streamId = (String) params.get("streamId");

            // If the session exists
            if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
                Session s = this.mapSessions.get(session);
                s.forceUnpublish(streamId);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                // The SESSION does not exist
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            e.printStackTrace();
            return getErrorResponse(e);
        }
    }

    /*******************/
    /** Recording API **/
    /*******************/

    @RequestMapping(value = "/recording/start", method = RequestMethod.POST)
    public ResponseEntity<?> startRecording(@RequestBody Map<String, Object> params) {
        String sessionId = (String) params.get("session");
        Recording.OutputMode outputMode = Recording.OutputMode.valueOf((String) params.get("outputMode"));
        boolean hasAudio = (boolean) params.get("hasAudio");
        boolean hasVideo = (boolean) params.get("hasVideo");

        RecordingProperties properties = new RecordingProperties.Builder().outputMode(outputMode).hasAudio(hasAudio)
                .hasVideo(hasVideo).build();

        System.out.println("Starting recording for session " + sessionId + " with properties {outputMode=" + outputMode
                + ", hasAudio=" + hasAudio + ", hasVideo=" + hasVideo + "}");
        try {
            Recording recording = this.openVidu.startRecording(sessionId, properties);

            this.sessionRecordings.put(sessionId,  recording.getId());
            return new ResponseEntity<>(recording.getId(), HttpStatus.OK);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @RequestMapping(value = "/recording/stop", method = RequestMethod.POST)
    public ResponseEntity<?> stopRecording(@RequestBody StudyRecordRequestDto studyRecordRequestDto) {
        String recordingId = studyRecordRequestDto.getRecordId();

        System.out.println("Stoping recording | {recordingId}=" + recordingId);

        try {
            Recording recording = this.openVidu.stopRecording(recordingId);
            studyService.insertStudy(studyRecordRequestDto, recording.getUrl());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SQLException | URISyntaxException |
                UnsupportedEncodingException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/recording/delete", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteRecording(@RequestBody Map<String, Object> params) {
        String recordingId = (String) params.get("recording");

        System.out.println("Deleting recording | {recordingId}=" + recordingId);

        try {
            this.openVidu.deleteRecording(recordingId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/recording/get/{recordingId}", method = RequestMethod.GET)
    public ResponseEntity<?> getRecording(@PathVariable(value = "recordingId") String recordingId) {

        System.out.println("Getting recording | {recordingId}=" + recordingId);

        try {
            Recording recording = this.openVidu.getRecording(recordingId);
            return new ResponseEntity<>(recording, HttpStatus.OK);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/recording/list", method = RequestMethod.GET)
    public ResponseEntity<?> listRecordings() {

        System.out.println("Listing recordings");

        try {
            List<Recording> recordings = this.openVidu.listRecordings();

            return new ResponseEntity<>(recordings, HttpStatus.OK);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private ResponseEntity<JsonObject> getErrorResponse(Exception e) {
        JsonObject json = new JsonObject();
        json.addProperty("cause", e.getCause().toString());
        json.addProperty("error", e.getMessage());
        json.addProperty("exception", e.getClass().getCanonicalName());
        return new ResponseEntity<>(json, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    protected JsonObject sessionToJson(Session session) {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.addProperty("sessionId", session.getSessionId());
        json.addProperty("customSessionId", session.getProperties().customSessionId());
        json.addProperty("recording", session.isBeingRecorded());
        json.addProperty("mediaMode", session.getProperties().mediaMode().name());
        json.addProperty("recordingMode", session.getProperties().recordingMode().name());
        json.add("defaultRecordingProperties",
                gson.toJsonTree(session.getProperties().defaultRecordingProperties()).getAsJsonObject());
        JsonObject connections = new JsonObject();
        connections.addProperty("numberOfElements", session.getConnections().size());
        JsonArray jsonArrayConnections = new JsonArray();
        session.getConnections().forEach(con -> {
            JsonObject c = new JsonObject();
            c.addProperty("connectionId", con.getConnectionId());
            c.addProperty("role", con.getRole().name());
            c.addProperty("token", con.getToken());
            c.addProperty("clientData", con.getClientData());
            c.addProperty("serverData", con.getServerData());
            JsonArray pubs = new JsonArray();
            con.getPublishers().forEach(p -> {
                pubs.add(gson.toJsonTree(p).getAsJsonObject());
            });
            JsonArray subs = new JsonArray();
            con.getSubscribers().forEach(s -> {
                subs.add(s);
            });
            c.add("publishers", pubs);
            c.add("subscribers", subs);
            jsonArrayConnections.add(c);
        });
        connections.add("content", jsonArrayConnections);
        json.add("connections", connections);
        return json;
    }

}
