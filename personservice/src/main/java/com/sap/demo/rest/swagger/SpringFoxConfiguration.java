package com.sap.demo.rest.swagger;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SpringFoxConfiguration {

	
	@Value("${personservicekubernetes.swagger.host}")
	private String hostName;
		

    @Bean
    @Profile("!Security2")
    public Docket api() {   
    	
   
    	
        return new Docket(DocumentationType.SWAGGER_2)
          .apiInfo(apiInfo())
          .host(hostName)
          .useDefaultResponseMessages(false)
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("com.sap.demo"))              
          .paths(PathSelectors.any())
          .build();                                           
    }
    
    
    
    
    @Bean
    @Profile("Security2")
    public Docket secureApi2() { 
    	
    	return new Docket(DocumentationType.SWAGGER_2)
    	          .apiInfo(apiInfo())
    	          .host(hostName)
    	          .globalOperationParameters(Arrays.asList(
    	        		  new ParameterBuilder()
    	        		  .name("Authorization")
    	                  .description("Place for your OAuth2 Token")
    	                  .modelRef(new ModelRef("string"))
    	                  .parameterType("header")
    	                  .required(true)
    	                  .build()))
    	          .useDefaultResponseMessages(false)
    	          .select()                                  
    	          .apis(RequestHandlerSelectors.basePackage("com.sap.demo"))              
    	          .paths(PathSelectors.any())
    	          .build();      	
    }
    
    private ApiInfo apiInfo() {
    	return new ApiInfo("Person Service Kubernetes API", 
    								"API that allows to perform crud Operations for person information stored in Mongo DB."
    								+ " It also includes an endpoint to display all environment variables and their respective"
    								+ " values in the capplication Container. In further evolutions of the code extensions to"
    								+ " Manage the Kyma registration are added.", 
    								"1.0", 
    								"", 
    								new Contact("Andreas Krause", "", "and.krause@sap.com"),
    								"", 
    								"", Collections.emptyList());
    }
    

}
