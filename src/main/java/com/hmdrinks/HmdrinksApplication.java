package com.hmdrinks;

import com.hmdrinks.Config.RateLimitingFilter;
import com.hmdrinks.Service.ElasticsearchSyncService;
import com.hmdrinks.Service.ShipperComissionDetailService;
import com.hmdrinks.Service.VNPayIpnHandler;
import com.hmdrinks.Service.ZaloPayService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@OpenAPIDefinition(servers = {@Server(url = "/", description = "HMDrinks Server URL")})
@SpringBootApplication(scanBasePackages = "com.hmdrinks")
@EnableAsync
@EnableScheduling
public class HmdrinksApplication {
	@Autowired
	private ElasticsearchSyncService elasticsearchSyncService;
	@Autowired
	private ShipperComissionDetailService shipperComissionDetailService;
	@Autowired
	private VNPayIpnHandler vnPayIpnHandler;
	@Autowired
	private SupportFunction supportFunction;
	@Autowired
	private ZaloPayService zaloPayService;
	public static void main(String[] args) {

		SpringApplication.run(HmdrinksApplication.class, args);
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

	}

	@PostConstruct
	public void init() {
//		shipperComissionDetailService.updateAllShipperCommissions();
		supportFunction.resetShipperDaily();
	}



}
