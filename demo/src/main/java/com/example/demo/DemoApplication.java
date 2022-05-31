package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.messaging.MessageChannel;

import java.util.Properties;

@Configuration
//@EnableConfigurationProperties(GMailProperties.class)
@EnableAutoConfiguration
@IntegrationComponentScan
public class DemoApplication {


    //@Autowired
    //GMailProperties gmail;

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx =
                new SpringApplicationBuilder(DemoApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(args);
        System.out.println(ctx.getBean(FooService.class).foo("testTLS"));
        ctx.close();
    }

    @MessagingGateway(defaultRequestChannel="foo.input")
    public static interface FooService {

        String foo(String request);

    }

    @Bean
    IntegrationFlow foo() {
        return f -> f
                .transform("payload + payload")
                .handle(String.class, (p, h) -> p.toUpperCase())
                .routeToRecipients(r ->
                        r.recipient("bridgeToNowhere", "true")
                                .recipient("smtpChannel", "true"));
    }

    @BridgeTo
    @Bean
    public MessageChannel bridgeToNowhere() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel smtpChannel() {
        return new DirectChannel();
    }

    @Bean
    IntegrationFlow smtp() {
        Properties prop = new Properties();
        prop.put("mail.debug", "false");
        //SSL config
        //prop.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        //prop.put("mail.smtp.ssl.enable","true");
        prop.put("mail.smtp.auth","true");
        //prop.put("mail.smtp.socketFactory.fallback","false");
        prop.put("mail.smtp.starttls.enable","true");
        prop.put("mail.smtp.starttls.required","false");

        //prop.put("mail.smtp.ssl.trust","smtp.gmail.com");

        return IntegrationFlows.from(smtpChannel())
                .enrichHeaders(Mail.headers()
                        .subject("TestMail")
                        .to("mech.kgv@gmail.com")
                        .from("mech.kgv@gmail.com"))
                .handle(Mail.outboundAdapter("smtp.gmail.com")
                                .port(587) //465 for ssl 587 for TLS
                                .protocol("smtp")
                                .credentials("mech.kgv@gmail.com","*****")
                                .javaMailProperties(prop)
                        , e -> e.id("smtpOut"))
                .get();
    }

}
