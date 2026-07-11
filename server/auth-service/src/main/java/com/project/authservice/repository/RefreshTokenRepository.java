package com.project.authservice.repository;

import com.project.authservice.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);

	void deleteByAccountId(Long accountId);

	/**
	 * Finds all active (non-revoked) refresh tokens for a given account that were
	 * issued from the same browser/device, identified by matching the User-Agent
	 * stored in the audit_logs entry written during the same login event.
	 *
	 * <p>Correlation is made by joining on account_id, action = 'LOGIN_SUCCESS',
	 * matching user_agent, and a 2-second creation-time window to account for the
	 * slight offset between the refresh token (saved in the main transaction) and
	 * the audit log (saved in a REQUIRES_NEW sub-transaction).</p>
	 *
	 * @param accountId the account whose tokens should be searched
	 * @param userAgent the User-Agent header value from the current login request
	 * @return list of active refresh tokens originating from the same device
	 */
	@Query(value = """
			SELECT rt.* FROM refresh_tokens rt
			INNER JOIN audit_logs al
			    ON al.account_id = rt.account_id
			   AND al.action = 'LOGIN_SUCCESS'
			   AND al.user_agent = :userAgent
			   AND ABS(TIMESTAMPDIFF(SECOND, al.created_at, rt.created_at)) <= 2
			WHERE rt.account_id = :accountId
			  AND rt.is_revoked = 0
			""", nativeQuery = true)
	List<RefreshToken> findActiveTokensByAccountAndUserAgent(
			@Param("accountId") Long accountId,
			@Param("userAgent") String userAgent);
}
