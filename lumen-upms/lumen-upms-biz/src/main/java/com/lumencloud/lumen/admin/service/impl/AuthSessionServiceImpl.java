package com.lumencloud.lumen.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lumencloud.lumen.admin.api.dto.AuthSessionSaveDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.vo.AuthSessionVO;
import com.lumencloud.lumen.admin.mapper.AuthSessionMapper;
import com.lumencloud.lumen.admin.service.AuthSessionService;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Authentication session service implementation.
 */
@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {

	private final AuthSessionMapper authSessionMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AuthSession saveSession(AuthSessionSaveDTO request) {
		String accessTokenHash = sha256(request.getAccessToken());
		String refreshTokenHash = sha256(request.getRefreshToken());

		AuthSession session = null;
		if (StringUtils.hasText(refreshTokenHash)) {
			session = authSessionMapper.selectOne(Wrappers.<AuthSession>lambdaQuery()
				.eq(AuthSession::getRefreshTokenHash, refreshTokenHash), false);
		}
		if (session == null && StringUtils.hasText(accessTokenHash)) {
			session = authSessionMapper.selectOne(Wrappers.<AuthSession>lambdaQuery()
				.eq(AuthSession::getAccessTokenHash, accessTokenHash), false);
		}
		if (session == null) {
			session = new AuthSession();
			session.setSid(UUID.randomUUID().toString().replace("-", ""));
			session.setCreateBy(request.getPrincipalName());
			session.setCreateTime(LocalDateTime.now());
		}

		session.setAccountId(request.getAccountId());
		session.setUserId(request.getUserId());
		session.setClientId(request.getClientId());
		session.setPrincipalName(request.getPrincipalName());
		session.setGrantType(request.getGrantType());
		session.setAccessTokenHash(accessTokenHash);
		session.setRefreshTokenHash(refreshTokenHash);
		session.setAccessTokenExpiresAt(request.getAccessTokenExpiresAt());
		session.setRefreshTokenExpiresAt(request.getRefreshTokenExpiresAt());
		session.setIpAddress(request.getIpAddress());
		session.setUserAgent(request.getUserAgent());
		session.setLastActiveTime(LocalDateTime.now());
		session.setLogoutTime(null);
		session.setStatus(CommonConstants.STATUS_NORMAL);
		session.setUpdateBy(request.getPrincipalName());
		session.setUpdateTime(LocalDateTime.now());

		if (session.getSessionId() == null) {
			authSessionMapper.insert(session);
		}
		else {
			authSessionMapper.updateById(session);
		}
		return session;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean logoutByAccessToken(String accessToken) {
		if (!StringUtils.hasText(accessToken)) {
			return Boolean.TRUE;
		}
		return markLogout(getByAccessToken(accessToken));
	}

	@Override
	public AuthSession getByAccessToken(String accessToken) {
		if (!StringUtils.hasText(accessToken)) {
			return null;
		}
		return authSessionMapper.selectOne(
				Wrappers.<AuthSession>lambdaQuery().eq(AuthSession::getAccessTokenHash, sha256(accessToken)), false);
	}

	@Override
	public AuthSession getByRefreshToken(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return null;
		}
		return authSessionMapper.selectOne(
				Wrappers.<AuthSession>lambdaQuery().eq(AuthSession::getRefreshTokenHash, sha256(refreshToken)), false);
	}

	@Override
	public List<AuthSessionVO> listByAccountId(Long accountId, String currentSid) {
		if (accountId == null) {
			return List.of();
		}
		return authSessionMapper
			.selectList(Wrappers.<AuthSession>lambdaQuery()
				.eq(AuthSession::getAccountId, accountId)
				.orderByDesc(AuthSession::getLastActiveTime)
			.orderByDesc(AuthSession::getCreateTime))
			.stream()
			.map(session -> toView(session, currentSid))
			.toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean logoutBySid(Long accountId, String sid) {
		if (accountId == null || !StringUtils.hasText(sid)) {
			return Boolean.FALSE;
		}
		AuthSession session = authSessionMapper.selectOne(Wrappers.<AuthSession>lambdaQuery()
			.eq(AuthSession::getAccountId, accountId)
			.eq(AuthSession::getSid, sid), false);
		return markLogout(session);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean logoutOtherSessions(Long accountId, String currentSid) {
		if (accountId == null || !StringUtils.hasText(currentSid)) {
			return Boolean.FALSE;
		}
		List<AuthSession> sessions = authSessionMapper.selectList(Wrappers.<AuthSession>lambdaQuery()
			.eq(AuthSession::getAccountId, accountId)
			.ne(AuthSession::getSid, currentSid)
			.eq(AuthSession::getStatus, CommonConstants.STATUS_NORMAL)
			.isNull(AuthSession::getLogoutTime));
		sessions.forEach(this::markLogout);
		return Boolean.TRUE;
	}

	private Boolean markLogout(AuthSession session) {
		if (session == null) {
			return Boolean.TRUE;
		}
		session.setStatus(CommonConstants.STATUS_LOCK);
		session.setLogoutTime(LocalDateTime.now());
		session.setUpdateTime(LocalDateTime.now());
		authSessionMapper.updateById(session);
		return Boolean.TRUE;
	}

	private AuthSessionVO toView(AuthSession session, String currentSid) {
		AuthSessionVO view = new AuthSessionVO();
		BeanUtils.copyProperties(session, view);
		view.setCurrent(Objects.equals(session.getSid(), currentSid));
		return view;
	}

	private String sha256(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}

}
