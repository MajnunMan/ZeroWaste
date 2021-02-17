package cgi.kesko.cgikeskobackend.model;

public class NotificationUserRequest {

    private String token;
    private int customer;


    public NotificationUserRequest(int customer, String token) {
        this.customer = customer;
        this.token = token;
    }


    public int getCustomer() {
        return customer;
    }

    public void setCustomer(int customer) {
        this.customer = customer;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
