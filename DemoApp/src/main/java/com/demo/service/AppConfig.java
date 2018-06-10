package com.demo.service;

import java.net.URI;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;

import com.demo.dto.Secrets;

@Configuration
public class AppConfig{

	@Value("${vault.uri}")
	private URI vaultUri;
	@Value("${vault.token}")
	private String vaultToken;
	@Value("${vault.path}")
	private String vaultPath;
	

	@Value("${jdbc.datasource.url}")
	private String url;
	@Value("${jdbc.datasource.username}")
	private String username;
	@Value("${jdbc.datasource.password}")
	private String password;
	@Value("${jdbc.datasource.driverClassName}")
	private String driver;
	@Value("${jdbc.datasource.validationQuery}")
	private String validationQuery;
	@Value("${jdbc.datasource.maxWait}")
	private long maxWait;
	@Value("${jdbc.datasource.maxActive}")
	private int maxActive;
	@Value("${jdbc.datasource.maxIdle}")
	private int maxIdle;
	@Value("${jdbc.datasource.testOnBorrow}")
	private boolean testOnBorrow;
	@Value("${jdbc.datasource.testOnReturn}")
	private boolean testOnReturn;


	@Bean
	@Autowired
	public BasicDataSource dataSource() {
		VaultTemplate vaultTemplate = new VaultTemplate(VaultEndpoint.from(vaultUri),new TokenAuthentication(vaultToken));
		VaultResponseSupport<Secrets> response = vaultTemplate.read(vaultPath, Secrets.class);
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driver);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(response.getData().getPassword());
		dataSource.setValidationQuery(validationQuery);
		dataSource.setMaxWait(maxWait);
		dataSource.setMaxIdle(maxIdle);
		dataSource.setTestOnBorrow(testOnBorrow);
		dataSource.setTestOnReturn(testOnReturn);
		dataSource.setMaxActive(maxActive);
		return dataSource;
	}
}
