package monitoring;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class SendEmail {

    private String username;
    private String password;
    private Message message;
    private String port;

    public SendEmail(String username, String password, String protocol) {
        this.username = username;
        this.password = password;
        if(protocol.equals("TLS")) {
            this.port = "587";
        }
        else if(protocol.equals("SSL")) {
            this.port = "465";
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void createMessage(String subject, String text, String adminList) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.auth", "true");
        if(getPort().equals("TLS")) {
            prop.put("mail.smtp.starttls.enable", "true"); //TLS
        }
        else if(getPort().equals("SSL")) {
            prop.put("mail.smtp.socketFactory.port", "465");
            prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }


        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(getUsername(), getPassword());
                    }
                });

        try {

            message = new MimeMessage(session);
            message.setFrom(new InternetAddress(getUsername()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(adminList));
            message.setSubject(subject);
            message.setText(text);

            System.out.println("Message Created");

        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

    public void sendMessage() {
        try {
            Transport.send(message);

            System.out.println("Message Sent");
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
    }


}
