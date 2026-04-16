package com.lumencloud.lumen.auth.endpoint;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTemplateRenderTest {

	private static final File TEMPLATE_DIR = new File("src/main/resources/templates/ftl");

	@Test
	void loginTemplateRendersSuccessfully() throws IOException, TemplateException {
		String html = render("login.ftl", Map.of("tokenDemoClientId", "test", "tokenDemoBasicAuth", "Basic dGVzdDp0ZXN0",
				"request", new TemplateRequest("/admin"), "error", "bad_credentials"));

		assertThat(html).contains("统一身份认证").contains("令牌演示面板").contains("演示 client：");
	}

	@Test
	void confirmTemplateRendersSuccessfully() throws IOException, TemplateException {
		String html = render("confirm.ftl",
				Map.of("request", new TemplateRequest("/admin"), "principalName", "admin", "clientId", "test", "state",
						"state-1", "scopeList", List.of("profile", "session:read")));

		assertThat(html).contains("应用授权确认").contains("确认授权").contains("session:read");
	}

	private static String render(String templateName, Map<String, Object> model) throws IOException, TemplateException {
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
		configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
		configuration.setTemplateLoader(new FileTemplateLoader(TEMPLATE_DIR, true));
		Template template = configuration.getTemplate(templateName);
		StringWriter stringWriter = new StringWriter();
		template.process(model, stringWriter);
		return stringWriter.toString();
	}

	public static final class TemplateRequest {

		private final String contextPath;

		public TemplateRequest(String contextPath) {
			this.contextPath = contextPath;
		}

		public String getContextPath() {
			return contextPath;
		}

	}

}
