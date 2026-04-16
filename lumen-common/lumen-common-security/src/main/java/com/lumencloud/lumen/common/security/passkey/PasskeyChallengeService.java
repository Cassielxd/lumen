package com.lumencloud.lumen.common.security.passkey;

import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.util.RedisUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Challenge storage for passkey registration and assertion flows.
 */
@Component
public class PasskeyChallengeService {

	public static final long DEFAULT_TTL_SECONDS = 300L;

	public PasskeyChallengeContext save(PasskeyChallengeContext context) {
		if (context == null || !StringUtils.hasText(context.getType()) || !StringUtils.hasText(context.getChallenge())) {
			throw new IllegalArgumentException("invalid passkey challenge context");
		}
		if (context.getIssuedAt() == null) {
			context.setIssuedAt(LocalDateTime.now());
		}
		RedisUtils.set(buildKey(context.getType(), context.getChallenge()), context, DEFAULT_TTL_SECONDS,
				TimeUnit.SECONDS);
		return context;
	}

	public Optional<PasskeyChallengeContext> get(String type, String challenge) {
		if (!StringUtils.hasText(type) || !StringUtils.hasText(challenge)) {
			return Optional.empty();
		}
		return Optional.ofNullable(RedisUtils.get(buildKey(type, challenge)));
	}

	public Optional<PasskeyChallengeContext> consume(String type, String challenge) {
		Optional<PasskeyChallengeContext> context = get(type, challenge);
		context.ifPresent(ignored -> delete(type, challenge));
		return context;
	}

	public void delete(String type, String challenge) {
		if (StringUtils.hasText(type) && StringUtils.hasText(challenge)) {
			RedisUtils.delete(buildKey(type, challenge));
		}
	}

	private String buildKey(String type, String challenge) {
		return CacheConstants.PASSKEY_CHALLENGE_KEY + ":" + type + ":" + challenge;
	}

}
