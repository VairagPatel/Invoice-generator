package in.bushansirgur.invoicegeneratorapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class InvoicegeneratorapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoicegeneratorapiApplication.class, args);
	}

}
