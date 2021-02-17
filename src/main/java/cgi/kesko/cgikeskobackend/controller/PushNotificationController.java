package cgi.kesko.cgikeskobackend.controller;


import cgi.kesko.cgikeskobackend.model.PushNotificationRequest;
import cgi.kesko.cgikeskobackend.model.NotificationUserRequest;
import cgi.kesko.cgikeskobackend.model.PushNotificationResponse;
import cgi.kesko.cgikeskobackend.service.PushNotificationService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
public class PushNotificationController {

    private PushNotificationService pushNotificationService;

//    @Value("#{${spring.datasource.url}}")
//    String databaseUrl;
//
//    @Value("#{${spring.datasource.username}}")
//    String username;
//
//    @Value("#{${spring.datasource.password}}")
//    String password;


    public PushNotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/notification/user")
    public ResponseEntity mapUserToken(@RequestBody NotificationUserRequest request) throws ClassNotFoundException, SQLException {
        System.out.println(request.getToken() + "," + request.getCustomer());

        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con= DriverManager.getConnection(
                "jdbc:mysql://remotemysql.com:3306/ALchX2qIMc","ALchX2qIMc","aFKmpvtsJl");
        Statement stmt=con.createStatement();
        ResultSet rs=stmt.executeQuery("select * from UserTokens as ut where ut.customer = " + request.getCustomer());
        if(!rs.next()){
            stmt.execute("insert into UserTokens(customer, token)  values('" + request.getCustomer() + "','" + request.getToken() +"')");
        }else{
            stmt.execute("update UserTokens set token = '" + request.getToken() + "' where customer ='" + request.getCustomer() + "'" );
        }
        con.close();
        return new ResponseEntity<>(new PushNotificationResponse(HttpStatus.OK.value(), "Data has been stored"), HttpStatus.OK);
    }


    @PostMapping("/notification")
    public ResponseEntity sendNotification() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con= DriverManager.getConnection(
                "jdbc:mysql://remotemysql.com:3306/ALchX2qIMc","ALchX2qIMc","aFKmpvtsJl");
        Statement stmt=con.createStatement();
        ResultSet rs=stmt.executeQuery("select Person.expirey_date, products.name, products.image from Person JOIN products ON Person.EAN = products.ean where Person.expirey_date > NOW() and Person.expirey_date < DATE_ADD(NOW(), INTERVAL 2 DAY) and Person.KCustomer = 6715");

        Timestamp timestamp = new Timestamp(new Date().getTime());
        Timestamp expiryDate;

        JsonArray jsonArray = new JsonArray();

        while(rs.next()) {
            expiryDate = rs.getTimestamp("expirey_date");

            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("image",rs.getString("image"));
            jsonObject.addProperty("name",rs.getString("name"));
            jsonObject.addProperty("expirationDate",daysBetweenUsingJoda(timestamp, expiryDate));

            jsonArray.add(jsonObject);

        }
        System.out.println(jsonArray.toString());

        con.close();



        PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
        pushNotificationRequest.setMessage(jsonArray.toString());
        pushNotificationRequest.setTitle("ATTENTION, YOUR FOOD IS EXPIRING!");
        pushNotificationRequest.setToken("dpjcMjlNiOA:APA91bEa__HLwbswNDlW6GTfAzou2Y7X_Mf1EU_2I7_0BW-yhXXJj3ic5xY4eFC8X1E9vSNGDgyj3-EcOapsw_tvDTGmmhDG4tdzp6wLotd3sqClYkT44LwqSk8sAnX8-5O_7OF7MrQQ");
        pushNotificationRequest.setTopic("topic");

        pushNotificationService.sendPushNotificationToToken(pushNotificationRequest);

        return new ResponseEntity<>(new PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."), HttpStatus.OK);
    }

    // Same stuff with Request Body
    @PostMapping("/notification/token")
    public ResponseEntity sendTokenNotification(@RequestBody PushNotificationRequest request) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("icon_link","");
        jsonObject.addProperty("product_name","");
        jsonObject.addProperty("time_till_expiry","");
        jsonObject.addProperty("clock_icon_link","");

        System.out.println(jsonObject.toString());

        PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
        pushNotificationRequest.setMessage(jsonObject.toString());
        pushNotificationRequest.setTitle("YOUR FOOD IS EXPIRING");
        pushNotificationRequest.setToken(request.getToken());
        pushNotificationRequest.setTopic(request.getTopic());

        pushNotificationService.sendPushNotificationToToken(pushNotificationRequest);

        return new ResponseEntity<>(new PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."), HttpStatus.OK);
    }

    @PostMapping("/notification/data")
    public ResponseEntity sendDataNotification(@RequestBody PushNotificationRequest request) {
        pushNotificationService.sendPushNotification(request);
        return new ResponseEntity<>(new PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."), HttpStatus.OK);
    }

    @GetMapping("/notification")
    public ResponseEntity sendSampleNotification() {
        pushNotificationService.sendSamplePushNotification();
        return new ResponseEntity<>(new PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."), HttpStatus.OK);
    }

    public static int daysBetweenUsingJoda(Timestamp timestamp, Timestamp expiryDate){
        return Days.daysBetween(
                new LocalDate(timestamp.getTime()),
                new LocalDate(expiryDate.getTime())).getDays();
    }
}
