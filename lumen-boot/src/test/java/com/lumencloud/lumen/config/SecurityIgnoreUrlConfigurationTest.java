package com.lumencloud.lumen.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIgnoreUrlConfigurationTest {

	@Test
	void applicationConfigShouldExposePublicClientListWithoutAuthentication() throws IOException {
		String yaml = Files.readString(Path.of("src/main/resources/application.yml"));
		assertThat(yaml).contains("/client/public/**");
	}

}
